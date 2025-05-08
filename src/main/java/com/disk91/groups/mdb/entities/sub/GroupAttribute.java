package com.disk91.groups.mdb.entities.sub;

import com.disk91.common.tools.CloneableObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.ArrayList;
import java.util.List;

@Tag(name = "Group Attribute", description = "Group custom attribute pushed by external modules")
public class GroupAttribute implements CloneableObject<GroupAttribute> {

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
    ) protected List<GroupAttributeParam> params;

    // === CLONE ===

    public GroupAttribute clone() {
        GroupAttribute u = new GroupAttribute();
        u.setType(type);
        u.setParams(new ArrayList<>());
        for (GroupAttributeParam param : params) {
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

    public List<GroupAttributeParam> getParams() {
        return params;
    }

    public void setParams(List<GroupAttributeParam> params) {
        this.params = params;
    }
}
