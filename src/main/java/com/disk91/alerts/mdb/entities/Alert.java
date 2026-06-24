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
package com.disk91.alerts.mdb.entities;

import com.disk91.alerts.mdb.entities.sub.AlertMedium;
import com.disk91.alerts.mdb.entities.sub.AlertSentEntry;
import com.disk91.alerts.mdb.entities.sub.AlertState;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * Alert - Persistent entity representing one alert instance.
 * Created by a source module when a business condition is detected; processed asynchronously
 * by the alert processor to resolve targets, select channels, render and deliver notifications.
 */
@Document(collection = "alerts_alerts")
@CompoundIndexes({
        @CompoundIndex(name = "alertId_idx", def = "{'alertId': 'hashed'}"),
        @CompoundIndex(name = "state_idx", def = "{'state': 1}"),
        @CompoundIndex(name = "requestMs_idx", def = "{'requestMs': 1}"),
        // Compound index used by the expiration check on RUNNING alerts
        @CompoundIndex(name = "state_expirationMs_idx", def = "{'state': 1, 'expirationMs': 1}"),
        // Index on sent array user login + date for user history queries
        @CompoundIndex(name = "sent_userLogin_requestMs_idx", def = "{'sent.userLogin': 1, 'requestMs': -1}")
})
public class Alert {

    @Id
    protected String id;

    // Stable business identifier instantiated by the source module (e.g. "alert-temp-high-deviceId123")
    protected String alertId;

    // Source module reference key (e.g. module name or rule identifier)
    protected String alertDefRef;

    // Short functional id of the AlertTemplate used to render and deliver notifications
    protected String alertTemplateId;

    // Associated device Id
    protected String deviceId;

    // Groups identifier used as the broadcast perimeter for user fan-out
    protected List<String> targetedGroups;

    // To be later used, alert parameter (not the static parameter replacement)
    protected List<String> parameters;

    // Current lifecycle state of this alert instance
    protected AlertState state;

    // Timestamp when the triggering event was detected by the source module (ms since epoch)
    protected long requestMs;

    // Timestamp when the alert was actually processed and the notification fired; 0 until fired
    protected long fireMs;

    // Timestamp when the alert auto-closes; 0 means no automatic expiration
    protected long expirationMs;

    // Per-medium delivery status populated by the alert processor
    protected List<AlertSentEntry> sent;

    // 24-character random secret allowing public access to this alert via a direct link
    protected String publicAccessId;

    // possible error string if the error has not been proceeded.
    protected String error;

    // ========================================

    /**
     * Factory - Build a minimal Alert ready to be persisted in PENDING state.
     * @param alertId         - stable business identifier (already instantiated)
     * @param alertDefRef     - source module reference
     * @param alertTemplateId - shortId of the AlertTemplate to use
     * @param groupIds        - target group identifier
     * @param parameters      - positional substitution values
     * @param requestMs       - event detection timestamp
     * @param publicAccessId  - 24-char random secret for public page access
     * @return initialised Alert instance in PENDING state
     */
    public static Alert newAlert(
            String alertId,
            String alertDefRef,
            String alertTemplateId,
            String deviceId,
            List<String> groupIds,
            List<String> parameters,
            long requestMs,
            String publicAccessId
    ) {
        Alert a = new Alert();
        a.setAlertId(alertId);
        a.setAlertDefRef(alertDefRef);
        a.setAlertTemplateId(alertTemplateId);
        a.setDeviceId(deviceId);
        a.setParameters(parameters != null ? parameters : new ArrayList<>());
        a.setState(AlertState.PENDING);
        a.setRequestMs(requestMs);
        a.setFireMs(0);
        a.setExpirationMs(0);
        a.setSent(new ArrayList<>());
        a.setTargetedGroups(groupIds != null ? groupIds : new ArrayList<>());
        a.setPublicAccessId(publicAccessId);
        a.setError("");
        return a;
    }

    // ========================================
    // Sent report
    public void upsertSent(String userId, AlertMedium medium, boolean sent, boolean ack, String error) {
        if ( this.sent == null ) this.sent = new ArrayList<>();
        boolean found = false;
        for ( AlertSentEntry entry : this.sent) {
            if ( userId.equals(entry.getUserLogin()) ) {
                entry.upsertState(medium, sent, ack, error);
                found = true;
            }
        }
        if ( !found ) {
            AlertSentEntry entry = new AlertSentEntry();
            entry.setUserLogin(userId);
            entry.setState(new ArrayList<>());
            entry.upsertState(medium, sent, ack, error);
            this.sent.add(entry);
        }
    }

    // ========================================
    // Getters & Setters

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getAlertId() { return alertId; }
    public void setAlertId(String alertId) { this.alertId = alertId; }

    public String getAlertDefRef() { return alertDefRef; }
    public void setAlertDefRef(String alertDefRef) { this.alertDefRef = alertDefRef; }

    public String getAlertTemplateId() { return alertTemplateId; }
    public void setAlertTemplateId(String alertTemplateId) { this.alertTemplateId = alertTemplateId; }

    public List<String> getTargetedGroups() {
        return targetedGroups;
    }

    public void setTargetedGroups(List<String> targetedGroups) {
        this.targetedGroups = targetedGroups;
    }

    public List<String> getParameters() { return parameters; }
    public void setParameters(List<String> parameters) { this.parameters = parameters; }

    public AlertState getState() { return state; }
    public void setState(AlertState state) { this.state = state; }

    public long getRequestMs() { return requestMs; }
    public void setRequestMs(long requestMs) { this.requestMs = requestMs; }

    public long getFireMs() { return fireMs; }
    public void setFireMs(long fireMs) { this.fireMs = fireMs; }

    public long getExpirationMs() { return expirationMs; }
    public void setExpirationMs(long expirationMs) { this.expirationMs = expirationMs; }

    public List<AlertSentEntry> getSent() { return sent; }
    public void setSent(List<AlertSentEntry> sent) { this.sent = sent; }

    public String getPublicAccessId() { return publicAccessId; }
    public void setPublicAccessId(String publicAccessId) { this.publicAccessId = publicAccessId; }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
}
