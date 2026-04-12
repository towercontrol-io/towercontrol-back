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
package com.disk91.files.api.interfaces;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

/**
 * Paginated response returned by the admin file search endpoint.
 * Wraps a page of FileUploadResponseItf entries together with pagination metadata.
 */
@Tag(name = "File admin list response", description = "Paginated list of files returned by the admin search endpoint")
public class FileAdminListResponseItf {

    @Schema(
            description = "Total number of files matching the search criteria",
            example = "142",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected long total;

    @Schema(
            description = "Current page index (0-based)",
            example = "0",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected int page;

    @Schema(
            description = "Number of records per page (1-250)",
            example = "50",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected int size;

    @ArraySchema(schema = @Schema(implementation = FileUploadResponseItf.class))
    @Schema(
            description = "Files on this page",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected List<FileUploadResponseItf> files;

    // ==========================
    // Getters & Setters

    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }

    public List<FileUploadResponseItf> getFiles() { return files; }
    public void setFiles(List<FileUploadResponseItf> files) { this.files = files; }
}

