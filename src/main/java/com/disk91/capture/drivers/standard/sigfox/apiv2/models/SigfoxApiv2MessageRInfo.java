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
@Tag(name="rInfo", description = "Data Message Reception information")
public class SigfoxApiv2MessageRInfo {

    @Schema(
            description ="Base station to send downlink message",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected SigfoxApiv2BaseStationMinimal baseStation;

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
            description ="Number of repetitions sent by the base station",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected int rep;

    @Schema(
            description ="List of callback status for this reception",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected List<SigfoxApiv2CallbackExecutionStatus> cbStatus;

    // ============================================================
    // Generated Getters & Setters
    // ============================================================


    public SigfoxApiv2BaseStationMinimal getBaseStation() {
        return baseStation;
    }

    public void setBaseStation(SigfoxApiv2BaseStationMinimal baseStation) {
        this.baseStation = baseStation;
    }

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

    public int getRep() {
        return rep;
    }

    public void setRep(int rep) {
        this.rep = rep;
    }

    public List<SigfoxApiv2CallbackExecutionStatus> getCbStatus() {
        return cbStatus;
    }

    public void setCbStatus(List<SigfoxApiv2CallbackExecutionStatus> cbStatus) {
        this.cbStatus = cbStatus;
    }
}
