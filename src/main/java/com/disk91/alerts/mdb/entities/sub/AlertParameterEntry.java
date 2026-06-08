/*
 * Copyright (c) - Paul Pinault (aka disk91) - 2026.
 *
 *    Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 *    and associated documentation files (the "Software"), to deal in the Software without restriction,
 *    including without limitation the rights to use, copy, modify, merge, publish, distribute,
 *    sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 *    furnished to do so, subject to the following conditions:
 *
 *    The above copyright notice and this permission notice shall be included in all copies or
 *    substantial portions of the Software.
 *
 *    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 *    FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
 *    OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 *    WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 *    IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.disk91.alerts.mdb.entities.sub;

import com.disk91.common.tools.CloneableObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * AlertParameterEntry - One entry in the ordered parameter list of an AlertTemplate.
 * The type identifies the category of the parameter; param carries an optional associated value
 * (e.g. the custom parameter name for CUSTOM_PARAM, the URL template for ALERT_LINK).
 */
@Tag(name = "Alert Parameter Entry", description = "One ordered parameter entry used to build alert messages")
public class AlertParameterEntry implements CloneableObject<AlertParameterEntry> {

    // Category of the dynamic parameter
    @Schema(
            description = "Category of the dynamic parameter injected into the message",
            example = "DEVICE_NAME",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected AlertParameter type;

    // Optional associated value, depends on the parameter type (e.g. custom name, link template)
    @Schema(
            description = "Associated value when required by the parameter type, empty otherwise",
            example = "temperature",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String param;

    // === CREATE ===

    /**
     * Factory - create an AlertParameterEntry without an associated value.
     * @param type - category of the parameter
     * @return new AlertParameterEntry instance
     */
    public static AlertParameterEntry of(AlertParameter type) {
        AlertParameterEntry e = new AlertParameterEntry();
        e.setType(type);
        e.setParam("");
        return e;
    }

    /**
     * Factory - create an AlertParameterEntry with an associated value.
     * @param type  - category of the parameter
     * @param param - associated value (custom name, link template, etc.)
     * @return new AlertParameterEntry instance
     */
    public static AlertParameterEntry of(AlertParameter type, String param) {
        AlertParameterEntry e = new AlertParameterEntry();
        e.setType(type);
        e.setParam(param);
        return e;
    }

    // === CLONE ===

    public AlertParameterEntry clone() {
        AlertParameterEntry u = new AlertParameterEntry();
        u.setType(type);
        u.setParam(param);
        return u;
    }

    // === GETTER / SETTER ===

    public AlertParameter getType() {
        return type;
    }

    public void setType(AlertParameter type) {
        this.type = type;
    }

    public String getParam() {
        return param;
    }

    public void setParam(String param) {
        this.param = param;
    }
}

