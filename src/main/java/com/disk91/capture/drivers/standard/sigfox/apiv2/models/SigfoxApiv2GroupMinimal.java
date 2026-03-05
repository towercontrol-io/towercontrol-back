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

@JsonIgnoreProperties(ignoreUnknown = true)
@Tag(name = "minimalGroup", description = "Defines a group entity minimal information")
public class SigfoxApiv2GroupMinimal {

    @Schema(
            description = "The group’s identifier",
            example = "57309674171c857460043087",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String id;

    @Schema(
            description = "The group’s name",
            example = "Group 1",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String name;

    @Schema(
            description = "Group’s type." +
                    "<ul>" +
                    "<li>0 -> SO</li>" +
                    "<li>2 -> Basic</li>" +
                    "<li>5 -> SVNO</li>" +
                    "<li>6 -> Partners</li>" +
                    "<li>7 -> NIP</li>" +
                    "<li>8 -> DIST</li>" +
                    "<li>9 -> Channel</li>" +
                    "<li>10 -> Starter</li>" +
                    "<li>11 -> Partner</li>" +
                    "</ul>",
            example = "0",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected int type;

    @Schema(
            description = "The depth level of the group in hierarchy.",
            example = "1",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected int level;

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

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }
}
