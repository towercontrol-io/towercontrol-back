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
@Tag(name="deviceBulk", description = "Bulk Device registration")
public class SigfoxApiv2DeviceBulk {

    @Schema(
            description ="The device type where new devices will be created",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String deviceTypeId;

    @Schema(
            description ="The devices names prefix",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String prefix;

    @Schema(
            description ="The product’s certificate name if any (mandatory if not a prototype)",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String productCertificate;

    @Schema(
            description ="If the devices are a prototype or not",
            example = "false",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected boolean prototype;

    @Schema(
            description ="True if the devices are activable and can take a token",
            example = "true",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected boolean activable;

    @Schema(
            description ="List of devices to be registered in Sigfox backend",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected List<SigfoxApiv2DeviceBulkCreation> devices;

    // ============================================================
    // Generated Getters & Setters
    // ============================================================


    public String getDeviceTypeId() {
        return deviceTypeId;
    }

    public void setDeviceTypeId(String deviceTypeId) {
        this.deviceTypeId = deviceTypeId;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getProductCertificate() {
        return productCertificate;
    }

    public void setProductCertificate(String productCertificate) {
        this.productCertificate = productCertificate;
    }

    public boolean isPrototype() {
        return prototype;
    }

    public void setPrototype(boolean prototype) {
        this.prototype = prototype;
    }

    public boolean isActivable() {
        return activable;
    }

    public void setActivable(boolean activable) {
        this.activable = activable;
    }

    public List<SigfoxApiv2DeviceBulkCreation> getDevices() {
        return devices;
    }

    public void setDevices(List<SigfoxApiv2DeviceBulkCreation> devices) {
        this.devices = devices;
    }
}
