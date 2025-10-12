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

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "User Update Request", description = "User update, request the current structure")
public class UserUpdateBodyRequest {

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
            description = "To indicate if the group list is to be considered",
            example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected boolean considerGroups;

    @Schema(
            description = "To indicate if group list contains subgroups",
            example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected boolean considerSubs;


    @Schema(
            description = "To indicate if the ACL list is to be considered",
            example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected boolean considerACLs;


    // ==========================
    // Construction

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

    public boolean isConsiderGroups() {
        return considerGroups;
    }

    public void setConsiderGroups(boolean considerGroups) {
        this.considerGroups = considerGroups;
    }

    public boolean isConsiderACLs() {
        return considerACLs;
    }

    public void setConsiderACLs(boolean considerACLs) {
        this.considerACLs = considerACLs;
    }

    public boolean isConsiderSubs() {
        return considerSubs;
    }

    public void setConsiderSubs(boolean considerSubs) {
        this.considerSubs = considerSubs;
    }
}
