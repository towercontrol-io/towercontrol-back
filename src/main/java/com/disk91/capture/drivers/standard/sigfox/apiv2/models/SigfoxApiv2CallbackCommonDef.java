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
@Tag(name="commonCbDef", description = "Common Information for callback")
public class SigfoxApiv2CallbackCommonDef {

    @Schema(
            description ="The callback’s channel.<br/>" +
                    "<ul>" +
                    "<li>URL</li>" +
                    "<li>BATCH_URL</li>" +
                    "<li>EMAIL</li>" +
                    "</ul>",
            example = "URL",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String channel;

    @Schema(
            description ="The callback’s type.<br/>" +
                    "<ul>" +
                    "<li>0 -> DATA</li>" +
                    "<li>1 -> SERVICE</li>" +
                    "<li>2 -> ERROR</li>" +
                    "</ul>",
            example = "0",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected int callbackType;

    @Schema(
            description ="The callback’s subtype. The subtype must be valid against its type.<br/>" +
                    "<ul>" +
                    "<li>0 -> STATUS callback sending information about the status of a device (available for SERVICE callbacks)</li>" +
                    "<li>1 -> GEOLOC callback sent on a message that can be geolocated (available for SERVICE callbacks)</li>" +
                    "<li>2 -> UPLINK callback for an uplink message (available for DATA callbacks)</li>" +
                    "<li>3 -> BIDIR callback for a bidirectional message (available for DATA callbacks)</li>" +
                    "<li>4 -> ACKNOWLEDGE callback sent on a downlink acknowledged message (available for SERVICE callbacks)</li>" +
                    "<li>5 -> REPEATER callback triggered when a repeater sends an OOB (available for SERVICE callbacks)</li>" +
                    "</ul>",
            example = "2",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected Integer callbackSubtype;
    // Due to the fact this field should not be present when the type is 2..
    // report as FRASIG-6022, resolution pending then it can be a int ...


    @Schema(
            description ="The custom payload configuration. Only for DATA callbacks",
            example = "int1::uint:8 int2::uint:8",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String payloadConfig;

    @Schema(
            description ="True to enable the callback, false else",
            example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected boolean enabled;

    /*
    @Schema(
            description ="True to duplicates callback, false else",
            example = "false",
            requiredMode = Schema.RequiredMode.REQUIRED

    )
    protected boolean sendDuplicate;
    */
    /*
    @Schema(
            description ="True if last use of the callback fails, false else",
            example = "false",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected boolean dead;
    */

    // ============================================================
    // Custom Getters & Setters
    // ============================================================

    // Due to the fact this field should not be present when the type is 2..
    // report as FRASIG-6022, resolution pending
    public int getCallbackSubtype() {
        return (this.callbackSubtype !=null)?callbackSubtype:-1;
    }


    // ============================================================
    // Generated Getters & Setters
    // ============================================================


    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public int getCallbackType() {
        return callbackType;
    }

    public void setCallbackType(int callbackType) {
        this.callbackType = callbackType;
    }


    public void setCallbackSubtype(int callbackSubtype) {
        this.callbackSubtype = callbackSubtype;
    }

    public String getPayloadConfig() {
        return payloadConfig;
    }

    public void setPayloadConfig(String payloadConfig) {
        this.payloadConfig = payloadConfig;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
