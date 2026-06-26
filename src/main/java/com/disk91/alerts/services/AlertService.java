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
import com.disk91.alerts.mdb.entities.sub.*;
import com.disk91.alerts.mdb.repositories.AlertRepository;
import com.disk91.audit.integration.AuditIntegration;
import com.disk91.common.config.CommonConfig;
import com.disk91.common.config.ModuleCatalog;
import com.disk91.common.tools.*;
import com.disk91.common.tools.exceptions.ITNotFoundException;
import com.disk91.common.tools.exceptions.ITParseException;
import com.disk91.devices.services.DeviceCache;
import com.disk91.groups.mdb.entities.Group;
import com.disk91.groups.services.GroupsServices;
import com.disk91.users.mdb.entities.User;
import com.disk91.users.mdb.entities.sub.UserAlertPreference;
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

import com.disk91.alerts.api.interfaces.AlertUserHistoryListResponseItf;
import com.disk91.alerts.api.interfaces.AlertUserHistoryResponseItf;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.disk91.users.services.UsersRolesCache.StandardRoles.ROLE_DEVICE_ALERTING;
import static com.disk91.users.services.UsersRolesCache.StandardRoles.ROLE_GOD_ADMIN;

@Service
public class AlertService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected AlertRepository alertRepository;

    @Autowired
    protected AlertTemplateCache alertTemplateCache;

    @Autowired
    protected CommonConfig commonConfig;

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

    @Autowired
    protected DeviceCache deviceCache;

    @Autowired
    protected EmailTools emailTools;

    @Autowired
    protected FirebaseTools firebaseTools;

    @Autowired
    protected AlertPopupService alertPopupService;

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
        if ( alert.getState() == AlertState.PENDING_QUEUE && template.getBehavior() == AlertBehavior.SILENT ) {

            // Resolve the first available group for context (GROUP_NAME parameter, etc.)
            Group platformGroup = null;
            if (alert.getTargetedGroups() != null && !alert.getTargetedGroups().isEmpty()) {
                try {
                    platformGroup = groupsServices.getGroupByShortId(alert.getTargetedGroups().getFirst());
                } catch (ITNotFoundException ignored) {}
            }

            // Render the message using the platform default language
            String renderedMessage = "";
            AlertLocaleMessage bestLocale = getRightAlertLocaleMessage(
                    commonConfig.getCommonLangDefault(), template.getOpen()
            );
            if (bestLocale != null) {
                // Prefer DEFAULT medium, then POPUP, then first available in the locale
                AlertMediumMessage messageVariant = getRightMedium(bestLocale, AlertMedium.DEFAULT);
                if (messageVariant == null) messageVariant = getRightMedium(bestLocale, AlertMedium.POPUP);
                if (messageVariant == null && !bestLocale.getMediums().isEmpty()) {
                    messageVariant = bestLocale.getMediums().getFirst();
                }
                if (messageVariant != null) {
                    renderedMessage = renderMessage(alert, template, messageVariant.getMessage(), platformGroup, null);
                }
            }
            // @TODO - we only have an audit type REPORT when the alert is SILENT / is that what we want ?
            //         we have audit on create for all by the way but not the message (not really a problem)
            auditIntegration.auditLog(
                    ModuleCatalog.Modules.ALERTS,
                    ActionCatalog.getActionName(ActionCatalog.Actions.AUDIT_ALERT_REPORT),
                    alert.getAlertDefRef(),
                    "Silent alert '{0}' type {1} for tenant {2}: {3}",
                    new String[]{alert.getAlertId(), alert.getAlertTemplateId(), String.join(", ", alert.getTargetedGroups()), renderedMessage}
            );
            log.info("[alerts] SILENT alert {} processed", alert.getAlertId());

        } else {
            // We need to determine the targeted user list, the conditions are the following
            // - Select the user with ROLE_DEVICE_ALERTING (global or ACL) in groups where alertGroup is true
            if (alert.getTargetedGroups() == null) {
                alert.setState(AlertState.ENDED);
                alert.setError("alerts-target-not-found");
                alertRepository.save(alert);
                return;

            }
            HashMap<String, User> targets = new HashMap<>();
            HashMap<String, Group> groups = new HashMap<>();
            for (String group : alert.getTargetedGroups()) {
                log.debug("[alerts] targeted group {} found", group);
                try {
                    Group g = groupsServices.getGroupByShortId(group);
                    if (g.isAlertGroup()) {
                        // Find users in this group
                        List<User> users = userCommon.getUsersByGroupWithRole(
                                g.getShortId(),
                                false,  // prefer to not go to sub to avoid spamming the group managers
                                true,
                                ROLE_DEVICE_ALERTING.getRoleName()
                        );
                        // Add users
                        for (User user : users) {
                            targets.put(user.getLogin(), user);
                            groups.put(user.getLogin(), g);
                        }
                    } else {
                        log.debug("[alerts] group skipped {} : not an alert group", group);
                    }
                } catch (ITNotFoundException ignored) {
                    log.debug("[alerts] group {} not found", group);
                }
            }

            // We have the list of Users, and we have it only once.

            // select open or close message list depending on whether this is a firing or ending event
            List<AlertLocaleMessage> localeMessages = (alert.getState() == AlertState.ENDING_QUEUE)
                    ? template.getClose()
                    : template.getOpen();

            // for each
            for (User user : targets.values()) {
                log.debug("[alerts] targeted user {}", user.getLogin());

                // Get the locale to be used
                AlertLocaleMessage bestLocale = this.getRightAlertLocaleMessage(
                        user.getLanguage(),
                        localeMessages
                );
                if (bestLocale == null) {
                    log.warn("[alerts] No locale message for alert {} user {}, skipping", alert.getAlertId(), user.getLogin());
                    continue;
                } else log.debug("[alerts] Selected locale {} for user  {} found", bestLocale.getLocale(), user.getLogin());

                // Get the preferred Medium
                AlertMedium selectedMedium = getRightMedium(user, bestLocale, template);

                if (selectedMedium == null) {
                    log.warn("[alerts] No compatible medium for alert {} user {}, skipping", alert.getAlertId(), user.getLogin());
                    continue;
                } else log.debug("[alerts] Selected medium {} for user  {} found", selectedMedium, user.getLogin());

                if (user.isPersonalDataAccessible()) {

                    // Find the associated AlertMediumMessage
                    AlertMediumMessage messageVariant = getRightMedium(bestLocale, selectedMedium);
                    if (messageVariant == null) {
                        log.warn("[alerts] No message variant for medium {} alert {} user {}, skipping",
                                selectedMedium, alert.getAlertId(), user.getLogin());
                        continue;
                    }

                    // generated the message based on this choice language / medium
                    String renderedMessage = renderMessage(
                            alert,
                            template,
                            messageVariant.getMessage(),
                            groups.get(user.getLogin()),
                            user
                    );
                    String renderedTitle = "";
                    if (messageVariant.getTitle() != null) {
                        renderedTitle = renderMessage(
                                alert,
                                template,
                                messageVariant.getTitle(),
                                groups.get(user.getLogin()),
                                user
                        );
                    }

                    alert.upsertSent(user.getLogin(), selectedMedium, false, false, "alerts-alert-not-sent");
                    alertRepository.save(alert);

                    switch (selectedMedium) {
                        case EMAIL -> {
                            user.setKeys(commonConfig.getEncryptionKey(), commonConfig.getApplicationKey());
                            try {
                                emailTools.send(
                                        user.getEncEmail(),
                                        renderedMessage,
                                        renderedTitle,
                                        (alertsConfig.getAlertsMailSender().isEmpty()) ? commonConfig.getCommonMailSender() : alertsConfig.getAlertsMailSender()
                                );
                                alert.upsertSent(user.getLogin(), selectedMedium, true, false, "");
                            } catch (ITParseException x) {
                                alert.upsertSent(user.getLogin(), selectedMedium, false, false, "alerts-failed-to-get-email");
                            }
                            user.cleanKeys();
                            alertRepository.save(alert);
                        }
                        case SMS -> {
                            // @TODO
                            alert.upsertSent(user.getLogin(), selectedMedium, false, false, "SMS Not yet implemented");
                            alertRepository.save(alert);
                            log.warn("[alerts] SMS not yet implemented");
                        }
                        case PUSH -> {
                            user.setKeys(commonConfig.getEncryptionKey(), commonConfig.getApplicationKey());
                            try {
                                if (user.getPushAddress() == null) {
                                    alert.upsertSent(user.getLogin(), selectedMedium, false, false, "alerts-failed-no-push-address");
                                } else {
                                    firebaseTools.sendPush(
                                            user.getEncPushAddress(),
                                            renderedTitle,
                                            renderedMessage
                                    );
                                    alert.upsertSent(user.getLogin(), selectedMedium, true, false, "");
                                }
                            } catch (ITParseException x) {
                                alert.upsertSent(user.getLogin(), selectedMedium, false, false, "alerts-failed-send-push");
                            }
                            user.cleanKeys();
                            alertRepository.save(alert);
                        }
                        case WHATSAPP -> {
                            // @TODO
                            alert.upsertSent(user.getLogin(), selectedMedium, false, false, "WHATSAPP Not yet implemented");
                            alertRepository.save(alert);
                            log.warn("[alerts] WHATSAPP not yet implemented");
                        }
                        case TOPIC -> {
                            // @TODO
                            alert.upsertSent(user.getLogin(), selectedMedium, false, false, "TOPIC Not yet implemented");
                            alertRepository.save(alert);
                            log.warn("[alerts] TOPIC not yet implemented");
                        }
                        case WEBHOOK -> {
                            // @TODO
                            alert.upsertSent(user.getLogin(), selectedMedium, false, false, "WEBHOOK Not yet implemented");
                            alertRepository.save(alert);
                            log.warn("[alerts] WEBHOOK not yet implemented");
                        }
                    }
                } else {
                    log.debug("[alerts] User {} presonal data not accesible",  user.getLogin());
                    alert.upsertSent(user.getLogin(), selectedMedium, false, false, "alerts-user-no-personal-data");
                    alertRepository.save(alert);
                }

                // Manage the POPUP
                if (template.getPreferred().contains(AlertMedium.POPUP)) {

                    AlertMediumMessage messageVariant = getRightMedium(bestLocale, AlertMedium.POPUP);
                    if (messageVariant != null) {

                        // generated the message based on this choice language / medium
                        String renderedMessage = renderMessage(
                                alert,
                                template,
                                messageVariant.getMessage(),
                                groups.get(user.getLogin()),
                                user
                        );

                        // write the alert event in the popup table
                        alertPopupService.createPopup(
                                user.getLogin(),
                                alert.getAlertId(),
                                renderedMessage,
                                template.getCriticality(),
                                alert.getRequestMs()
                        );

                        // Update state
                        alert.upsertSent(user.getLogin(), AlertMedium.POPUP, true, true, "");
                    } else {
                        alert.upsertSent(user.getLogin(), AlertMedium.POPUP, false, false, "alerts-no-popup-config");
                    }

                }

            } // loop on users
        }

        // Now, we update the alert behavior
        switch (alert.getState()) {

            case PENDING_QUEUE -> {
                alert.setFireMs(now);
                AlertBehavior behavior = template.getBehavior();
                switch (behavior) {
                    case SILENT, FIRE_FORGET -> alert.setState(AlertState.ENDED);
                    case FIRE_TO_END, FIRE_UNTIL -> {
                        alert.setState(AlertState.RUNNING);
                        if (template.getDurationMs() > 0) {
                            alert.setExpirationMs(now + template.getDurationMs());
                        } else alert.setExpirationMs(now + 10*Now.ONE_MINUTE);
                    }
                    default -> {
                        log.warn("[alerts] Unknown behavior {} for alert {}, moving to ENDED", behavior, alert.getAlertId());
                        alert.setState(AlertState.ENDED);
                    }
                }
                alertRepository.save(alert);
            }

            case ENDING_QUEUE -> {
                log.debug("[alerts] Alert {} close-notification processed, moving to ENDED", alert.getAlertId());
                alert.setState(AlertState.ENDED);
                alert.setExpirationMs(now);
                alertRepository.save(alert);
            }

            default -> {
                log.warn("[alerts] Worker dequeued alert {} in unexpected state {}", alert.getAlertId(), alert.getState());
            }
        }
    }

    /**
     * Substitute positional parameters into a message template.
     * Placeholders are 1-based: {1} is replaced by parameters.get(0), {2} by parameters.get(1), etc.
     * @param alert - the alert
     * @param template - the related alert template
     * @param messageTemplate - the right message template
     * @param group - the user associated group
     * @param user - the targeted user
     * @return the rendered message with all placeholders replaced
     */
    private String renderMessage(
            Alert alert,
            AlertTemplate template,
            String messageTemplate,
            Group group,
            User user
    ) {
        // compose the parameter list based on the template
        ArrayList<String> parameters = new ArrayList<>();
        if (user != null) user.setKeys(commonConfig.getEncryptionKey(), commonConfig.getApplicationKey());
        for (AlertParameterEntry p : template.getParameters() ) {
            switch (p.getType()) {
                case DEVICE_ID -> parameters.add(alert.getDeviceId());
                case DEVICE_NAME -> {
                    try {
                        com.disk91.devices.mdb.entities.Device d = deviceCache.getDevice(alert.getDeviceId());
                        parameters.add(d.getName());
                    } catch (ITNotFoundException x) {
                        parameters.add("Unknown");
                    }
                }
                case GROUP_NAME -> parameters.add(group != null ? group.getName() : "");
                case USER_FIRSTNAME -> {
                    if (user == null) { parameters.add(""); break; }
                    try {
                        parameters.add(user.getEncProfileFirstName());
                    } catch (ITParseException x) {
                        parameters.add("");
                    }
                }
                case USER_LASTNAME -> {
                    if (user == null) { parameters.add(""); break; }
                    try {
                        parameters.add(user.getEncProfileLastName());
                    } catch (ITParseException x) {
                        parameters.add("");
                    }
                }
                case USER_GENDER -> {
                    if (user == null) { parameters.add(""); break; }
                    try {
                        parameters.add(user.getEncProfileGender());
                    } catch (ITParseException x) {
                        parameters.add("");
                    }
                }
                case ALERT_TIME -> {
                    try {
                        String tzId = (user != null) ? user.getEncProfileTimezone() : null;
                        TimeZone tz = (tzId != null && !tzId.isBlank()) ? TimeZone.getTimeZone(tzId) : null;
                        parameters.add(DateConverters.timestampToTime(alert.getFireMs(), tz));
                    } catch (ITParseException x) {
                        parameters.add(DateConverters.timestampToTime(alert.getFireMs(), null));
                    }
                }
                case ALERT_DATE_TIME -> {
                    try {
                        String tzId = (user != null) ? user.getEncProfileTimezone() : null;
                        TimeZone tz = (tzId != null && !tzId.isBlank()) ? TimeZone.getTimeZone(tzId) : null;
                        parameters.add(DateConverters.timestampToDateTime(alert.getFireMs(), tz));
                    } catch (ITParseException x) {
                        parameters.add(DateConverters.timestampToDateTime(alert.getFireMs(), null));
                    }
                }
                case CUSTOM_PARAM -> parameters.add(p.getParam());
                case SERVICE_NAME -> parameters.add(commonConfig.getCommonServiceName());
                case SERVICE_HOME -> parameters.add(commonConfig.getCommonServiceFrontBaseUrl());
                case ALERT_LINK -> {
                    String url;
                    if ( p.getParam() != null && !p.getParam().isEmpty()) {
                        // prefer the user provided url (fully qualified when starts by http)
                        if ( p.getParam().startsWith("http") ) {
                            url = p.getParam();
                        } else {
                            url = commonConfig.getCommonServiceFrontBaseUrl() + p.getParam();
                        }
                        url = url.replace("{aid}", alert.getAlertId());
                        url = url.replace("{did}", alert.getDeviceId());
                        url = url.replace("{pubid}", alert.getPublicAccessId());
                    } else {
                        url = commonConfig.getCommonServiceFrontBaseUrl() + alertsConfig.getAlertsDirectLink();
                        url = url.replace("!aid!", alert.getAlertId());
                        url = url.replace("!did!", alert.getDeviceId());
                        url = url.replace("!pubid!", alert.getPublicAccessId());
                    }
                    parameters.add(url);
                }
            }
        }

        if (user != null) user.cleanKeys();

        // Now replace the parameters in the mesage
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
     * @param targetedGroups  - list of group identifiers used as the broadcast perimeter for user fan-out
     * @param parameters      - positional substitution values ({1}, {2}, ...)
     * @param requestMs       - event detection timestamp (ms since epoch)
     * @return the persisted Alert in PENDING_QUEUE state, or null when rejected as a duplicate
     * @throws ITParseException when the referenced template does not exist
     */
    public Alert createAlert(
            String alertId,
            String alertDefRef,
            String alertTemplateId,
            String deviceId,
            List<String> targetedGroups,
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
        Alert alert = Alert.newAlert(alertId, alertDefRef, alertTemplateId, deviceId, targetedGroups, parameters, requestMs, publicAccessId);
        alert = alertRepository.save(alert);
        log.debug("[alerts] Alert {} created (template={}, groups={})", alertId, alertTemplateId, targetedGroups);
        auditIntegration.auditLog(
                ModuleCatalog.Modules.ALERTS,
                ActionCatalog.getActionName(ActionCatalog.Actions.AUDIT_ALERT_CREATED),
                alertDefRef,
                "Alert '{0}' type {1} created for tenant {2}",
                new String[]{alertId, alertTemplateId, String.join(", ", targetedGroups)}
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
                new String[]{alertId, alert.getAlertTemplateId(), String.join(", ", alert.getTargetedGroups())}
        );
        enqueue(alert);
    }

    /**
     * Return a paginated list of alerts for the requesting user, ordered by requestMs descending.
     * The role is resolved internally: ROLE_GOD_ADMIN users receive all alerts (unfiltered by user)
     * with full sent list and targetedGroups; regular users receive only alerts they appear in,
     * with their own delivery entry and empty targetedGroups.
     * @param userLogin   - requesting user login
     * @param page        - 0-based page number
     * @param size        - page size, 1–100
     * @param templateIds - optional list of alertTemplateId values to filter on; null or empty means no filter
     * @return paginated history response
     * @throws ITParseException  when page or size are out of range
     * @throws ITNotFoundException when the requesting user cannot be found
     */
    public AlertUserHistoryListResponseItf getUserAlertHistory(
            String userLogin,
            int page,
            int size,
            List<String> templateIds
    ) throws ITParseException, ITNotFoundException {
        if (size < 1 || size > 100) throw new ITParseException("alerts-history-invalid-page-size");
        if (page < 0) throw new ITParseException("alerts-history-invalid-page");

        // Resolve role: load the user and check for GOD_ADMIN
        User requester = userCommon.getUser(userLogin);
        boolean isAdmin = requester.isInRole(ROLE_GOD_ADMIN);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "requestMs"));

        Page<Alert> alertPage;
        if (isAdmin) {
            // Admin sees all alerts, optionally filtered by template
            if (templateIds != null && !templateIds.isEmpty()) {
                alertPage = alertRepository.findByAlertTemplateIdIn(templateIds, pageable);
            } else {
                alertPage = alertRepository.findAll(pageable);
            }
        } else {
            // Regular user sees only alerts where they appear in the sent array
            if (templateIds != null && !templateIds.isEmpty()) {
                alertPage = alertRepository.findByUserInSentAndTemplateIdIn(userLogin, templateIds, pageable);
            } else {
                alertPage = alertRepository.findByUserInSent(userLogin, pageable);
            }
        }

        List<AlertUserHistoryResponseItf> items = new ArrayList<>();
        for (Alert alert : alertPage.getContent()) {
            AlertUserHistoryResponseItf r = new AlertUserHistoryResponseItf();
            r.buildFrom(alert, userLogin, isAdmin);
            items.add(r);
        }

        AlertUserHistoryListResponseItf response = new AlertUserHistoryListResponseItf();
        response.setTotal(alertPage.getTotalElements());
        response.setPage(page);
        response.setSize(size);
        response.setAlerts(items);
        return response;
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
