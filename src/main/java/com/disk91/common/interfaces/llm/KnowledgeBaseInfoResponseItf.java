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

@Tag(name = "Knowledge base info response", description = "Information about a knowledge base")
public class KnowledgeBaseInfoResponseItf {

    @Schema(
            description = "The knowledge base identifier",
            example = "support-docs",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String knowledgeBaseId;

    @Schema(
            description = "Number of documents in the knowledge base",
            example = "150",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected long documentCount;

    @Schema(
            description = "Last synchronization timestamp in milliseconds",
            example = "1707494400000",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected long lastSyncMs;

    @Schema(
            description = "Knowledge base description",
            example = "Support documentation for IoT devices",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String description;

    // ==========================
    // Getters & Setters

    public String getKnowledgeBaseId() {
        return knowledgeBaseId;
    }

    public void setKnowledgeBaseId(String knowledgeBaseId) {
        this.knowledgeBaseId = knowledgeBaseId;
    }

    public long getDocumentCount() {
        return documentCount;
    }

    public void setDocumentCount(long documentCount) {
        this.documentCount = documentCount;
    }

    public long getLastSyncMs() {
        return lastSyncMs;
    }

    public void setLastSyncMs(long lastSyncMs) {
        this.lastSyncMs = lastSyncMs;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
