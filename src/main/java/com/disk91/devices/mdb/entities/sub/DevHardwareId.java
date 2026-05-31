/*
 * Copyright (c) - Paul Pinault (aka disk91) - 2025.
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
package com.disk91.devices.mdb.entities.sub;

import com.disk91.common.tools.CloneableObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;


@Tag(name = "Device Hardware Id", description = "Device Hardware Identifier Definition")
public class DevHardwareId implements CloneableObject<DevHardwareId> {

    // Type of hardware Id
    @Schema(
            description = "Type of hardware Id",
            example = "MAC",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String type;

    // Associated Id
    @Schema(
            description = "Associated Id",
            example = "10:10:10:10:10:10",
            requiredMode = Schema.RequiredMode.REQUIRED
    ) protected String id;

    // === INIT ===

    public static DevHardwareId newDevHardwareId(String type, String id) {
        DevHardwareId devHardwareId = new DevHardwareId();
        devHardwareId.type = type;
        devHardwareId.id = id;
        return devHardwareId;
    }

    // === CLONE ===

    public DevHardwareId clone() {
        DevHardwareId u = new DevHardwareId();
        u.setType(type);
        u.setId(id);
        return u;
    }

    // === GETTER / SETTER ===


    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
