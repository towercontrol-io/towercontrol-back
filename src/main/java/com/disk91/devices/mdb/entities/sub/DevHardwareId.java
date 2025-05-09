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
