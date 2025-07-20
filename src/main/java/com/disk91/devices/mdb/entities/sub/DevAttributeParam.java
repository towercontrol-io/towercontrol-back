package com.disk91.devices.mdb.entities.sub;

import com.disk91.common.tools.CloneableObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.ArrayList;

@Tag(name = "Device Attribute Parameter", description = "Device custom attribute parameter")
public class DevAttributeParam implements CloneableObject<DevAttributeParam> {

    // Group Name
    @Schema(
            description = "Name of the parameter",
            example = "account",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String key;

    // Associated Role
    @Schema(
            description = "Associated values",
            example = "[ 1234, abcd]",
            requiredMode = Schema.RequiredMode.REQUIRED
    ) protected ArrayList<String> values;

    // === CLONE ===

    public DevAttributeParam clone() {
        DevAttributeParam u = new DevAttributeParam();
        u.setKey(key);
        u.setValues(new ArrayList<>(values));
        return u;
    }

    // === GETTER / SETTER ===


    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public ArrayList<String> getValues() {
        return values;
    }

    public void setValues(ArrayList<String> values) {
        this.values = values;
    }
}
