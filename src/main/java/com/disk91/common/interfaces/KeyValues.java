package com.disk91.common.interfaces;

import com.disk91.common.tools.CloneableObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.ArrayList;

@Tag(name = "Key with multiple values attached", description = "Structure with one key and multiple values attached")
public class KeyValues implements CloneableObject<KeyValues> {

    // Group Name
    @Schema(
            description = "Name of the key",
            example = "networkId",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String key;

    // Associated Role
    @Schema(
            description = "Associated values",
            example = "[1234, abcd]",
            requiredMode = Schema.RequiredMode.REQUIRED
    ) protected ArrayList<String> values;

    // === CLONE ===

    public KeyValues clone() {
        KeyValues u = new KeyValues();
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
