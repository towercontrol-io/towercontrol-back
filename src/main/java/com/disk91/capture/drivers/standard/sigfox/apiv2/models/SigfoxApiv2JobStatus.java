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
@Tag(name="Job Status", description = "The informations about the devices already treated")
public class SigfoxApiv2JobStatus {

    @Schema(
            description ="The total number of devices given",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected int total;

    @Schema(
            description ="The number of devices successfully changed",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected int nbSuccess;

    @Schema(
            description ="The number of devices unsuccessfully changed",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected int nbErrors;


    @Schema(
            description ="reasons of each errors",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected List<SigfoxApiv2JobError> errors;


    // ============================================================
    // Generated Getters & Setters
    // ============================================================


    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getNbSuccess() {
        return nbSuccess;
    }

    public void setNbSuccess(int nbSuccess) {
        this.nbSuccess = nbSuccess;
    }

    public int getNbErrors() {
        return nbErrors;
    }

    public void setNbErrors(int nbErrors) {
        this.nbErrors = nbErrors;
    }

    public List<SigfoxApiv2JobError> getErrors() {
        return errors;
    }

    public void setErrors(List<SigfoxApiv2JobError> errors) {
        this.errors = errors;
    }
}
