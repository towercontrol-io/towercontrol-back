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
@Tag(name="Group", description = "Defines the group entity")
public class SigfoxApiv2Group extends SigfoxApiv2GroupBase {

    @Schema(
            description ="The group’s identifier.",
            example = "572f1204017975032d8ec1dd",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String id;

    @Schema(
            description ="The group’s name to ascii and lowercase.",
            example = "group 1",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String nameCI;


    @Schema(
            description ="The group’s path sorted by descending ancestor {id} (direct parent to farest parent)",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected List<SigfoxApiv2GroupMinimal> path;

    @Schema(
            description ="Number of prototype registered. Accessible only for groups under SO.",
            example = "56",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected int currentPrototypeCount;

    @Schema(
            description ="Maximum number of prototype allowed. Accessible only for groups under SO.",
            example = "100",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected int maxPrototypeAllowed;

    // ============================================================
    // Generated Getters & Setters
    // ============================================================


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNameCI() {
        return nameCI;
    }

    public void setNameCI(String nameCI) {
        this.nameCI = nameCI;
    }

    public List<SigfoxApiv2GroupMinimal> getPath() {
        return path;
    }

    public void setPath(List<SigfoxApiv2GroupMinimal> path) {
        this.path = path;
    }

    public int getCurrentPrototypeCount() {
        return currentPrototypeCount;
    }

    public void setCurrentPrototypeCount(int currentPrototypeCount) {
        this.currentPrototypeCount = currentPrototypeCount;
    }

    public int getMaxPrototypeAllowed() {
        return maxPrototypeAllowed;
    }

    public void setMaxPrototypeAllowed(int maxPrototypeAllowed) {
        this.maxPrototypeAllowed = maxPrototypeAllowed;
    }
}
