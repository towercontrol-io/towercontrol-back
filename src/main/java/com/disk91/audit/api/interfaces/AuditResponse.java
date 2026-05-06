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

@Tag(name = "Audit Log", description = "One of the Audit log trace")
public class AuditResponse {

    @Schema(
            description = "Timestamp of the audit log, ms since EPOC",
            example = "178667672",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected long actionMs;

    @Schema(
            description = "Service Name",
            example = "USER",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String service;

    @Schema(
            description = "Action Name",
            example = "USER_CREATE",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String action;

    @Schema(
            description = "Owner",
            example = "system",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String owner;


    @Schema(
            description = "Log String",
            example = "A new user xxxx has been created",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String logStr;

    @Schema(
            description = "When true chained signature verification is valid (not yet implemented, always true)",
            example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected boolean linkChain;


    // ==========================
    // Init

    // ==========================
    // Getters & Setters


    public long getActionMs() {
        return actionMs;
    }

    public void setActionMs(long actionMs) {
        this.actionMs = actionMs;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getLogStr() {
        return logStr;
    }

    public void setLogStr(String logStr) {
        this.logStr = logStr;
    }

    public boolean isLinkChain() {
        return linkChain;
    }

    public void setLinkChain(boolean linkChain) {
        this.linkChain = linkChain;
    }
}
