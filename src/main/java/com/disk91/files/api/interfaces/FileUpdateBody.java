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

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Request body for updating file metadata (description, access type and/or short name).
 */
@Tag(name = "File metadata update body", description = "Fields that can be updated on an existing file")
public class FileUpdateBody {

    @Schema(
            description = "New access control type: PUBLIC, CONNECTED or PRIVATE",
            example = "PRIVATE",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String accessType;

    @Schema(
            description = "New human-readable description; pass null or empty to clear it",
            example = "Updated description",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String description;

    @Schema(
            description = "Short name management: true = generate a short name if none exists, " +
                    "false = remove the existing short name, null = leave unchanged",
            example = "true",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected boolean withShortName;

    @Schema(
            description = "Access key management: true = generate (or regenerate) a 16-character access key, " +
                    "false = remove the existing access key, null = leave unchanged",
            example = "true",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected boolean withAccessKey;

    // ==========================
    // Getters & Setters

    public String getAccessType() { return accessType; }
    public void setAccessType(String accessType) { this.accessType = accessType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean getWithShortName() { return withShortName; }
    public void setWithShortName(boolean withShortName) { this.withShortName = withShortName; }

    public boolean getWithAccessKey() { return withAccessKey; }
    public void setWithAccessKey(boolean withAccessKey) { this.withAccessKey = withAccessKey; }
}

