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
package com.disk91.alerts.pdb.entities;

import com.disk91.alerts.mdb.entities.sub.AlertCriticality;
import jakarta.persistence.*;

import java.util.UUID;

/**
 * AlertPopup - Persistent popup notification record stored in PostgreSQL.
 * One entry per user per alert delivery via the POPUP medium.
 * Entries are purged after alerts.max.history.ms milliseconds.
 */
@Entity
@Table(
        name = "alerts_popups",
        indexes = {
                @Index(name = "idx_popup_user_login", columnList = "user_login"),
                @Index(name = "idx_popup_time_ms", columnList = "time_ms")
        }
)
public class AlertPopup {

    @Id
    @Column(name = "popup_id", nullable = false, unique = true)
    protected UUID id;

    // Login of the user to whom this popup is addressed
    @Column(name = "user_login", nullable = false)
    protected String userLogin;

    // Reference to the source alert
    @Column(name = "alert_id")
    protected String alertId;

    // Rendered and translated message ready for display
    @Column(name = "message", columnDefinition = "TEXT")
    protected String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "criticality", length = 50)
    protected AlertCriticality criticality;

    // Alert emission timestamp
    @Column(name = "time_ms", nullable = false)
    protected long timeMs;

    // 0 = not yet viewed; populated with current ms when the user marks popups as viewed
    @Column(name = "viewed_ms", nullable = false)
    protected long viewedMs;

    // ==========================
    // Factory

    /**
     * Build a new unread AlertPopup ready for persistence.
     * @param userLogin - targeted user login
     * @param alertId - source alert identifier
     * @param message - rendered localized message
     * @param criticality - alert criticality level
     * @param timeMs - alert emission timestamp
     * @return a transient AlertPopup instance
     */
    public static AlertPopup create(
            String userLogin,
            String alertId,
            String message,
            AlertCriticality criticality,
            long timeMs
    ) {
        AlertPopup p = new AlertPopup();
        p.id = UUID.randomUUID();
        p.userLogin = userLogin;
        p.alertId = alertId;
        p.message = message;
        p.criticality = criticality;
        p.timeMs = timeMs;
        p.viewedMs = 0L;
        return p;
    }

    // ==========================
    // Getters & Setters

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getUserLogin() { return userLogin; }
    public void setUserLogin(String userLogin) { this.userLogin = userLogin; }

    public String getAlertId() { return alertId; }
    public void setAlertId(String alertId) { this.alertId = alertId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public AlertCriticality getCriticality() { return criticality; }
    public void setCriticality(AlertCriticality criticality) { this.criticality = criticality; }

    public long getTimeMs() { return timeMs; }
    public void setTimeMs(long timeMs) { this.timeMs = timeMs; }

    public long getViewedMs() { return viewedMs; }
    public void setViewedMs(long viewedMs) { this.viewedMs = viewedMs; }
}
