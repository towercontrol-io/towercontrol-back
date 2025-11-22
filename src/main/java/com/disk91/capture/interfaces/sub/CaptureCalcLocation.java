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

public class CaptureCalcLocation implements CloneableObject<CaptureCalcLocation>  {

    @Schema(
            description = "Latitude in decimal degrees",
            example = "48.8566",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected double latitude;

    @Schema(
            description = "Longitude in decimal degrees",
            example = "2.3522",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected double longitude;

    @Schema(
            description = "Altitude in meters",
            example = "35.0",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected double altitude;

    @Schema(
            description = "Accuracy in meters",
            example = "5.5",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected double accuracy;

    @Schema(
            description = "Hexagon ID",
            example = "8e1a2b",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String hexagonId;

    // ======================================
    @Override
    public CaptureCalcLocation clone() {
        CaptureCalcLocation o = new CaptureCalcLocation();
        o.latitude = this.latitude;
        o.longitude = this.longitude;
        o.altitude = this.altitude;
        o.accuracy = this.accuracy;
        o.hexagonId = this.hexagonId;
        return o;
    }

    // ======================================


    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(double accuracy) {
        this.accuracy = accuracy;
    }

    public String getHexagonId() {
        return hexagonId;
    }

    public void setHexagonId(String hexagonId) {
        this.hexagonId = hexagonId;
    }
}
