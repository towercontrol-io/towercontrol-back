package com.disk91.groups.mdb.entities.sub;

import com.disk91.common.tools.CloneableObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.ArrayList;

@Tag(name = "Group Attribute Parameter", description = "Group custom attribute parameter")
public class GroupAttributeParam implements CloneableObject<GroupAttributeParam> {

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

    public GroupAttributeParam clone() {
        GroupAttributeParam u = new GroupAttributeParam();
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
