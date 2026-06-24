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

import com.disk91.alerts.api.interfaces.AlertPopupCountResponseItf;
import com.disk91.alerts.api.interfaces.AlertPopupResponseItf;
import com.disk91.alerts.config.AlertsConfig;
import com.disk91.alerts.mdb.entities.sub.AlertCriticality;
import com.disk91.alerts.pdb.entities.AlertPopup;
import com.disk91.alerts.pdb.repositories.AlertPopupRepository;
import com.disk91.common.tools.Now;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AlertPopupService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected AlertPopupRepository alertPopupRepository;

    @Autowired
    protected AlertsConfig alertsConfig;


    /**
     * Create and persist a popup notification for a user.
     * Called by AlertService when the selected delivery medium is POPUP.
     * @param userLogin - targeted user login
     * @param alertId - source alert identifier
     * @param message - rendered localized message
     * @param criticality - alert criticality level
     * @param timeMs - alert emission timestamp
     */
    public void createPopup(
            String userLogin,
            String alertId,
            String message,
            AlertCriticality criticality,
            long timeMs
    ) {
        AlertPopup popup = AlertPopup.create(userLogin, alertId, message, criticality, timeMs);
        alertPopupRepository.save(popup);
        log.debug("[alerts] Popup created for user {} alertId {}", userLogin, alertId);
    }

    /**
     * Return the popup list for the requesting user.
     * Includes all unread popups and read popups from the last two days, capped at MAX_POPUP_COUNT.
     * @param userLogin - requesting user login
     * @return ordered list of popup response objects (newest first)
     */
    public List<AlertPopupResponseItf> getPopupsForUser(String userLogin) {
        long sinceMs = Now.NowUtcMs() - alertsConfig.getAlertsPopupMaxDisplayedMs();
        List<AlertPopup> popups = alertPopupRepository.findRecentOrUnread(
                userLogin,
                sinceMs,
                PageRequest.of(0, alertsConfig.getAlertsPopupMaxDisplayed())
        );

        List<AlertPopupResponseItf> result = new ArrayList<>();
        for (AlertPopup p : popups) {
            AlertPopupResponseItf r = new AlertPopupResponseItf();
            r.buildFrom(p);
            result.add(r);
        }
        return result;
    }

    /**
     * Return the count of unread popup notifications for the requesting user (badge count).
     * @param userLogin - requesting user login
     * @return count response containing the number of unread popups
     */
    public AlertPopupCountResponseItf getUnreadCount(String userLogin) {
        long count = alertPopupRepository.countByUserLoginAndViewedMs(userLogin, 0L);
        AlertPopupCountResponseItf r = new AlertPopupCountResponseItf();
        r.setUnreadCount(count);
        return r;
    }

    /**
     * Return popups created after the given timestamp for the requesting user.
     * Used by the toaster polling mechanism: the response contains all data needed
     * to display the toaster without a second API call.
     * Does not affect the viewed/unread state of any entry.
     * @param userLogin - requesting user login
     * @param sinceMs - lower bound timestamp (exclusive), client-managed
     * @return list of new popup entries ordered oldest first
     */
    public List<AlertPopupResponseItf> getNewPopupsSince(String userLogin, long sinceMs) {
        List<AlertPopup> popups = alertPopupRepository
                .findByUserLoginAndTimeMsGreaterThanOrderByTimeMsAsc(userLogin, sinceMs);

        List<AlertPopupResponseItf> result = new ArrayList<>();
        for (AlertPopup p : popups) {
            AlertPopupResponseItf r = new AlertPopupResponseItf();
            r.buildFrom(p);
            result.add(r);
        }
        return result;
    }

    /**
     * Mark all unread popups as viewed for the requesting user.
     * @param userLogin - requesting user login
     */
    public void markAllViewed(String userLogin) {
        int updated = alertPopupRepository.markAllViewedByUser(userLogin, Now.NowUtcMs());
        log.debug("[alerts] Marked {} popup(s) as viewed for user {}", updated, userLogin);
    }

    /**
     * Scheduled hourly purge of popup entries older than alerts.max.history.ms.
     */
    @Scheduled(fixedDelay = 3600_0000)
    public void purgeExpiredPopups() {
        long cutoffMs = Now.NowUtcMs() - alertsConfig.getAlertsMaxHistoryMs();
        alertPopupRepository.deleteByTimeMsBefore(cutoffMs);
        log.debug("[alerts] Purged popup entries older than {} ms", alertsConfig.getAlertsMaxHistoryMs());
    }
}
