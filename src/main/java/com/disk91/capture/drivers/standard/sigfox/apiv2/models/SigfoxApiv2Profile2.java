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
@Tag(name="Profile", description = "Defines a profile entity")
public class SigfoxApiv2Profile2 {

    @Schema(
            description ="The profile’s identifier",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String id;

    @Schema(
            description ="The profile’s name",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String name;

    @Schema(
            description ="The group information",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected SigfoxApiv2GroupMinimal group;

    @Schema(
            description ="Lists the role contained in this custom role.",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected List<SigfoxApiv2RoleMinimal> roles;



    // ============================================================
    // Generated Getters & Setters
    // ============================================================


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SigfoxApiv2GroupMinimal getGroup() {
        return group;
    }

    public void setGroup(SigfoxApiv2GroupMinimal group) {
        this.group = group;
    }

    public List<SigfoxApiv2RoleMinimal> getRoles() {
        return roles;
    }

    public void setRoles(List<SigfoxApiv2RoleMinimal> roles) {
        this.roles = roles;
    }
}
