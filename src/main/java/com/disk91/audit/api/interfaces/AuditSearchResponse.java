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
package com.disk91.audit.api.interfaces;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "Audit log search response", description = "Result of an audit log search query")
public class AuditSearchResponse {

    @Schema(
            description = "Total number of entries matching the search criteria",
            example = "150",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected long total;

    @Schema(
            description = "Number of elements per page returned",
            example = "50",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected int pageSize;

    @Schema(
            description = "Total number of pages available for the current search criteria",
            example = "3",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected int totalPages;

    @Schema(
            description = "Status of the search. 'ok' on success, 'audit-log-non-database' when no database backend is configured.",
            example = "ok",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String status;

    @Schema(
            description = "List of audit log entries for the requested page, ordered from most recent to oldest",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected List<AuditResponse> logs;


    // ==========================
    // Getters & Setters

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<AuditResponse> getLogs() {
        return logs;
    }

    public void setLogs(List<AuditResponse> logs) {
        this.logs = logs;
    }
}

