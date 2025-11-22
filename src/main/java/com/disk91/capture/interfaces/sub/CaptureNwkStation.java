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
import com.disk91.common.tools.CustomField;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

public class CaptureNwkStation implements CloneableObject<CaptureNwkStation> {

    @Schema(
            description = "Network timestamp when the frame was received in milliseconds since epoch",
            example = "1672531200000",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected long nkwTimestamp;

    @Schema(
            description = "Network timestamp nanoseconds part (when available)",
            example = "123456789",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected long nkwTimeNs;

    @Schema(
            description = "Station identifier",
            example = "station-01",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String stationId;

    @Schema(
            description = "RSSI value",
            example = "-70",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected int rssi;

    @Schema(
            description = "SNR value",
            example = "7.5",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected double snr;

    @Schema(
            description = "Station location when available",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected CaptureCalcLocation stationLocation;

    @Schema(
            description = "Protocol dependant custom parameters as key-value pairs",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected List<CustomField> customParams;

    // ======================================
    @Override
    public CaptureNwkStation clone() {
        CaptureNwkStation o = new CaptureNwkStation();
        o.nkwTimestamp = this.nkwTimestamp;
        o.nkwTimeNs = this.nkwTimeNs;
        o.stationId = this.stationId;
        o.rssi = this.rssi;
        o.snr = this.snr;

        if (this.stationLocation != null) {
            o.stationLocation = this.stationLocation.clone();
        } else {
            o.stationLocation = null;
        }

        o.customParams = new ArrayList<>();
        if (this.customParams != null) {
            for (CustomField f : this.customParams) {
                o.customParams.add(f.clone());
            }
        }

        return o;
    }
    // ======================================


    public long getNkwTimestamp() {
        return nkwTimestamp;
    }

    public void setNkwTimestamp(long nkwTimestamp) {
        this.nkwTimestamp = nkwTimestamp;
    }

    public long getNkwTimeNs() {
        return nkwTimeNs;
    }

    public void setNkwTimeNs(long nkwTimeNs) {
        this.nkwTimeNs = nkwTimeNs;
    }

    public String getStationId() {
        return stationId;
    }

    public void setStationId(String stationId) {
        this.stationId = stationId;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public double getSnr() {
        return snr;
    }

    public void setSnr(double snr) {
        this.snr = snr;
    }

    public CaptureCalcLocation getStationLocation() {
        return stationLocation;
    }

    public void setStationLocation(CaptureCalcLocation stationLocation) {
        this.stationLocation = stationLocation;
    }

    public List<CustomField> getCustomParams() {
        return customParams;
    }

    public void setCustomParams(List<CustomField> customParams) {
        this.customParams = customParams;
    }
}
