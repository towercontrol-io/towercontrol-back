/*
 * Copyright (c) 2018.
 *
 *  This is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  this software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  -------------------------------------------------------------------------------
 *  Author : Paul Pinault aka disk91
 *  See https://www.disk91.com
 *
 *  Commercial license of this software can be obtained contacting disk91.com or ingeniousthings.fr
 *  -------------------------------------------------------------------------------
 *
 */

/**
 * -------------------------------------------------------------------------------
 * This file is part of IngeniousThings Sigfox-Api.
 *
 * IngeniousThings Sigfox-Api is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * IngeniousThings Sigfox-Api is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 * -------------------------------------------------------------------------------
 * Author : Paul Pinault aka disk91
 * See https://www.disk91.com
 * ----
 * More information about IngeniousThings : https://www.ingeniousthings.fr
 * ----
 * Commercial license of this software can be obtained contacting ingeniousthings
 * -------------------------------------------------------------------------------
 */
package com.disk91.capture.drivers.standard.sigfox.apiv2.models;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Tag(
        name = "computedLocation",
        description = "Contains the estimated position of the device within a " +
        "circle based on the GPS data or the Sigfox Geolocation service"
)
public class SigfoxApiv2ComputedLocation {

    @Schema(
            description ="The device’s estimated latitude",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected double lat;

    @Schema(
            description ="The device’s estimated longitude",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected double lng;

    @Schema(
            description ="The radius of the precision circle (meters)",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected long radius;

    @Schema(
            description ="Define from which source the geolocation has been computed <br/>" +
                    "<ul>" +
                    "<li>0 -> Location computed by legacy mode using RSSI and position of station</li>" +
                    "<li>1 -> Location computed using the GPS data inside the payload</li>" +
                    "<li>2 -> Location computed by Atlas Network</li>" +
                    "<li>3 -> Location computed by Atlas POI</li>" +
                    "<li>4 -> Location computed by Atlas HD<li>" +
                    "<li>5 -> Location computed using private DB location<li>" +
                    "<li>6 -> Location computed using WiFi location<li>" +
                    "<li>7 -> Location computed using Proximity location<li>" +
                    "</ul>",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected int sourceCode;


    @Schema(
            description ="The place ids computed by the Sigfox Geolocation service",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected List<String> placeIds;


    // ============================================================
    // Generated Getters & Setters
    // ============================================================


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

    public long getRadius() {
        return radius;
    }

    public void setRadius(long radius) {
        this.radius = radius;
    }

    public int getSourceCode() {
        return sourceCode;
    }

    public void setSourceCode(int sourceCode) {
        this.sourceCode = sourceCode;
    }

    public List<String> getPlaceIds() {
        return placeIds;
    }

    public void setPlaceIds(List<String> placeIds) {
        this.placeIds = placeIds;
    }
}
