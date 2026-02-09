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
package com.disk91.common.interfaces.llm;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Knowledge base document body", description = "Body for adding a document to a knowledge base")
public class KnowledgeDocumentBody {

    @Schema(
            description = "Unique identifier for the document",
            example = "doc-device-config-001",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String documentId;

    @Schema(
            description = "The content of the document to be indexed",
            example = "This document explains how to configure IoT devices...",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String content;

    @Schema(
            description = "Optional title or name of the document",
            example = "Device Configuration Guide",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String title;

    @Schema(
            description = "Optional category or type of the document",
            example = "user-guide",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String category;

    @Schema(
            description = "Optional source URL or reference",
            example = "https://docs.example.com/device-config",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String sourceUrl;

    // ==========================
    // Getters & Setters

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }
}
