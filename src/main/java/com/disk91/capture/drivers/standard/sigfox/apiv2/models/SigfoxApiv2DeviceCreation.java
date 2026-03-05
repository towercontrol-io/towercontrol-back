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
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@JsonIgnoreProperties(ignoreUnknown = true)
@Tag(name="deviceCreation", description = "Defines the device’s common properties for reading or creation (not update)")
public class SigfoxApiv2DeviceCreation {

    @Schema(
            description ="The device’s identifier (hexadecimal format)",
            requiredMode = Schema.RequiredMode.REQUIRED

    )
    protected String id;

    @Schema(
            description ="The device’s name",
            requiredMode = Schema.RequiredMode.REQUIRED

    )
    protected String name;

    @Schema(
            description ="Defines a device type entity",
            requiredMode = Schema.RequiredMode.REQUIRED

    )
    protected String deviceTypeId;

    @Schema(
            description ="The device’s PAC (Porting Access Code)",
            requiredMode = Schema.RequiredMode.REQUIRED

    )
    protected String pac;

    @Schema(
            description ="The device’s provided latitude",
            example = "0",
            requiredMode = Schema.RequiredMode.REQUIRED

    )
    protected double lat;

    @Schema(
            description ="The device’s provided longitude",
            example = "0",
            requiredMode = Schema.RequiredMode.REQUIRED

    )
    protected double lng;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(
            description ="Product certificate",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected SigfoxApiv2ProductCertificate productCertificate;

    @Schema(
            description ="If the device is a prototype or not",
            example = "false",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected boolean prototype;

    @Schema(
            description ="Allow token renewal ?",
            example = "true",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected boolean automaticRenewal;


    @Schema(
            description ="True if the device is activable and can take a token",
            example = "true",
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDeviceTypeId() {
        return deviceTypeId;
    }

    public void setDeviceTypeId(String deviceTypeId) {
        this.deviceTypeId = deviceTypeId;
    }

    public String getPac() {
        return pac;
    }

    public void setPac(String pac) {
        this.pac = pac;
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

    public SigfoxApiv2ProductCertificate getProductCertificate() {
        return productCertificate;
    }

    public void setProductCertificate(SigfoxApiv2ProductCertificate productCertificate) {
        this.productCertificate = productCertificate;
    }

    public boolean isPrototype() {
        return prototype;
    }

    public void setPrototype(boolean prototype) {
        this.prototype = prototype;
    }

    public boolean isAutomaticRenewal() {
        return automaticRenewal;
    }

    public void setAutomaticRenewal(boolean automaticRenewal) {
        this.automaticRenewal = automaticRenewal;
    }

    public boolean isActivable() {
        return activable;
    }

    public void setActivable(boolean activable) {
        this.activable = activable;
    }
}
