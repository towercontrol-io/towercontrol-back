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
package com.disk91.users.api.interfaces.sub;

import com.disk91.users.mdb.entities.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Role information", description = "Role structure for front-end usage")
public class RoleItf {

    @Schema(
            description = "Role name, required for modifications",
            example = "ROLE_USER_ADMIN",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String name;

    @Schema(
            description = "Role description used for i18n",
            example = "role-user-admin-desc",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String description;

    @Schema(
            description = "Role description in english",
            example = "User administrator",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String enDescription;

    @Schema(
            description = "False for technical roles not assignable by user.",
            example = "true",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected boolean assignable;


    // ==========================
    // Constructors

    public static RoleItf getRoleItfFromRole(Role _r) {
        RoleItf r = new RoleItf();
        r.setName(_r.getName());
        r.setDescription(_r.getDescription());
        r.setEnDescription(_r.getEnDescription());
        r.setAssignable(_r.isAssignable());
        return r;
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

    public boolean isAssignable() {
        return assignable;
    }

    public void setAssignable(boolean assignable) {
        this.assignable = assignable;
    }
}
