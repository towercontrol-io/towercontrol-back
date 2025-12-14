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

import com.disk91.common.interfaces.KeyValues;
import com.disk91.common.tools.CloneableObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.ArrayList;
import java.util.List;

@Tag(name = "Device Attribute", description = "Device custom attribute pushed by external modules")
public class DevAttribute implements CloneableObject<DevAttribute> {

    // Group Name
    @Schema(
            description = "type of attribute for search, it must start by the module name",
            example = "billing",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String type;

    // Associated Role
    @Schema(
            description = "Associated parameters, content depends on type",
            example = "",
            requiredMode = Schema.RequiredMode.REQUIRED
    ) protected List<KeyValues> params;

    // === HELPER ===

    public void addOneSimpleParam(String type, String key, String value) {
        this.setType(type);
        if ( this.params == null ) this.params = new ArrayList<>();
        KeyValues kv = new KeyValues();
        kv.setOnKeyValue( key, value );
        this.params.add(kv);
    }

    // === CLONE ===

    public DevAttribute clone() {
        DevAttribute u = new DevAttribute();
        u.setType(type);
        u.setParams(new ArrayList<>());
        for (KeyValues param : params) {
            u.getParams().add(param.clone());
        }
        return u;
    }

    // === GETTER / SETTER ===


    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<KeyValues> getParams() {
        return params;
    }

    public void setParams(List<KeyValues> params) {
        this.params = params;
    }
}
