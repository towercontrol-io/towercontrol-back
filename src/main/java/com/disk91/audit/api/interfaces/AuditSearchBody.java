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

@Tag(name = "Audit log search body", description = "Search criteria for audit log queries")
public class AuditSearchBody {

    @Schema(
            description = "Free-text search applied as a case-insensitive partial match on service name, action name and owner simultaneously. Leave empty for no filter.",
            example = "USER",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String search;

    @Schema(
            description = "Start date filter in milliseconds since epoch (inclusive). 0 means no lower bound.",
            example = "1700000000000",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected long startMs;

    @Schema(
            description = "End date filter in milliseconds since epoch (inclusive). 0 means no upper bound.",
            example = "1800000000000",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected long endMs;

    @Schema(
            description = "Page number (0-based). Defaults to 0.",
            example = "0",
            defaultValue = "0",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected int page;

    @Schema(
            description = "Number of elements per page. Defaults to 50, maximum is 200.",
            example = "50",
            defaultValue = "50",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected int pageSize;


    // ==========================
    // Getters & Setters

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    public long getStartMs() {
        return startMs;
    }

    public void setStartMs(long startMs) {
        this.startMs = startMs;
    }

    public long getEndMs() {
        return endMs;
    }

    public void setEndMs(long endMs) {
        this.endMs = endMs;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }
}



