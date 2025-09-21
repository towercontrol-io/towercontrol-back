/*
 * Copyright (c) - Paul Pinault (aka disk91) - 2024.
 *
 *    Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 *    and associated documentation files (the "Software"), to deal in the Software without restriction,
 *    including without limitation the rights to use, copy, modify, merge, publish, distribute,
 *    sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 *    furnished to do so, subject to the following conditions:
 *
 *    The above copyright notice and this permission notice shall be included in all copies or
 *    substantial portions of the Software.
 *
 *    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 *    FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
 *    OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 *    WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 *    IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.disk91.users.api.interfaces;

import com.disk91.common.tools.CustomField;
import com.disk91.users.mdb.entities.User;
import com.disk91.users.mdb.entities.sub.UserAcl;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "User Update", description = "User update structure, for local or global updates")
public class UserUpdateBody {

    @Schema(
            description = "User to be modified login (hash)",
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String login;


    @Schema(
            description = "To indicate if the roles structure is to be considered",
            example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected boolean considerRoles;


    @Schema(
            description = "List of affectable roles",
            example = "[ROLE_GROUP_LADMIN, ROLE_DEVICE_READ]",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected List<String> roles;


    @Schema(
            description = "To indicate if the group list is to be considered",
            example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected boolean considerGroups;


    @Schema(
            description = "List of owned groups (shortId)",
            example = "[ XdfhYII, Jy6FSHB ]",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected List<String> groups;

    @Schema(
            description = "To indicate if the ACL list is to be considered",
            example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected boolean considerACLs;


    @Schema(
            description = "List of acls",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected List<UserAcl> acls;

    // ==========================
    // Construction

    public static UserUpdateBody getUserUpdateBodyFromUser(User u) {
        UserUpdateBody uub = new UserUpdateBody();

        uub.setLogin(u.getLogin());
        uub.setConsiderRoles(true);
        uub.setRoles(u.getRoles());
        uub.setConsiderGroups(true);
        uub.setGroups(u.getGroups());
        uub.setConsiderACLs(true);
        uub.setAcls(u.getAcls());

        return uub;
    }


    // ==========================
    // Getters & Setters


    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public boolean isConsiderRoles() {
        return considerRoles;
    }

    public void setConsiderRoles(boolean considerRoles) {
        this.considerRoles = considerRoles;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public boolean isConsiderGroups() {
        return considerGroups;
    }

    public void setConsiderGroups(boolean considerGroups) {
        this.considerGroups = considerGroups;
    }

    public List<String> getGroups() {
        return groups;
    }

    public void setGroups(List<String> groups) {
        this.groups = groups;
    }

    public boolean isConsiderACLs() {
        return considerACLs;
    }

    public void setConsiderACLs(boolean considerACLs) {
        this.considerACLs = considerACLs;
    }

    public List<UserAcl> getAcls() {
        return acls;
    }

    public void setAcls(List<UserAcl> acls) {
        this.acls = acls;
    }
}
