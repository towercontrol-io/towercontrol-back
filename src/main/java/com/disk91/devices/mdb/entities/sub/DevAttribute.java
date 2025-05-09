package com.disk91.devices.mdb.entities.sub;

import com.disk91.common.tools.CloneableObject;
import com.disk91.groups.mdb.entities.sub.GroupAttributeParam;
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
    ) protected List<GroupAttributeParam> params;

    // === CLONE ===

    public DevAttribute clone() {
        DevAttribute u = new DevAttribute();
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
