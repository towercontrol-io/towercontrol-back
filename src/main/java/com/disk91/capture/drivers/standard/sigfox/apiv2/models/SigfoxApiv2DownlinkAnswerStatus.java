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

@JsonIgnoreProperties(ignoreUnknown = true)
@Tag(name="downlinkAnswerStatus", description = "Downlink Answer Status")
public class SigfoxApiv2DownlinkAnswerStatus {


    @Schema(
            description ="Base station to send downlink message",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected SigfoxApiv2BaseStationMinimal baseStation;

    @Schema(
            description ="Planned downlink power as it was computed by the backend",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected double plannedPower;

    @Schema(
            description ="Response content, hex encoded",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String data;

    @Schema(
            description ="Name of the first operator which received the message as roaming",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String operator;

    @Schema(
            description ="Country of the operator",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String country;


    // ============================================================
    // Generated Getters & Setters
    // ============================================================


    public SigfoxApiv2BaseStationMinimal getBaseStation() {
        return baseStation;
    }

    public void setBaseStation(SigfoxApiv2BaseStationMinimal baseStation) {
        this.baseStation = baseStation;
    }

    public double getPlannedPower() {
        return plannedPower;
    }

    public void setPlannedPower(double plannedPower) {
        this.plannedPower = plannedPower;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }
}
