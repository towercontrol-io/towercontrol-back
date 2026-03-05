/*
 * Copyright (c) 2018.
 *
 *  This is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  this software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  -------------------------------------------------------------------------------
 *  Author : Paul Pinault aka disk91
 *  See https://www.disk91.com
 *
 *  Commercial license of this software can be obtained contacting disk91.com or ingeniousthings.fr
 *  -------------------------------------------------------------------------------
 *
 */

/**
 * -------------------------------------------------------------------------------
 * This file is part of IngeniousThings Sigfox-Api.
 *
 * IngeniousThings Sigfox-Api is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * IngeniousThings Sigfox-Api is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 * -------------------------------------------------------------------------------
 * Author : Paul Pinault aka disk91
 * See https://www.disk91.com
 * ----
 * More information about IngeniousThings : https://www.ingeniousthings.fr
 * ----
 * Commercial license of this software can be obtained contacting ingeniousthings
 * -------------------------------------------------------------------------------
 */
package com.disk91.capture.drivers.standard.sigfox.apiv2.models;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Tag(name="User", description = "User Details")
public class SigfoxApiv2UserRead extends SigfoxApiv2UserCommon {

    @Schema(
            description ="The user’s identifier.",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String id;

    @Schema(
            description ="The user’s email.",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String email;

    @Schema(
            description ="The encoded user’s password (only available with a specific right).",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String password;

    @Schema(
            description ="If the user account is locked or not.",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected boolean locked;

    @Schema(
            description ="The user’s creation time since epoch.",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected long creationTime;

    @Schema(
            description ="The user’s last login time.",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected long lastLoginTime;


    @Schema(
            description ="Defines the rights the user has on a group",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected List<SigfoxApiv2UserRoleRead> userRoles;

    // ============================================================
    // Generated Getters & Setters
    // ============================================================


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }

    public long getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(long lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }

    public List<SigfoxApiv2UserRoleRead> getUserRoles() {
        return userRoles;
    }

    public void setUserRoles(List<SigfoxApiv2UserRoleRead> userRoles) {
        this.userRoles = userRoles;
    }
}
