/*
 * Copyright (c) - Paul Pinault (aka disk91) - 2019.
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

package com.disk91.capture.drivers.standard.sigfox.sub;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SigfoxAdvancedMessLocation {

    @Schema(
            description = "Latitude",
            example = "45.790955300746326",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected double lat;

    @Schema(
            description = "Longitude",
            example = "3.0807459140508637",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected double lng;

    @Schema(
            description ="Radius",
            example = "6978",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected int radius;

    @Schema(
            description ="Source:<br/>" +
                    "<ul>" +
                    "<li>1 : GPS extracted from payload</li>" +
                    "<li>2 : Network computed location</li>" +
                    "<li>6 : WiFi computed location</li>" +
                    "</ul>",
            example = "2",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected int source;

    @Schema(
            description ="Status ??",
            example = "1",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected int status;

    // ============================================================================
    // Generated Getter & Setters
    // ============================================================================

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public int getRadius() {
        return radius;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public int getSource() {
        return source;
    }

    public void setSource(int source) {
        this.source = source;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
