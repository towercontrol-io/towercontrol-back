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

import com.disk91.capture.mdb.entities.CaptureEndpoint;
import com.disk91.common.tools.CustomField;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.ArrayList;
import java.util.List;

@Tag(name = "Capture Endpoint", description = "Capture endpoint definition")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CaptureEndpointResponseItf {

    @Schema(
            description = "endpoint unique identifier",
            example = "",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String id;


    @Schema(
            description = "User defined name for the endpoint",
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
            description = "Endpoint short Id (unique)",
            example = "Azh55hjq",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String ref;

    @Schema(
            description = "Endpoint owner Id",
            example = "3C152025461561D51A351...",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String owner;


    @Schema(
            description = "For endpoints with source non restricted to a specific owner, this reduce the security by allowing any valid JWT to post data to this endpoint instead of owner's one",
            example = "false",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected boolean wideOpen;


    @Schema(
            description = "This will force encryption for the payload data at rest",
            example = "false",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected boolean encrypted;

    @Schema(
            description = "Date / time of creation of the endpoint in milliseconds since epoch",
            example = "1672531199000",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected long creationMs;

    @Schema(
            description = "Id of the protocol to be used for this endpoint",
            example = "Axdsf7Gh",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String protocolId;

    @Schema(
            description = "List of protocol specific fields to be provided for configuration",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected List<CustomField> customConfig;

    // stats

    @Schema(
            description = "Stats - total frame received",
            example = "1250",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected long totalFramesReceived;

    @Schema(
            description = "Stats - total frame accepted to pivot",
            example = "1250",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected long totalFramesAcceptedToPivot;

    @Schema(
            description = "Stats - total frame accepted in a driver to pivot",
            example = "1250",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected long totalInDriver;


    @Schema(
            description = "Stats - total frame accepted to process",
            example = "1250",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected long totalFramesAcceptedToProcess;

    @Schema(
            description = "Stats - total frame refused due to user restrictions",
            example = "1250",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected long totalBadOwnerRefused;

    @Schema(
            description = "Stats - total frame refused due payload format issues",
            example = "1250",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected long totalBadPayloadFormat;

    @Schema(
            description = "Stats - total frame refused due device ritgh issues",
            example = "1250",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected long totalBadDeviceRight;

    @Schema(
            description = "Stats - current frame in queue to process",
            example = "1250",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected long totalQueuedToProcess;


    // ==========================================

    public static CaptureEndpointResponseItf fromCaptureEndpoint(CaptureEndpoint ce) {
        CaptureEndpointResponseItf res = new CaptureEndpointResponseItf();
        res.setId(ce.getId());
        res.setName(ce.getName());
        res.setDescription(ce.getDescription());
        res.setRef(ce.getRef());
        res.setOwner(ce.getOwner());
        res.setWideOpen(ce.isWideOpen());
        res.setEncrypted(ce.isEncrypted());
        res.setCreationMs(ce.getCreationMs());
        res.setProtocolId(ce.getProtocolId());
        ArrayList<CustomField> cf = new ArrayList<>();
        if ( ce.getCustomConfig() != null ) { // clone them
            for ( CustomField c : ce.getCustomConfig() ) {
                cf.add( c.clone() );
            }
        }
        res.setCustomConfig(cf);
        res.setTotalFramesReceived(ce.getTotalFramesReceived());
        res.setTotalFramesAcceptedToPivot(ce.getTotalFramesAcceptedToPivot());
        res.setTotalInDriver(ce.getTotalInDriver());
        res.setTotalFramesAcceptedToProcess(ce.getTotalFramesAcceptedToProcess());
        res.setTotalBadOwnerRefused(ce.getTotalBadOwnerRefused());
        res.setTotalBadPayloadFormat(ce.getTotalBadPayloadFormat());
        res.setTotalBadDeviceRight(ce.getTotalBadDeviceRight());
        res.setTotalQueuedToProcess(ce.getTotalQueuedToProcess());
        return res;
    }

    // ==========================================


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public boolean isWideOpen() {
        return wideOpen;
    }

    public void setWideOpen(boolean wideOpen) {
        this.wideOpen = wideOpen;
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public void setEncrypted(boolean encrypted) {
        this.encrypted = encrypted;
    }

    public long getCreationMs() {
        return creationMs;
    }

    public void setCreationMs(long creationMs) {
        this.creationMs = creationMs;
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

    public long getTotalFramesReceived() {
        return totalFramesReceived;
    }

    public void setTotalFramesReceived(long totalFramesReceived) {
        this.totalFramesReceived = totalFramesReceived;
    }

    public long getTotalFramesAcceptedToPivot() {
        return totalFramesAcceptedToPivot;
    }

    public void setTotalFramesAcceptedToPivot(long totalFramesAcceptedToPivot) {
        this.totalFramesAcceptedToPivot = totalFramesAcceptedToPivot;
    }

    public long getTotalInDriver() {
        return totalInDriver;
    }

    public void setTotalInDriver(long totalInDriver) {
        this.totalInDriver = totalInDriver;
    }

    public long getTotalFramesAcceptedToProcess() {
        return totalFramesAcceptedToProcess;
    }

    public void setTotalFramesAcceptedToProcess(long totalFramesAcceptedToProcess) {
        this.totalFramesAcceptedToProcess = totalFramesAcceptedToProcess;
    }

    public long getTotalBadOwnerRefused() {
        return totalBadOwnerRefused;
    }

    public void setTotalBadOwnerRefused(long totalBadOwnerRefused) {
        this.totalBadOwnerRefused = totalBadOwnerRefused;
    }

    public long getTotalBadPayloadFormat() {
        return totalBadPayloadFormat;
    }

    public void setTotalBadPayloadFormat(long totalBadPayloadFormat) {
        this.totalBadPayloadFormat = totalBadPayloadFormat;
    }

    public long getTotalBadDeviceRight() {
        return totalBadDeviceRight;
    }

    public void setTotalBadDeviceRight(long totalBadDeviceRight) {
        this.totalBadDeviceRight = totalBadDeviceRight;
    }

    public long getTotalQueuedToProcess() {
        return totalQueuedToProcess;
    }

    public void setTotalQueuedToProcess(long totalQueuedToProcess) {
        this.totalQueuedToProcess = totalQueuedToProcess;
    }
}
