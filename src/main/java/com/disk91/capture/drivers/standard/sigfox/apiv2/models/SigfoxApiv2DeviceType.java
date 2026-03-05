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
@Tag(name = "Device type", description = "Defines the device type’s properties")
public class SigfoxApiv2DeviceType extends SigfoxApiv2DeviceTypeMinimal {


    @Schema(
            description = "Keep alive period in seconds (0 to not keep alive else 1800 second minimum)",
            example = "6000",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected long keepAlive;

    @Schema(
            description = "Email address to contact in case of problems occurring while executing a callback",
            example = "alert@foo.bar",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String alertEmail;


    @Schema(
            description = "The device type’s description",
            example = "My demo device type",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String description;

    @Schema(
            description = "The downlink mode to use for the devices of this device type.<br/>" +
                    "<ul>" +
                    "<li>0 -> DIRECT</li>" +
                    "<li>1 -> CALLBACK</li>" +
                    "<li>2 -> NONE</li>" +
                    "<li>3 -> MANAGE</li>" +
                    "</ul>",
            example = "1",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected int downlinkMode;



    @Schema(
            description = "Downlink data to be sent to the devices of this device type if downlinkMode is equal to 0. " +
                    "It must be an 8 byte length message given in hexadecimal string format.",
            example = "'{tapId}0000{rssi}'",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String downlinkDataString;

    @Schema(
            description = "The payload’s type<br/>" +
                    "<ul>" +
                    "<li>2 -> Regular (raw payload)</li>" +
                    "<li>3 -> Custom grammar</li>" +
                    "<li>4 -> Geolocation</li>" +
                    "<li>5 -> Display in ASCII</li>" +
                    "<li>6 -> Radio planning frame</li>" +
                    "<li>9 -> Sensitv2</li>" +
                    "</ul>",
            example = "1",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected int payloadType;

    @Schema(
            description = "The payload configuration. Required if the payload type is Custom, else ignored.",
            example = "Firmware_Version::uint:8 Voltage_Value::uint:16:little-endian",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String payloadConfig;


    @Schema(
            description = "The group entity owner of this device type",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected SigfoxApiv2GroupMinimal group;

    @Schema(
            description = "The contract entity attached of this device type",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected SigfoxApiv2ContractInfoMin contract;

    @Schema(
            description = "The geoloc payload attached to the device type",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected SigfoxApiv2GeolocPayloadConfig geolocPayloadConfig;

    @Schema(
            description = "Date of the creation of this device type (in milliseconds)",
            example = "1462801032158",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected long creationTime;

    @Schema(
            description = "Identifier of the user who created this device type.",
            example = "57309674171c857460043087",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String createdBy;

    @Schema(
            description = "Date of the last edition of this device type (in milliseconds)." +
                    "Note: this field is lastEditedTime according to Yml but this is not true.",
            example = "1462801032158",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected long lastEditionTime;

    @Schema(
            description = "Identifier of the user who last edited this device type.",
            example = "57309674171c857460043087",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String lastEditedBy;

    @Schema(
            description = "Allows the automatic renewal of devices attached to this device type",
            example = "true",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected boolean automaticRenewal;


    // ============================================================
    // Generated Getters & Setters
    // ============================================================


    public long getKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(long keepAlive) {
        this.keepAlive = keepAlive;
    }

    public String getAlertEmail() {
        return alertEmail;
    }

    public void setAlertEmail(String alertEmail) {
        this.alertEmail = alertEmail;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getDownlinkMode() {
        return downlinkMode;
    }

    public void setDownlinkMode(int downlinkMode) {
        this.downlinkMode = downlinkMode;
    }

    public String getDownlinkDataString() {
        return downlinkDataString;
    }

    public void setDownlinkDataString(String downlinkDataString) {
        this.downlinkDataString = downlinkDataString;
    }

    public int getPayloadType() {
        return payloadType;
    }

    public void setPayloadType(int payloadType) {
        this.payloadType = payloadType;
    }

    public String getPayloadConfig() {
        return payloadConfig;
    }

    public void setPayloadConfig(String payloadConfig) {
        this.payloadConfig = payloadConfig;
    }

    public SigfoxApiv2GroupMinimal getGroup() {
        return group;
    }

    public void setGroup(SigfoxApiv2GroupMinimal group) {
        this.group = group;
    }

    public SigfoxApiv2ContractInfoMin getContract() {
        return contract;
    }

    public void setContract(SigfoxApiv2ContractInfoMin contract) {
        this.contract = contract;
    }

    public SigfoxApiv2GeolocPayloadConfig getGeolocPayloadConfig() {
        return geolocPayloadConfig;
    }

    public void setGeolocPayloadConfig(SigfoxApiv2GeolocPayloadConfig geolocPayloadConfig) {
        this.geolocPayloadConfig = geolocPayloadConfig;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getLastEditedBy() {
        return lastEditedBy;
    }

    public void setLastEditedBy(String lastEditedBy) {
        this.lastEditedBy = lastEditedBy;
    }

    public boolean isAutomaticRenewal() {
        return automaticRenewal;
    }

    public void setAutomaticRenewal(boolean automaticRenewal) {
        this.automaticRenewal = automaticRenewal;
    }

    public long getLastEditionTime() {
        return lastEditionTime;
    }

    public void setLastEditionTime(long lastEditionTime) {
        this.lastEditionTime = lastEditionTime;
    }
}
