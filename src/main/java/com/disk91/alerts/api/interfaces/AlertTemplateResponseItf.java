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

import com.disk91.alerts.mdb.entities.AlertTemplate;
import com.disk91.alerts.mdb.entities.sub.AlertCriticality;
import com.disk91.alerts.mdb.entities.sub.AlertLocaleMessage;
import com.disk91.alerts.mdb.entities.sub.AlertMedium;
import com.disk91.alerts.mdb.entities.sub.AlertParameterEntry;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.ArrayList;

/**
 * AlertTemplateResponseItf - API response representation of a single AlertTemplate.
 * Populated via buildFrom(AlertTemplate).
 */
@Tag(name = "Alert Template Response", description = "Alert template details returned by the API")
public class AlertTemplateResponseItf {

    @Schema(description = "Short functional identifier of the template", example = "ABCDEF",
            requiredMode = Schema.RequiredMode.REQUIRED)
    protected String shortId;

    @Schema(description = "Template name", example = "High temperature alert",
            requiredMode = Schema.RequiredMode.REQUIRED)
    protected String name;

    @Schema(description = "Template description",
            example = "Fired when temperature exceeds threshold",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    protected String description;

    @Schema(description = "Login of the template owner", example = "a3f2b1c9d4e5f6a7",
            requiredMode = Schema.RequiredMode.REQUIRED)
    protected String owner;

    @Schema(description = "True when the template is globally visible to all connected users",
            example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
    protected boolean global;

    @Schema(description = "Ordered list of dynamic parameters",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    protected ArrayList<AlertParameterEntry> parameters;

    @Schema(description = "Per-locale open messages", requiredMode = Schema.RequiredMode.REQUIRED)
    protected ArrayList<AlertLocaleMessage> open;

    @Schema(description = "Per-locale close messages", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    protected ArrayList<AlertLocaleMessage> close;

    @Schema(description = "Firing behavior name", example = "FIRE_FORGET",
            requiredMode = Schema.RequiredMode.REQUIRED)
    protected String behavior;

    @Schema(description = "Preferred delivery mediums in priority order",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    protected ArrayList<AlertMedium> preferred;

    @Schema(description = "Auto-close duration in milliseconds; 0 means no expiration",
            example = "900000", requiredMode = Schema.RequiredMode.REQUIRED)
    protected long durationMs;

    @Schema(description = "Criticality Level",
            example = "INFO", requiredMode = Schema.RequiredMode.REQUIRED)
    protected AlertCriticality criticality;

    @Schema(description = "Number of alert repeat before retry message sent",
            example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
    protected int retryTimes;

    // Duration for the retry period in Ms, after this duration a reminder will be sent on a different medium (mode FIRE_TO_END) (0 disable)
    @Schema(description = "Alert duration until a retry message is sent",
            example = "600000", requiredMode = Schema.RequiredMode.REQUIRED)
    protected long retryMs;


    // ==========================
    // Builder

    /**
     * Populate this response from an AlertTemplate entity.
     * @param t - source entity
     */
    public void buildFrom(AlertTemplate t) {
        this.shortId = t.getShortId();
        this.name = t.getName();
        this.description = t.getDescription();
        this.owner = t.getOwner();
        this.global = t.isGlobal();
        this.parameters = t.getParameters();
        this.open = t.getOpen();
        this.close = t.getClose();
        this.behavior = t.getBehavior() != null ? t.getBehavior().name() : null;
        this.preferred = t.getPreferred();
        this.durationMs = t.getDurationMs();
        this.criticality = t.getCriticality();
        this.retryTimes = t.getRetryTimes();
        this.retryMs = t.getRetryMs();
    }

    // ==========================
    // Getters & Setters

    public String getShortId() { return shortId; }
    public void setShortId(String shortId) { this.shortId = shortId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }

    public boolean isGlobal() { return global; }
    public void setGlobal(boolean global) { this.global = global; }

    public ArrayList<AlertParameterEntry> getParameters() { return parameters; }
    public void setParameters(ArrayList<AlertParameterEntry> parameters) { this.parameters = parameters; }

    public ArrayList<AlertLocaleMessage> getOpen() { return open; }
    public void setOpen(ArrayList<AlertLocaleMessage> open) { this.open = open; }

    public ArrayList<AlertLocaleMessage> getClose() { return close; }
    public void setClose(ArrayList<AlertLocaleMessage> close) { this.close = close; }

    public String getBehavior() { return behavior; }
    public void setBehavior(String behavior) { this.behavior = behavior; }

    public ArrayList<AlertMedium> getPreferred() { return preferred; }
    public void setPreferred(ArrayList<AlertMedium> preferred) { this.preferred = preferred; }

    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }

    public AlertCriticality getCriticality() {
        return criticality;
    }

    public void setCriticality(AlertCriticality criticality) {
        this.criticality = criticality;
    }

    public int getRetryTimes() {
        return retryTimes;
    }

    public void setRetryTimes(int retryTimes) {
        this.retryTimes = retryTimes;
    }

    public long getRetryMs() {
        return retryMs;
    }

    public void setRetryMs(long retryMs) {
        this.retryMs = retryMs;
    }
}

