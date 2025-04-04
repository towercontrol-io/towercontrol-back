package com.disk91.users.mdb.entities.sub;

import com.disk91.common.tools.CloneableObject;
import com.disk91.common.tools.CustomField;

import java.util.ArrayList;

public class UserAcl implements CloneableObject<UserAcl> {

    // Group Name
    protected String group;

    // Associated Role
    protected ArrayList<String> roles;

    // === CLONE ===

    public UserAcl clone() {
        UserAcl u = new UserAcl();
        u.setGroup(group);
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
}
