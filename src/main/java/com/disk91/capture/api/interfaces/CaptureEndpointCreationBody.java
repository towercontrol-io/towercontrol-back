/*
 * Copyright (c) - Paul Pinault (aka disk91) - 2025.
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
package com.disk91.capture.api.interfaces;

import com.disk91.common.tools.CustomField;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;

@Tag(name = "Capture Endpoint Creation", description = "Request Capture Endpoint Creation")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CaptureEndpointCreationBody {

    @Schema(
            description = "User defined name for the endpoint to create",
            example = "My HeyIoT endpoint",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String name;


    @Schema(
            description = "User defined description for the endpoint to create",
            example = "Where I get my temperature data",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String description;


    @Schema(
            description = "This will force encryption for the payload data at rest",
            example = "false",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected boolean encrypted;


    @Schema(
            description = "Id of the protocol to be used for this endpoint",
            example = "Axdsf7Gh",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String protocolId;

    @Schema(
            description = "Force the wide Open value (override of the default protocol value) :  Not only endpoint owner can report data to this endpoint. Manipulate carefully. Valid JWT still required.",
            example = "false",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected boolean forceWideOpen;


    @Schema(
            description = "List of protocol specific fields to be provided for configuration",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected List<CustomField> customConfig;


    // ==========================================


    // ==========================================


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public void setEncrypted(boolean encrypted) {
        this.encrypted = encrypted;
    }

    public String getProtocolId() {
        return protocolId;
    }

    public void setProtocolId(String protocolId) {
        this.protocolId = protocolId;
    }

    public List<CustomField> getCustomConfig() {
        return customConfig;
    }

    public void setCustomConfig(List<CustomField> customConfig) {
        this.customConfig = customConfig;
    }

    public boolean isForceWideOpen() {
        return forceWideOpen;
    }

    public void setForceWideOpen(boolean forceWideOpen) {
        this.forceWideOpen = forceWideOpen;
    }
}
