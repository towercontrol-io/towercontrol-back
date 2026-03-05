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
@Tag(name="urlCbDef", description = "Callback Url Definition")
public class SigfoxApiv2CallbackUrlDef {

    @Schema(
            description ="The callback URL. Mandatory for URL and BATCH_URL callbacks.",
            example = "example: http://myserver.com/sigfox/callback",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String urlPattern;

    @Schema(
            description ="The HTTP method used to send the callback (GET, POST or PUT). Mandatory for URL callbacks:<br/>" +
                    "<ul>" +
                    "<li>GET</li>" +
                    "<li>POST</li>" +
                    "<li>PUT</li>" +
                    "</ul>",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String httpMethod;

    @Schema(
            description ="True if this callback is used for downlink, else false.",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected boolean downlinkHook;

    @Schema(
            description ="Headers of the request. The header value can contain a variable " +
                    "(predefined or custom). Mandatory for URL callbacks.",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected KeyValue headers;

    @Schema(
            description ="Send SNI (Server Name Indication) for SSL/TLS connections. Used by BATCH_URL and URL callbacks (optional).",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected boolean sendSni;

    @Schema(
            description ="The body template of the request, only if httpMethpd is set to POST Or PUT. It can contain predefined " +
                    "and custom variables. Mandatory for URL callbacks",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String bodyTemplate;

    // ============================================================
    // Generated Getters & Setters
    // ============================================================


    public String getUrlPattern() {
        return urlPattern;
    }

    public void setUrlPattern(String urlPattern) {
        this.urlPattern = urlPattern;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public boolean isDownlinkHook() {
        return downlinkHook;
    }

    public void setDownlinkHook(boolean downlinkHook) {
        this.downlinkHook = downlinkHook;
    }

    public KeyValue getHeaders() {
        return headers;
    }

    public void setHeaders(KeyValue headers) {
        this.headers = headers;
    }

    public boolean isSendSni() {
        return sendSni;
    }

    public void setSendSni(boolean sendSni) {
        this.sendSni = sendSni;
    }

    public String getBodyTemplate() {
        return bodyTemplate;
    }

    public void setBodyTemplate(String bodyTemplate) {
        this.bodyTemplate = bodyTemplate;
    }
}
