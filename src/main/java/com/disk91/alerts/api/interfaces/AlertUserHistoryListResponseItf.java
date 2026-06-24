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

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

/**
 * AlertUserHistoryListResponseItf - Paginated list of alert history entries for the authenticated user.
 */
@Tag(name = "Alert user history list", description = "Paginated list of alerts from the authenticated user's history")
public class AlertUserHistoryListResponseItf {

    @Schema(description = "Total number of alerts matching the query",
            example = "142", requiredMode = Schema.RequiredMode.REQUIRED)
    protected long total;

    @Schema(description = "Current page index (0-based)",
            example = "0", requiredMode = Schema.RequiredMode.REQUIRED)
    protected int page;

    @Schema(description = "Number of records per page (1-100)",
            example = "20", requiredMode = Schema.RequiredMode.REQUIRED)
    protected int size;

    @ArraySchema(schema = @Schema(implementation = AlertUserHistoryResponseItf.class))
    @Schema(description = "Alerts on this page", requiredMode = Schema.RequiredMode.REQUIRED)
    protected List<AlertUserHistoryResponseItf> alerts;

    // ==========================
    // Getters & Setters

    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }

    public List<AlertUserHistoryResponseItf> getAlerts() { return alerts; }
    public void setAlerts(List<AlertUserHistoryResponseItf> alerts) { this.alerts = alerts; }
}
