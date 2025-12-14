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

@Tag(name = "Device Sub State (when multiple state can be cumulative)", description = "Device sub State")
public class DevSubState implements CloneableObject<DevSubState> {

    public static final int DEV_SUBSTATE_NONE               = 0x00000000;
    public static final int DEV_SUBSTATE_UPGRADABLE         = 0x00000001;
    public static final int DEV_SUBSTATE_UPGRADED           = 0x00000002;
    public static final int DEV_SUBSTATE_CONFIGURABLE       = 0x00000004;
    public static final int DEV_SUBSTATE_CONFIGURED         = 0x00000008;
    public static final int DEV_SUBSTATE_NWK_SUBSCRIPTION   = 0x00000010;
    public static final int DEV_SUBSTATE_USR_SUBSCRIPTION   = 0x00000020;


    // Date of the location update
    @Schema(
            description = "Sub state bitfield",
            example = "0x11",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected int subState;


    // === CLONE ===

    public DevSubState clone() {
        DevSubState u = new DevSubState();
        u.setSubState(subState);
        return u;
    }

    // ==== SPECIAL METHODS ====


    // === GETTER / SETTER ===


    public int getSubState() {
        return subState;
    }

    public void setSubState(int subState) {
        this.subState = subState;
    }
}
