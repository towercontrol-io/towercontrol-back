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

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Tag(name="bulkTransfer", description = "Result of bulk transfer")
public class SigfoxApiv2DeviceBulkTransferRequest {

    @Schema(
            description ="The device type where new devices will be created",
            requiredMode = Schema.RequiredMode.REQUIRED

    )
    protected String deviceTypeId;

    @Tag(name="deviceTransferDetails", description = "Device transfered details")
    public static class DeviceTransferDetails {
        @Schema(
                description ="The device’s identifier (hexadecimal format)",
                requiredMode = Schema.RequiredMode.REQUIRED

        )
        protected String id;

        @Schema(
                description ="Whether to keep the device history or not",
                example = "false",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        protected boolean keepHistory;

        @Schema(
                description ="true if the device is activable and can take a token. " +
                        "Not used if the device has already a token and if the " +
                        "transferred is intra-order.",
                example = "false",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        protected boolean activable;




        // ============================================================
        // Generated Getters & Setters
        // ============================================================


        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public boolean isKeepHistory() {
            return keepHistory;
        }

        public void setKeepHistory(boolean keepHistory) {
            this.keepHistory = keepHistory;
        }

        public boolean isActivable() {
            return activable;
        }

        public void setActivable(boolean activable) {
            this.activable = activable;
        }
    }

    @Schema(
            description ="List all the transfered devices",
            requiredMode = Schema.RequiredMode.REQUIRED

    )
    protected List<DeviceTransferDetails> data;


    // ============================================================
    // Custom
    // ============================================================

    public void initOne(String deviceId){
        this.data = new ArrayList<>();
        DeviceTransferDetails d = new DeviceTransferDetails();
        d.setActivable(true);
        d.setKeepHistory(true);
        d.setId(deviceId);
        this.data.add(d);
    }



    // ============================================================
    // Generated Getters & Setters
    // ============================================================


    public String getDeviceTypeId() {
        return deviceTypeId;
    }

    public void setDeviceTypeId(String deviceTypeId) {
        this.deviceTypeId = deviceTypeId;
    }

    public List<DeviceTransferDetails> getData() {
        return data;
    }

    public void setData(List<DeviceTransferDetails> data) {
        this.data = data;
    }
}
