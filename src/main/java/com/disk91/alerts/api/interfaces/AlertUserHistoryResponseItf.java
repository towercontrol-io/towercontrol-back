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
package com.disk91.alerts.api.interfaces;

import com.disk91.alerts.mdb.entities.Alert;
import com.disk91.alerts.mdb.entities.sub.AlertSentEntry;
import com.disk91.alerts.mdb.entities.sub.AlertState;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.ArrayList;
import java.util.List;

/**
 * AlertUserHistoryResponseItf - One alert entry in the user's personal alert history.
 * Sensitive fields (id, publicAccessId, targetedGroups) are excluded.
 * The sent field contains only the requesting user's own delivery record.
 */
@Tag(name = "Alert history entry", description = "Single alert entry in the authenticated user's history")
public class AlertUserHistoryResponseItf {

    @Schema(description = "Stable business identifier of the alert",
            example = "alert-temperature-high-123456", requiredMode = Schema.RequiredMode.REQUIRED)
    protected String alertId;

    @Schema(description = "Source module reference key",
            example = "dev-DEVICEID", requiredMode = Schema.RequiredMode.REQUIRED)
    protected String alertDefRef;

    @Schema(description = "Short identifier of the alert template used",
            example = "ABCDEF", requiredMode = Schema.RequiredMode.REQUIRED)
    protected String alertTemplateId;

    @Schema(description = "Device identifier associated with this alert",
            example = "123456789", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    protected String deviceId;

    @Schema(description = "Current lifecycle state of the alert",
            example = "ENDED", requiredMode = Schema.RequiredMode.REQUIRED)
    protected AlertState state;

    @Schema(description = "Timestamp when the triggering event was detected (ms since epoch)",
            example = "1749600000000", requiredMode = Schema.RequiredMode.REQUIRED)
    protected long requestMs;

    @Schema(description = "Timestamp when the alert was processed and the notification fired; 0 until fired",
            example = "1749600001000", requiredMode = Schema.RequiredMode.REQUIRED)
    protected long fireMs;

    @Schema(description = "Timestamp when the alert auto-closes; 0 means no automatic expiration or ended",
            example = "0", requiredMode = Schema.RequiredMode.REQUIRED)
    protected long expirationMs;

    @Schema(description = "Error description if processing failed; empty on success",
            example = "", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    protected String error;

    @Schema(description = "Targeted group identifiers; populated for ROLE_GOD_ADMIN only, empty list for regular users",
            requiredMode = Schema.RequiredMode.REQUIRED)
    protected List<String> targetedGroups;

    @Schema(description = "Delivery records; all users for ROLE_GOD_ADMIN, only the requesting user for regular users",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    protected List<AlertSentEntry> sent;

    // ==========================
    // Builder

    /**
     * Populate this response from an Alert entity.
     * Admin receives full sent list and targetedGroups; regular user receives only their own sent entry and empty targetedGroups.
     * @param alert - source entity
     * @param userLogin - requesting user login used to filter the sent array when not admin
     * @param isAdmin - when true, expose full sent list and targetedGroups
     */
    public void buildFrom(Alert alert, String userLogin, boolean isAdmin) {
        this.alertId = alert.getAlertId();
        this.alertDefRef = alert.getAlertDefRef();
        this.alertTemplateId = alert.getAlertTemplateId();
        this.deviceId = alert.getDeviceId();
        this.state = alert.getState();
        this.requestMs = alert.getRequestMs();
        this.fireMs = alert.getFireMs();
        this.expirationMs = alert.getExpirationMs();
        this.error = alert.getError();

        if (isAdmin) {
            // Admin sees the full delivery list and the targeted groups
            this.targetedGroups = alert.getTargetedGroups() != null ? alert.getTargetedGroups() : new ArrayList<>();
            this.sent = alert.getSent() != null ? alert.getSent() : new ArrayList<>();
        } else {
            // Regular user sees only their own delivery entry and no group information
            this.targetedGroups = new ArrayList<>();
            this.sent = new ArrayList<>();
            if (alert.getSent() != null) {
                for (AlertSentEntry entry : alert.getSent()) {
                    if (userLogin.equals(entry.getUserLogin())) {
                        this.sent.add(entry);
                        break;
                    }
                }
            }
        }
    }

    // ==========================
    // Getters & Setters

    public String getAlertId() { return alertId; }
    public void setAlertId(String alertId) { this.alertId = alertId; }

    public String getAlertDefRef() { return alertDefRef; }
    public void setAlertDefRef(String alertDefRef) { this.alertDefRef = alertDefRef; }

    public String getAlertTemplateId() { return alertTemplateId; }
    public void setAlertTemplateId(String alertTemplateId) { this.alertTemplateId = alertTemplateId; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public AlertState getState() { return state; }
    public void setState(AlertState state) { this.state = state; }

    public long getRequestMs() { return requestMs; }
    public void setRequestMs(long requestMs) { this.requestMs = requestMs; }

    public long getFireMs() { return fireMs; }
    public void setFireMs(long fireMs) { this.fireMs = fireMs; }

    public long getExpirationMs() { return expirationMs; }
    public void setExpirationMs(long expirationMs) { this.expirationMs = expirationMs; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public List<String> getTargetedGroups() { return targetedGroups; }
    public void setTargetedGroups(List<String> targetedGroups) { this.targetedGroups = targetedGroups; }

    public List<AlertSentEntry> getSent() { return sent; }
    public void setSent(List<AlertSentEntry> sent) { this.sent = sent; }
}
