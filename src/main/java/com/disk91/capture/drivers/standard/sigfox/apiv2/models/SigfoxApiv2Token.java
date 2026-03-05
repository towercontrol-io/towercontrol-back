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
@Tag(name="Token", description = "Contains the token information of the device")
public class SigfoxApiv2Token {

    @Schema(
            description ="Token State.<br/>" +
                    "<ul>" +
                    "<li>0 -> OK</li>" +
                    "<li>1 -> OFF_CONTRACT</li>" +
                    "<li>2 -> TOKEN_NOT_CONSUMED</li>" +
                    "<li>3 -> INVALID_TOKEN</li>" +
                    "</ul>",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected int state;


    @Schema(
            description ="Token state description Valid Off Contract Not Consumed Invalid",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String detailMessage;

    @Schema(
            description ="The device’s communication end time (in milliseconds since the Unix Epoch)",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected long end;

    @Schema(
            description ="The number of free messages left for this token",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected int freeMessages;

    @Schema(
            description ="The number of free messages already sent for this token",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected int freeMessagesSent;

    // ============================================================
    // Generated Getters & Setters
    // ============================================================


    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public String getDetailMessage() {
        return detailMessage;
    }

    public void setDetailMessage(String detailMessage) {
        this.detailMessage = detailMessage;
    }

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public int getFreeMessages() {
        return freeMessages;
    }

    public void setFreeMessages(int freeMessages) {
        this.freeMessages = freeMessages;
    }

    public int getFreeMessagesSent() {
        return freeMessagesSent;
    }

    public void setFreeMessagesSent(int freeMessagesSent) {
        this.freeMessagesSent = freeMessagesSent;
    }
}
