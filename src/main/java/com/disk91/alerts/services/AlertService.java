/*
 * Copyright (c) - Paul Pinault (aka disk91) - 2026.
 *
 *    Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 *    and associated documentation files (the "Software"), to deal in the Software without restriction,
 *    including without limitation the rights to use, copy, modify, merge, publish, distribute,
 *    sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 *    furnished to do so, subject to the following conditions:
 *
 *    The above copyright notice and this permission notice shall be included in all copies or
 *    substantial portions of the Software.
 *
 *    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 *    FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
 *    OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 *    WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 *    IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.disk91.alerts.services;

import com.disk91.alerts.config.ActionCatalog;
import com.disk91.alerts.config.AlertsConfig;
import com.disk91.alerts.mdb.entities.Alert;
import com.disk91.alerts.mdb.entities.AlertTemplate;
import com.disk91.alerts.mdb.entities.sub.AlertBehavior;
import com.disk91.alerts.mdb.entities.sub.AlertState;
import com.disk91.alerts.mdb.repositories.AlertRepository;
import com.disk91.alerts.mdb.entities.sub.AlertLocaleMessage;
import com.disk91.alerts.mdb.entities.sub.AlertMedium;
import com.disk91.alerts.mdb.entities.sub.AlertMediumMessage;
import com.disk91.alerts.mdb.entities.sub.AlertSentEntry;
import com.disk91.alerts.mdb.entities.sub.AlertSentState;
import com.disk91.audit.integration.AuditIntegration;
import com.disk91.common.config.ModuleCatalog;
import com.disk91.common.tools.Now;
import com.disk91.common.tools.RandomString;
import com.disk91.common.tools.exceptions.ITNotFoundException;
import com.disk91.common.tools.exceptions.ITParseException;
import com.disk91.groups.mdb.entities.Group;
import com.disk91.groups.services.GroupsCache;
import com.disk91.groups.services.GroupsServices;
import com.disk91.users.mdb.entities.User;
import com.disk91.users.mdb.entities.sub.UserAlertPreference;
import com.disk91.users.mdb.repositories.UserRepository;
import com.disk91.users.services.UserCommon;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.disk91.users.services.UsersRolesCache.StandardRoles.ROLE_DEVICE_ALERTING;

@Service
public class AlertService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected AlertRepository alertRepository;

    @Autowired
    protected AlertTemplateCache alertTemplateCache;

    @Autowired
    protected AlertsConfig alertsConfig;

    @Autowired
    @Qualifier("alertTaskExecutor")
    protected ThreadPoolTaskExecutor alertTaskExecutor;

    @Autowired
    protected AuditIntegration auditIntegration;

    @Autowired
    protected UserCommon userCommon;

    @Autowired
    protected GroupsServices groupsServices;

    // ================================================================================================================
    // WORKER INFRASTRUCTURE
    // ================================================================================================================

    // In-memory queue holding alerts ready for worker consumption.
    // An alert is added here after its state is persisted as PENDING_QUEUE or ENDING.
    private final LinkedBlockingQueue<Alert> alertQueue = new LinkedBlockingQueue<>();

    // Controls the worker loop; set to false on shutdown to let workers exit cleanly.
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * Initialise workers at startup.
     * Resets alerts stuck in PENDING_QUEUE (left over from a previous run) back to PENDING,
     * then re-enqueues all PENDING and ENDING alerts so nothing is lost across restarts.
     * Submits exactly alerts.max.parallel.processing worker threads to the Spring task executor.
     */
    @PostConstruct
    private void initAlertProcessor() {
        // Reset PENDING_QUEUE alerts stuck in the memory queue when the previous run stopped
        List<Alert> stuckPending = alertRepository.findAlertsByState(AlertState.PENDING_QUEUE);
        for (Alert alert : stuckPending) {
            alert.setState(AlertState.PENDING);
            alertRepository.save(alert);
        }
        if (!stuckPending.isEmpty()) {
            log.info("[alerts] Reset {} PENDING_QUEUE alert(s) to PENDING at startup", stuckPending.size());
        }

        // Reset ENDING_QUEUE alerts stuck in the memory queue when the previous run stopped
        List<Alert> stuckEnding = alertRepository.findAlertsByState(AlertState.ENDING_QUEUE);
        for (Alert alert : stuckEnding) {
            alert.setState(AlertState.ENDING);
            alertRepository.save(alert);
        }
        if (!stuckEnding.isEmpty()) {
            log.info("[alerts] Reset {} ENDING_QUEUE alert(s) to ENDING at startup", stuckEnding.size());
        }

        running.set(true);

        // Start N worker threads via the Spring-managed thread pool
        int maxParallel = alertsConfig.getAlertsMaxParallelProcessing();
        for (int i = 0; i < maxParallel; i++) {
            alertTaskExecutor.submit(this::workerLoop);
        }
        log.info("[alerts] Alert processor started with {} workers", maxParallel);

        // Re-enqueue PENDING and ENDING alerts together, oldest-first, so submission order is preserved
        List<Alert> toRequeue = alertRepository.findAlertsByStateInOrderByRequestMsAsc(
                List.of(AlertState.PENDING, AlertState.ENDING)
        );
        int pendingCount = 0, endingCount = 0;
        for (Alert alert : toRequeue) {
            if (alert.getState() == AlertState.PENDING) pendingCount++;
            else endingCount++;
            enqueue(alert);
        }
        log.info("[alerts] Startup enqueue: {} PENDING, {} ENDING", pendingCount, endingCount);
    }

    /**
     * Signal workers to stop on application shutdown.
     * The ThreadPoolTaskExecutor waits up to awaitTerminationSeconds for workers to finish their
     * current alert and exit on the next queue poll timeout.
     */
    @PreDestroy
    private void destroyAlertProcessor() {
        log.info("[alerts] Alert processor stopping");
        running.set(false);
        // Workers will exit within 1 second (poll timeout); ThreadPoolTaskExecutor handles the rest
    }

    // ================================================================================================================
    // WORKER LOOP
    // ================================================================================================================

    /**
     * Long-running worker loop: dequeues one alert at a time and processes it.
     * Blocks up to 1 second on each poll so it can check the running flag without busy-waiting.
     * Submitted once per worker thread at startup; exits when running becomes false.
     */
    private void workerLoop() {
        log.debug("[alerts] Worker started: {}", Thread.currentThread().getName());
        while (running.get()) {
            try {
                Alert alert;
                try {
                    alert = alertQueue.poll(1, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                if (alert == null) continue;

                try {
                    processAlert(alert, Now.NowUtcMs());
                } catch (Exception e) {
                    log.error("[alerts] Error processing alert {} (state={}): {}",
                            alert.getAlertId(), alert.getState(), e.getMessage());
                }
            } catch (Exception e) {
                log.error("[alerts] Unexpected Exception in worker loop {}", e.getMessage());
            }
        }
        log.debug("[alerts] Worker stopped: {}", Thread.currentThread().getName());
    }
    /**
     * Transition an alert to its queue state, persist it, and add it to the in-memory queue.
     * PENDING → PENDING_QUEUE, ENDING → ENDING_QUEUE.
     * @param alert - the alert to enqueue; its state is mutated in place
     */
    private void enqueue(Alert alert) {
        alert.setState(alert.getState() == AlertState.PENDING ? AlertState.PENDING_QUEUE : AlertState.ENDING_QUEUE);
        alertRepository.save(alert);
        alertQueue.offer(alert);
    }

    // ================================================================================================================
    // ALERT STATE MACHINE
    // ================================================================================================================

    /**
     * Find the right locale to be used
     *
     */
     private AlertLocaleMessage getRightAlertLocaleMessage(
             String prefLocale,
             List<AlertLocaleMessage> templates
     ) {
         // find the best language, based on what template propose and what user prefer, if no match, prefer English on
         // select the first available in template if English not available

         String userLangPrefix = (prefLocale != null && prefLocale.length() == 2) ? prefLocale : "en";
         AlertLocaleMessage bestLocale = null;
         AlertLocaleMessage englishFallback = null;
         AlertLocaleMessage firstAvailable = null;
         for (AlertLocaleMessage lm : templates) {
             if (lm.getLocale() == null) continue;
             if (firstAvailable == null) firstAvailable = lm;
             String lmPrefix = lm.getLocale().length() >= 2
                     ? lm.getLocale().substring(0, 2).toLowerCase()
                     : lm.getLocale().toLowerCase();
             if (lmPrefix.equals(userLangPrefix)) { bestLocale = lm; break; }
             if (lmPrefix.equals("en") && englishFallback == null) englishFallback = lm;
         }
         if (bestLocale == null) bestLocale = (englishFallback != null) ? englishFallback : firstAvailable;
         return bestLocale;
     }

    /**
     * Find the best Medium for a given User in regard of the template preference
     * Can return NULL when no match
     */
    private AlertMedium getRightMedium(User user, AlertLocaleMessage alm, AlertTemplate template) {

        // find the best medium based on medium accepted by user and the medium preferred by template:
        //     Scan the template preferred in the given order and take the first accepted by the user.
        //     When no match between user choice and template, we select default in template when exists
        //     When selection comes to DEFAULT, for user we try PUSH, then SMS, then EMAIL based on what user accepts
        //     When no preferred, we can scan the available list in ale

        UserAlertPreference upref = user.getAlertPreference() != null ? user.getAlertPreference() : UserAlertPreference.of();
        boolean templateHasDefault = template.getPreferred().isEmpty() || template.getPreferred().contains(AlertMedium.DEFAULT);
        AlertMedium selectedMedium = null;

        // scan template preferred mediums in order and pick first one the user accepts
        for (AlertMedium m : template.getPreferred()) {
            if (m == AlertMedium.DEFAULT) continue;
            if (m == AlertMedium.PUSH  && upref.isPushAlert())  { selectedMedium = m; break; }
            if (m == AlertMedium.SMS   && upref.isSmsAlert())   { selectedMedium = m; break; }
            if (m == AlertMedium.EMAIL && upref.isEmailAlert()) { selectedMedium = m; break; }
        }
        // if no match and template allows DEFAULT, apply user preference order: push > sms > email
        if (selectedMedium == null && templateHasDefault) {
            if      (upref.isPushAlert())  selectedMedium = AlertMedium.PUSH;
            else if (upref.isSmsAlert())   selectedMedium = AlertMedium.SMS;
            else if (upref.isEmailAlert()) selectedMedium = AlertMedium.EMAIL;
        }
        // No match, search the user & existing patch with that order of preference (PUSH, SMS, EMAIL)
        if ( selectedMedium == null ) {
            if ( upref.isPushAlert() ) {
                for ( AlertMediumMessage amm : alm.getMediums() ) {
                    if ( amm.getMedium() == AlertMedium.PUSH ) { selectedMedium = AlertMedium.PUSH; break; }
                }
            }
            if ( upref.isSmsAlert() && selectedMedium == null) {
                for ( AlertMediumMessage amm : alm.getMediums() ) {
                    if ( amm.getMedium() == AlertMedium.SMS ) { selectedMedium = AlertMedium.SMS; break; }
                }
            }
            if ( upref.isEmailAlert() && selectedMedium == null) {
                for ( AlertMediumMessage amm : alm.getMediums() ) {
                    if ( amm.getMedium() == AlertMedium.EMAIL ) { selectedMedium = AlertMedium.EMAIL; break; }
                }
            }
        }
        return selectedMedium;
    }


    /**
     * Find the best Medium for a given User in regard of the template preference
     * Can return NULL when no match
     */
    private AlertMediumMessage getRightMedium(AlertLocaleMessage bestLocale, AlertMedium selectedMedium) {
        // find the message variant for the selected medium in the chosen locale; fall back to DEFAULT variant
        AlertMediumMessage messageVariant = null;
        AlertMediumMessage defaultVariant = null;
        for (AlertMediumMessage mm : bestLocale.getMediums()) {
            if (mm.getMedium() == AlertMedium.DEFAULT) defaultVariant = mm;
            if (mm.getMedium() == selectedMedium) { messageVariant = mm; break; }
        }
        if (messageVariant == null) messageVariant = defaultVariant;
        return messageVariant;
    }

    /**
     * Process one alert dequeued by a worker, dispatching on its current state.
     * PENDING_QUEUE: fire open notification and transition to RUNNING or ENDED per template behavior.
     * ENDING_QUEUE:  fire close notification and transition to ENDED.
     * @param alert - the alert to process; its state is mutated in place
     * @param now   - current time used to set fireMs and compute expiration
     */
    private void processAlert(Alert alert, long now) {

        // Get the template message
        AlertTemplate template = null;
        try {
            template = alertTemplateCache.getTemplateByShortId(alert.getAlertTemplateId());
        } catch (ITNotFoundException e) {
            log.error("[alerts] Template {} not found for alert {}, moving to ENDED",
                    alert.getAlertTemplateId(), alert.getAlertId());
            alert.setState(AlertState.ENDED);
            alert.setError("alerts-template-not-found");
            alertRepository.save(alert);
            return;
        }

        // Get the targeted Users / or Silent
        if ( alert.getState() == AlertState.PENDING && template.getBehavior() == AlertBehavior.SILENT ) {
            // Silent Alarm - create message based on app default language



        } else {
            // We need to determine the targeted user list, the conditions are the following
            // - Select the user with ROLE_DEVICE_ALERTING (global or ACL) in groups where alertGroup is true
            if ( alert.getTargetedGroups() == null ) {
                alert.setState(AlertState.ENDED);
                alert.setError("alerts-target-not-found");
                alertRepository.save(alert);
                return;

            }
            HashMap<String, User> targets = new HashMap<>();
            for ( String group : alert.getTargetedGroups() ) {
                try {
                    Group g = groupsServices.getGroupByShortId(group);
                    if ( g.isAlertGroup() ) {
                        // Find users in this group
                        List<User> users = userCommon.getUsersByGroupWithRole(
                                g.getShortId(),
                                false,  // prefer to not go to sub to avoid spamming the group managers
                                true,
                                ROLE_DEVICE_ALERTING.getRoleName()
                        );
                        // Add users
                        for ( User user : users ) {
                            targets.put(user.getLogin(),user);
                        }
                    }
                } catch (ITNotFoundException ignored) {}
            }

            // We have the list of Users, and we have it only once.

            // select open or close message list depending on whether this is a firing or ending event
            List<AlertLocaleMessage> localeMessages = (alert.getState() == AlertState.ENDING_QUEUE)
                    ? template.getClose()
                    : template.getOpen();

            List<AlertSentEntry> sentList = new ArrayList<>();

            // for each
            for (User user : targets.values()) {

                // Get the locale to be used
                AlertLocaleMessage bestLocale = this.getRightAlertLocaleMessage(
                        user.getLanguage(),
                        localeMessages
                );
                if (bestLocale == null) {
                    log.warn("[alerts] No locale message for alert {} user {}, skipping", alert.getAlertId(), user.getLogin());
                    continue;
                }

                // Get the preferred Medium
                AlertMedium selectedMedium = getRightMedium(user, bestLocale, template);

                if (selectedMedium == null) {
                    log.warn("[alerts] No compatible medium for alert {} user {}, skipping", alert.getAlertId(), user.getLogin());
                    continue;
                }

                // Find the associated AlertMediumMessage
                AlertMediumMessage messageVariant = getRightMedium(bestLocale, selectedMedium);
                if (messageVariant == null) {
                    log.warn("[alerts] No message variant for medium {} alert {} user {}, skipping",
                            selectedMedium, alert.getAlertId(), user.getLogin());
                    continue;
                }

                // >>>>>>>>> @TODO 
                // generated the message based on this choice language / medium avec la bonne complétion d'information.
                String renderedMessage = renderMessage(messageVariant.getMessage(), alert.getParameters());

                // @TODO: deliver renderedMessage via selectedMedium for this user

                List<AlertSentState> states = new ArrayList<>();
                AlertSentState primaryState = new AlertSentState();
                primaryState.setMedium(selectedMedium);
                primaryState.setSent(false);  // updated by the delivery layer
                primaryState.setAck(false);
                states.add(primaryState);

                // on genere en plus un message popup si POPUP est dans la liste du template, on utilise la meme langue que précédemment.
                if (template.getPreferred().contains(AlertMedium.POPUP)) {
                    // @TODO: persist popup entry in the popup history table
                    AlertSentState popupState = new AlertSentState();
                    popupState.setMedium(AlertMedium.POPUP);
                    popupState.setSent(false);
                    popupState.setAck(false);
                    states.add(popupState);
                }

                AlertSentEntry entry = new AlertSentEntry();
                entry.setUserId(user.getLogin());
                entry.setState(states);
                sentList.add(entry);



                // verify if user personal data are accessible (if not skip this one)
                if (!user.isPersonalDataAccessible()) {
                    log.debug("[alerts] Skipping user {} - personal data not accessible", user.getLogin());



                    continue;
                }


            }

            alert.setSent(sentList);
        }








        switch (alert.getState()) {

            case PENDING_QUEUE -> {
                alert.setFireMs(now);
                AlertBehavior behavior = template.getBehavior();
                switch (behavior) {
                    case SILENT -> {
                        // Audit trace only — no notification sent
                        // @TODO - besoin du message sinon ca sert à rien
                        auditIntegration.auditLog(
                                ModuleCatalog.Modules.ALERTS,
                                ActionCatalog.getActionName(ActionCatalog.Actions.AUDIT_ALERT_REPORT),
                                alert.getAlertDefRef(),
                                "Alert '{0}' type {1} created for tenant {2}",
                                new String[]{alert.getAlertId(), alert.getAlertTemplateId(), alert.getTargetedGroups()}
                        );
                        log.info("[alerts] SILENT alert {} processed", alert.getAlertId());
                        alert.setState(AlertState.ENDED);
                    }
                    case FIRE_FORGET -> {
                        // @TODO: deliver open notification via configured channels
                        log.info("[alerts] FIRE_FORGET alert {} fired", alert.getAlertId());
                        alert.setState(AlertState.ENDED);
                    }
                    case FIRE_TO_END, FIRE_UNTIL -> {
                        // @TODO: deliver open notification via configured channels
                        log.info("[alerts] {} alert {} fired, now RUNNING", behavior, alert.getAlertId());
                        alert.setState(AlertState.RUNNING);
                        if (template.getDurationMs() > 0) {
                            alert.setExpirationMs(now + template.getDurationMs());
                        }
                    }
                    default -> {
                        log.warn("[alerts] Unknown behavior {} for alert {}, moving to ENDED", behavior, alert.getAlertId());
                        alert.setState(AlertState.ENDED);
                    }
                }
                alertRepository.save(alert);
            }

            case ENDING_QUEUE -> {
                // @TODO: deliver close notification via configured channels (FIRE_TO_END only)
                log.info("[alerts] Alert {} close-notification processed, moving to ENDED", alert.getAlertId());
                alert.setState(AlertState.ENDED);
                alertRepository.save(alert);
            }

            default -> log.warn("[alerts] Worker dequeued alert {} in unexpected state {}",
                    alert.getAlertId(), alert.getState());
        }
    }

    /**
     * Substitute positional parameters into a message template.
     * Placeholders are 1-based: {1} is replaced by parameters.get(0), {2} by parameters.get(1), etc.
     * @param messageTemplate - raw message string with {n} placeholders
     * @param parameters      - ordered list of values to inject
     * @return the rendered message with all placeholders replaced
     */
    private String renderMessage(String messageTemplate, List<String> parameters) {
        if (parameters == null || parameters.isEmpty()) return messageTemplate;
        String result = messageTemplate;
        for (int i = 0; i < parameters.size(); i++) {
            result = result.replace("{" + (i + 1) + "}", parameters.get(i));
        }
        return result;
    }

    // ================================================================================================================
    // PUBLIC API
    // ================================================================================================================

    /**
     * Create a new alert, persist it, and enqueue it for immediate async processing.
     * For FIRE_TO_END and FIRE_UNTIL behaviors, a duplicate alert (same alertId already in
     * PENDING, PENDING_QUEUE, or RUNNING) is silently ignored and null is returned.
     * @param alertId         - stable business identifier, already instantiated
     * @param alertDefRef     - source module reference key
     * @param alertTemplateId - shortId of the AlertTemplate to use
     * @param targetedGroup   - group identifier used as the broadcast perimeter for user fan-out
     * @param parameters      - positional substitution values ({1}, {2}, ...)
     * @param requestMs       - event detection timestamp (ms since epoch)
     * @return the persisted Alert in PENDING_QUEUE state, or null when rejected as a duplicate
     * @throws ITParseException when the referenced template does not exist
     */
    public Alert createAlert(
            String alertId,
            String alertDefRef,
            String alertTemplateId,
            String targetedGroup,
            List<String> parameters,
            long requestMs
    ) throws ITParseException {

        AlertTemplate template;
        try {
            template = alertTemplateCache.getTemplateByShortId(alertTemplateId);
        } catch (ITNotFoundException e) {
            log.warn("[alerts] createAlert rejected: template {} not found", alertTemplateId);
            throw new ITParseException("alerts-template-not-found");
        }

        // Deduplication: reject when an equivalent alert is already active
        AlertBehavior behavior = template.getBehavior();
        if (behavior == AlertBehavior.FIRE_TO_END || behavior == AlertBehavior.FIRE_UNTIL) {
            List<Alert> active = alertRepository.findActiveAlertsByAlertId(
                    alertId,
                    Arrays.asList(AlertState.PENDING, AlertState.PENDING_QUEUE, AlertState.RUNNING)
            );
            if (!active.isEmpty()) {
                log.debug("[alerts] Alert {} already active (state={}), duplicate ignored",
                        alertId, active.getFirst().getState());
                return null;
            }
        }

        String publicAccessId = RandomString.getRandomString(24);
        // Persist first as PENDING to obtain the MongoDB id, then transition to PENDING_QUEUE
        Alert alert = Alert.newAlert(alertId, alertDefRef, alertTemplateId, targetedGroup, parameters, requestMs, publicAccessId);
        alert = alertRepository.save(alert);
        log.debug("[alerts] Alert {} created (template={}, group={})", alertId, alertTemplateId, targetedGroup);
        auditIntegration.auditLog(
                ModuleCatalog.Modules.ALERTS,
                ActionCatalog.getActionName(ActionCatalog.Actions.AUDIT_ALERT_CREATED),
                alertDefRef,
                "Alert '{0}' type {1} created for tenant {2}",
                new String[]{alertId, alertTemplateId, targetedGroup}
        );

        enqueue(alert);
        return alert;
    }

    /**
     * Signal the end of a RUNNING alert: transition to ENDING and enqueue for close-notification delivery.
     * @param alertId - stable business identifier of the RUNNING alert to end
     * @throws ITNotFoundException when no RUNNING alert is found for this alertId
     */
    public void endAlert(String alertId) throws ITNotFoundException {
        Alert alert = alertRepository.findOneAlertByAlertId(alertId);
        if (alert == null || alert.getState() != AlertState.RUNNING) {
            log.warn("[alerts] endAlert called on non-RUNNING alert {}", alertId);
            throw new ITNotFoundException("alerts-not-running");
        }
        alert.setState(AlertState.ENDING);
        alertRepository.save(alert);
        log.debug("[alerts] Alert {} moved to ENDING_QUEUE and enqueued", alertId);
        auditIntegration.auditLog(
                ModuleCatalog.Modules.ALERTS,
                ActionCatalog.getActionName(ActionCatalog.Actions.AUDIT_ALERT_ENDED),
                alert.getAlertDefRef(),
                "Alert '{0}' type {1} ended for tenant {2}",
                new String[]{alertId, alert.getAlertTemplateId(), alert.getTargetedGroups()}
        );
        enqueue(alert);
    }

    // ================================================================================================================
    // SCHEDULED MAINTENANCE
    // ================================================================================================================

    /**
     * Periodically expire RUNNING alerts whose expirationMs has elapsed.
     * Runs every 30 seconds; not queue-driven since no external event triggers expiration.
     */
    @Scheduled(fixedDelay = 30_000, initialDelay = 60_000)
    protected void checkExpiredAlerts() {
        long now = Now.NowUtcMs();
        List<Alert> expired = alertRepository.findExpiredRunningAlerts(AlertState.RUNNING, now);
        for (Alert alert : expired) {
            log.info("[alerts] Alert {} expired, moving to ENDED", alert.getAlertId());
            alert.setState(AlertState.ENDED);
            alertRepository.save(alert);
        }
    }

    /**
     * Daily purge: delete ENDED alerts older than alerts.max.retention.ms.
     * Runs once per day with a 1-hour initial delay to avoid startup contention.
     */
    @Scheduled(fixedRateString = "PT24H", initialDelayString = "PT1H")
    public void purgeOldAlerts() {
        long cutoffMs = Now.NowUtcMs() - alertsConfig.getAlertsMaxRetentionMs();
        log.info("[alerts] Purging ENDED alerts with requestMs before {}", cutoffMs);
        try {
            alertRepository.deleteByStateAndRequestMsBefore(AlertState.ENDED, cutoffMs);
            log.info("[alerts] Alert purge complete");
        } catch (Exception e) {
            log.error("[alerts] Alert purge failed: {}", e.getMessage());
        }
    }
}
