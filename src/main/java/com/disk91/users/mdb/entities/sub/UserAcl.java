package com.disk91.users.mdb.entities.sub;

import com.disk91.common.tools.CloneableObject;
import com.disk91.common.tools.CustomField;
import com.disk91.users.services.UsersRolesCache;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.ArrayList;

@Tag(name = "User ACL", description = "User right on groups definitions")
public class UserAcl implements CloneableObject<UserAcl> {

    // Group Name
    @Schema(
            description = "Group",
            example = "mygroup",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String group;

    // Local Name
    @Schema(
            description = "Allows to change the name of the group locally",
            example = "my favorite group",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String localName;

    // Associated Role
    @Schema(
            description = "Associated Roles",
            example = "[ROLE_DEVICE_READ, ROLE_DEVICE_WRITE]",
            requiredMode = Schema.RequiredMode.REQUIRED
    ) protected ArrayList<String> roles;

    // === FUNCTIONAL ===

    /**
     * Check if a given role has been attributed to the user
     * @param role
     */
    public boolean isInRole(String role) {
        if ( this.roles == null ) return false;
        for ( String r : this.roles ) {
            if ( r.compareTo(role) == 0 ) return true;
        }
        return false;
    }

    public boolean isInRole(UsersRolesCache.StandardRoles role) {
        return isInRole(role.getRoleName());
    }

    public void addRole(String role) {
        if ( this.roles == null ) this.roles = new ArrayList<>();
        if ( !isInRole(role) ) this.roles.add(role);
    }

    // === CLONE ===

    public UserAcl clone() {
        UserAcl u = new UserAcl();
        u.setGroup(group);
        u.setLocalName(localName);
        u.setRoles(new ArrayList<>(roles));
        return u;
    }

    // === GETTER / SETTER ===

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public ArrayList<String> getRoles() {
        return roles;
    }

    public void setRoles(ArrayList<String> roles) {
        this.roles = roles;
    }

    public String getLocalName() {
        return localName;
    }

    public void setLocalName(String localName) {
        this.localName = localName;
    }
}
