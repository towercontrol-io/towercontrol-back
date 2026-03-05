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
@Tag(name="modemCertificateResponse", description = "Details on a Modem certificate")
public class SigfoxApiv2CertificateModemResponse {

    @Schema(
            description ="Id of the certificate in hexadecimal form",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String id;

    @Schema(
            description ="Name of the certificate",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String name;

    @Schema(
            description ="Status of the certificate <br/>" +
                    "<ul>" +
                    "<li>0=ONGOING</li>" +
                    "<li>1=FINALIZED</li>" +
                    "</ul>",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected int status;

    @Schema(
            description ="Certificate’s key built from code and index",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String certificateKey;

    @Schema(
            description ="Name of the manufacturer",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String manufacturerName;

    @Schema(
            description ="Version of the certificate",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String version;

    @Schema(
            description ="Description of the certificate",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String description;

    @Schema(
            description ="List of modes of the certificate<br>" +
                    "<ul>" +
                    "<li>1=DOWNLINK</li>" +
                    "<li>2=MONARCH</li>" +
                    "</ul>",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected int[] modes;

    @Schema(
            description ="List of allowed RC<br/>" +
                    "<ul>" +
                    "<li>0=RC1</li>" +
                    "<li>1=RC2</li>" +
                    "<li>2=RC3</li>" +
                    "<li>3=RC101</li>" +
                    "<li>4=RC4</li>" +
                    "</ul>",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected int[] standards;

    @Tag(name="rcConfiguration", description = "Details per Rc for output")
    public static class RcConfiguration {

        @Schema(
                description ="Certified output power",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        protected int outputPower;

        @Schema(
                description ="Is the certificate power balanced?",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        protected boolean balancedLinkBudget;

        public int getOutputPower() {
            return outputPower;
        }

        public void setOutputPower(int outputPower) {
            this.outputPower = outputPower;
        }

        public boolean isBalancedLinkBudget() {
            return balancedLinkBudget;
        }

        public void setBalancedLinkBudget(boolean balancedLinkBudget) {
            this.balancedLinkBudget = balancedLinkBudget;
        }
    }

    @Schema(
            description ="List of the per-RC configuration of the certificate",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected List<RcConfiguration> standardCfgs;


    @Schema(
            description ="Input sensitivity",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected int inputSensitivity;

    @Schema(
            description ="Repeater function Flag",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected boolean repeaterFunction;

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

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getCertificateKey() {
        return certificateKey;
    }

    public void setCertificateKey(String certificateKey) {
        this.certificateKey = certificateKey;
    }

    public String getManufacturerName() {
        return manufacturerName;
    }

    public void setManufacturerName(String manufacturerName) {
        this.manufacturerName = manufacturerName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int[] getModes() {
        return modes;
    }

    public void setModes(int[] modes) {
        this.modes = modes;
    }

    public int[] getStandards() {
        return standards;
    }

    public void setStandards(int[] standards) {
        this.standards = standards;
    }

    public List<RcConfiguration> getStandardCfgs() {
        return standardCfgs;
    }

    public void setStandardCfgs(List<RcConfiguration> standardCfgs) {
        this.standardCfgs = standardCfgs;
    }

    public int getInputSensitivity() {
        return inputSensitivity;
    }

    public void setInputSensitivity(int inputSensitivity) {
        this.inputSensitivity = inputSensitivity;
    }

    public boolean isRepeaterFunction() {
        return repeaterFunction;
    }

    public void setRepeaterFunction(boolean repeaterFunction) {
        this.repeaterFunction = repeaterFunction;
    }
}
