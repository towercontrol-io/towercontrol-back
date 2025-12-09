/*
 * Copyright (c) - Paul Pinault (aka disk91) - 2024.
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
package com.disk91.capture.interfaces.sub;

import com.disk91.common.tools.CloneableObject;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;

public class CaptureMetaData implements CloneableObject<CaptureMetaData> {

    @Schema(
            description = "Network frame UUID",
            example = "550e8400-e29b-41d4-a716-446655440000",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String nwkUuid;

    @Schema(
            description = "Original data timestamp in milliseconds since epoch",
            example = "1672531200000",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected long nwkTimestamp;

    @Schema(
            description = "Original data timestamp nanoseconds part (when available)",
            example = "123456789",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected long nwkTimeNs;

    @Schema(
            description = "Device Id as identified on the network",
            example = "nwk-dev-001",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String nwkDeviceId;

    @Schema(
            description = "Device Id as identified in the platform",
            example = "",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String deviceId;

    @Schema(
            description = "Session counter when available",
            example = "42",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected int sessionCounter;

    @Schema(
            description = "Uplink Frame counter when available",
            example = "42",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected int frameCounterUp;

    @Schema(
            description = "Downlink Frame counter when available",
            example = "42",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected int frameCounterDwn;


    @Schema(
            description = "Frame port when available",
            example = "1",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected int framePort;

    @Schema(
            description = "Whether the frame has confirm request flag",
            example = "true",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected boolean confirmReq;

    @Schema(
            description = "Whether the frame was confirmed",
            example = "false",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected boolean confirmed;

    @Schema(
            description = "When the frame requests a downlink",
            example = "false",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected boolean downlinkReq;

    @Schema(
            description = "When the frame corresponds to a downlink response",
            example = "false",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected boolean downlinkResp;

    @Schema(
            description = "Radio metadata when available",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected CaptureRadioMetadata radioMetadata;

    @Schema(
            description = "Calculated location when available",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected CaptureCalcLocation calculatedLocation;

    // ======================================
    public static CaptureMetaData init() {
        CaptureMetaData cmd = new CaptureMetaData();
        cmd.radioMetadata = new CaptureRadioMetadata();
        cmd.radioMetadata.customParams = new ArrayList<>();
        cmd.calculatedLocation = new CaptureCalcLocation();
        return cmd;
    }

    // ======================================
    @Override
    public CaptureMetaData clone() {
        CaptureMetaData o = new CaptureMetaData();
        o.nwkUuid = this.nwkUuid;
        o.nwkTimestamp = this.nwkTimestamp;
        o.nwkTimeNs = this.nwkTimeNs;
        o.nwkDeviceId = this.nwkDeviceId;
        o.deviceId = this.deviceId;
        o.sessionCounter = this.sessionCounter;
        o.frameCounterUp = this.frameCounterUp;
        o.frameCounterDwn = this.frameCounterDwn;
        o.framePort = this.framePort;
        o.confirmReq = this.confirmReq;
        o.confirmed = this.confirmed;
        o.downlinkReq = this.downlinkReq;
        o.downlinkResp = this.downlinkResp;
        if (this.radioMetadata != null) {
            o.radioMetadata = this.radioMetadata.clone();
        } else {
            o.radioMetadata = null;
        }
        if (this.calculatedLocation != null) {
            o.calculatedLocation = this.calculatedLocation.clone();
        } else {
            o.calculatedLocation = null;
        }
        return o;
    }

    // ======================================

    public String getNwkUuid() {
        return nwkUuid;
    }

    public void setNwkUuid(String nwkUuid) {
        this.nwkUuid = nwkUuid;
    }

    public long getNwkTimestamp() {
        return nwkTimestamp;
    }

    public void setNwkTimestamp(long nwkTimestamp) {
        this.nwkTimestamp = nwkTimestamp;
    }

    public long getNwkTimeNs() {
        return nwkTimeNs;
    }

    public void setNwkTimeNs(long nwkTimeNs) {
        this.nwkTimeNs = nwkTimeNs;
    }

    public String getNwkDeviceId() {
        return nwkDeviceId;
    }

    public void setNwkDeviceId(String nwkDeviceId) {
        this.nwkDeviceId = nwkDeviceId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public int getSessionCounter() {
        return sessionCounter;
    }

    public void setSessionCounter(int sessionCounter) {
        this.sessionCounter = sessionCounter;
    }

    public int getFrameCounterUp() {
        return frameCounterUp;
    }

    public void setFrameCounterUp(int frameCounterUp) {
        this.frameCounterUp = frameCounterUp;
    }

    public int getFramePort() {
        return framePort;
    }

    public void setFramePort(int framePort) {
        this.framePort = framePort;
    }

    public boolean isConfirmReq() {
        return confirmReq;
    }

    public void setConfirmReq(boolean confirmReq) {
        this.confirmReq = confirmReq;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }

    public boolean isDownlinkReq() {
        return downlinkReq;
    }

    public void setDownlinkReq(boolean downlinkReq) {
        this.downlinkReq = downlinkReq;
    }

    public boolean isDownlinkResp() {
        return downlinkResp;
    }

    public void setDownlinkResp(boolean downlinkResp) {
        this.downlinkResp = downlinkResp;
    }

    public CaptureRadioMetadata getRadioMetadata() {
        return radioMetadata;
    }

    public void setRadioMetadata(CaptureRadioMetadata radioMetadata) {
        this.radioMetadata = radioMetadata;
    }

    public CaptureCalcLocation getCalculatedLocation() {
        return calculatedLocation;
    }

    public void setCalculatedLocation(CaptureCalcLocation calculatedLocation) {
        this.calculatedLocation = calculatedLocation;
    }

    public int getFrameCounterDwn() {
        return frameCounterDwn;
    }

    public void setFrameCounterDwn(int frameCounterDwn) {
        this.frameCounterDwn = frameCounterDwn;
    }
}
