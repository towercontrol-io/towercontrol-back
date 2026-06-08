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
 * AlertMediumMessage - One medium-specific message variant within a locale block.
 * Binds a delivery medium to its message text. Parameters are escaped with {1}, {2}, etc.
 * matching the position in the parent AlertTemplate parameter list.
 */
@Tag(name = "Alert Medium Message", description = "Medium-specific message text for one locale in an alert template")
public class AlertMediumMessage implements CloneableObject<AlertMediumMessage> {

    // Target delivery medium for this message variant
    @Schema(
            description = "Target delivery medium for this message variant",
            example = "EMAIL",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected AlertMedium medium;

    // Message body in markdown format; parameters escaped as {1}, {2}, ...
    @Schema(
            description = "Message body in markdown format; use {1}, {2}... to inject parameters",
            example = "Temperature alert on device {1}: current value is {2}°C",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String message;

    // === CREATE ===

    /**
     * Factory - create an AlertMediumMessage for a given medium and message text.
     * @param medium  - target delivery medium
     * @param message - message body with {n} parameter placeholders
     * @return new AlertMediumMessage instance
     */
    public static AlertMediumMessage of(AlertMedium medium, String message) {
        AlertMediumMessage m = new AlertMediumMessage();
        m.setMedium(medium);
        m.setMessage(message);
        return m;
    }

    // === CLONE ===

    public AlertMediumMessage clone() {
        AlertMediumMessage u = new AlertMediumMessage();
        u.setMedium(medium);
        u.setMessage(message);
        return u;
    }

    // === GETTER / SETTER ===

    public AlertMedium getMedium() {
        return medium;
    }

    public void setMedium(AlertMedium medium) {
        this.medium = medium;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

