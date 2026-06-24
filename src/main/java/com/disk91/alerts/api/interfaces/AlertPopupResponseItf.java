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

import com.disk91.alerts.mdb.entities.sub.AlertCriticality;
import com.disk91.alerts.pdb.entities.AlertPopup;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * AlertPopupResponseItf - API response for a single popup notification entry.
 */
@Tag(name = "Alert popup entry", description = "A single popup notification delivered to a user")
public class AlertPopupResponseItf {

    @Schema(description = "Source alert identifier", example = "alert-temperature-high-123456",
            requiredMode = Schema.RequiredMode.REQUIRED)
    protected String alertId;

    @Schema(description = "Rendered localized message ready for display",
            example = "Temperature exceeded threshold on sensor A3",
            requiredMode = Schema.RequiredMode.REQUIRED)
    protected String message;

    @Schema(description = "Alert criticality level", example = "WARNING",
            requiredMode = Schema.RequiredMode.REQUIRED)
    protected AlertCriticality criticality;

    @Schema(description = "Alert emission timestamp (ms since epoch)", example = "1749600000000",
            requiredMode = Schema.RequiredMode.REQUIRED)
    protected long timeMs;

    // ==========================
    // Builder

    /**
     * Populate this response from an AlertPopup entity.
     * @param p - source entity
     */
    public void buildFrom(AlertPopup p) {
        this.alertId = p.getAlertId();
        this.message = p.getMessage();
        this.criticality = p.getCriticality();
        this.timeMs = p.getTimeMs();
    }

    // ==========================
    // Getters & Setters

    public String getAlertId() { return alertId; }
    public void setAlertId(String alertId) { this.alertId = alertId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public AlertCriticality getCriticality() { return criticality; }
    public void setCriticality(AlertCriticality criticality) { this.criticality = criticality; }

    public long getTimeMs() { return timeMs; }
    public void setTimeMs(long timeMs) { this.timeMs = timeMs; }

}
