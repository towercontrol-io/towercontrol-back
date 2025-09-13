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

import com.disk91.users.mdb.entities.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "User Accessible Roles", description = "Get the roles a user can self assign")
public class UserAccessibleRolesResponse {

    @Schema(
            description = "Name of the role",
            example = "ROLE_USER_ADMIN",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String name;

    @Schema(
            description = "Description slug for i18n",
            example = "role-user-admin",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String description;

    @Schema(
            description = "Description english string",
            example = "user administrator",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String enDescription;


    // ==========================
    // Init

    public void initFromRole(Role r) {
        this.name = r.getName();
        this.description = r.getDescription();
        this.enDescription = r.getEnDescription();
    }

    public static UserAccessibleRolesResponse getUserAccessibleRolesResponseFromRole(Role r) {
        UserAccessibleRolesResponse resp = new UserAccessibleRolesResponse();
        resp.initFromRole(r);
        return resp;
    }

    // ==========================
    // Getters & Setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEnDescription() {
        return enDescription;
    }

    public void setEnDescription(String enDescription) {
        this.enDescription = enDescription;
    }

}
