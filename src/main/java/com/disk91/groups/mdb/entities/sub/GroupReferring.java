package com.disk91.groups.mdb.entities.sub;

import com.disk91.common.tools.CloneableObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Referring Group", description = "Groups this group referred to for configuration")
public class GroupReferring implements CloneableObject<GroupReferring> {

    // Group Name
    @Schema(
            description = "Id of the referring group",
            example = "123456",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String groupId;

    // Associated Role
    @Schema(
            description = "Priority associated to this group, higher is more important",
            example = "1",
            requiredMode = Schema.RequiredMode.REQUIRED
    ) protected int priority;

    // === CLONE ===

    public GroupReferring clone() {
        GroupReferring u = new GroupReferring();
        u.setGroupId(groupId);
        u.setPriority(priority);
        return u;
    }

    // === GETTER / SETTER ===


    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }
}
