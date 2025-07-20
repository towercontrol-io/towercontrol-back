package com.disk91.devices.mdb.entities.sub;

import com.disk91.common.tools.CloneableObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Associated Group", description = "Groups this device is associated with for access rights and organization")
public class DevGroupAssociated implements CloneableObject<DevGroupAssociated> {

    // Group Name
    @Schema(
            description = "Id of the associated group",
            example = "123456",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String groupId;

    // === CLONE ===

    public DevGroupAssociated clone() {
        DevGroupAssociated u = new DevGroupAssociated();
        u.setGroupId(groupId);
        return u;
    }

    // === GETTER / SETTER ===


    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

}
