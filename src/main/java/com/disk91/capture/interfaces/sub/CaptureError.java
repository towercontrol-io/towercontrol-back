/*
 * Copyright (c) - Paul Pinault (aka disk91) - 2024.
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
package com.disk91.capture.interfaces.sub;

import com.disk91.common.tools.CloneableObject;
import io.swagger.v3.oas.annotations.media.Schema;

public class CaptureError implements CloneableObject<CaptureError> {

    public enum CaptureErrorLevel {
        CAP_ERROR_NONE,
        CAP_ERROR_WARNING,
        CAP_ERROR_ERROR,
    }

    @Schema(
            description = "Error level",
            example = "CAP_ERROR_NONE",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected CaptureErrorLevel level;

    @Schema(
            description = "Error code",
            example = "203",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String code;

    @Schema(
            description = "Error message",
            example = "Server was down",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String message;


    @Override
    public CaptureError clone() {
        CaptureError o = new CaptureError();
        o.setLevel(this.level);
        o.setCode(this.code);
        o.setMessage(this.message);
        return o;
    }

    // === GETTER / SETTER ===

    public CaptureErrorLevel getLevel() {
        return level;
    }

    public void setLevel(CaptureErrorLevel level) {
        this.level = level;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
