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
@Tag(name="consumption", description = "Consumption report detail")
public class SigfoxApiv2Consumption {

    @Schema(
            description ="Number of uplink messages this day.",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected int frameCount;

    @Schema(
            description ="Number of downlink messages this day.",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected int downlinkFrameCount;

    @Schema(
            description ="Number of uplink roaming messages this day.",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected int roamingFrameCount;

    @Schema(
            description ="Number of downlink roaming messages this day.",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected int roamingDownlinkFrameCount;

    @Tag(name="roamingDetails", description = "Roaming Consumption report detail")
    public static class RoamingDetails {

        @Schema(
                description ="Country of the Operator (3 letters).",
                example = "FRA",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        protected String territory;

        @Schema(
                description ="Name of the operator.",
                example = "SIGFOX_France",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        protected String operator;

        @Schema(
                description ="Number of uplink roaming messages this day for this operator.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        protected int territoryRoamingFrameCount;

        @Schema(
                description ="Number of downlink roaming messages this day for this operator.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        protected int territoryRoamingDownlinkFrameCount;

        // =======================


        public String getTerritory() {
            return territory;
        }

        public void setTerritory(String territory) {
            this.territory = territory;
        }

        public String getOperator() {
            return operator;
        }

        public void setOperator(String operator) {
            this.operator = operator;
        }

        public int getTerritoryRoamingFrameCount() {
            return territoryRoamingFrameCount;
        }

        public void setTerritoryRoamingFrameCount(int territoryRoamingFrameCount) {
            this.territoryRoamingFrameCount = territoryRoamingFrameCount;
        }

        public int getTerritoryRoamingDownlinkFrameCount() {
            return territoryRoamingDownlinkFrameCount;
        }

        public void setTerritoryRoamingDownlinkFrameCount(int territoryRoamingDownlinkFrameCount) {
            this.territoryRoamingDownlinkFrameCount = territoryRoamingDownlinkFrameCount;
        }
    }

    @Schema(
            description ="Roaming details for each of the operator.",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected List<RoamingDetails> roamingDetails;



    // ============================================================
    // Generated Getters & Setters
    // ============================================================


    public int getFrameCount() {
        return frameCount;
    }

    public void setFrameCount(int frameCount) {
        this.frameCount = frameCount;
    }

    public int getDownlinkFrameCount() {
        return downlinkFrameCount;
    }

    public void setDownlinkFrameCount(int downlinkFrameCount) {
        this.downlinkFrameCount = downlinkFrameCount;
    }

    public int getRoamingFrameCount() {
        return roamingFrameCount;
    }

    public void setRoamingFrameCount(int roamingFrameCount) {
        this.roamingFrameCount = roamingFrameCount;
    }

    public int getRoamingDownlinkFrameCount() {
        return roamingDownlinkFrameCount;
    }

    public void setRoamingDownlinkFrameCount(int roamingDownlinkFrameCount) {
        this.roamingDownlinkFrameCount = roamingDownlinkFrameCount;
    }

    public List<RoamingDetails> getRoamingDetails() {
        return roamingDetails;
    }

    public void setRoamingDetails(List<RoamingDetails> roamingDetails) {
        this.roamingDetails = roamingDetails;
    }
}
