/*
 * Copyright (c) - Paul Pinault (aka disk91) - 2020.
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

package com.disk91.capture.drivers.standard.sigfox.apiv2.models;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Tag(name = "Device type", description = "Defines the device type’s properties")
public class SigfoxApiv2DeviceTypeGlobal extends SigfoxApiv2DeviceType {

    @Schema(
            description = "The list of the contracts associated with the device type",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected List<SigfoxApiv2ContractInfoMin> contracts;

    @Schema(
            description = "The list of the contracts that were associated with the device type at some point, but are not anymore.",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected List<SigfoxApiv2ContractInfoMin> detachedContracts;

    // ---


    public List<SigfoxApiv2ContractInfoMin> getContracts() {
        return contracts;
    }

    public void setContracts(List<SigfoxApiv2ContractInfoMin> contracts) {
        this.contracts = contracts;
    }

    public List<SigfoxApiv2ContractInfoMin> getDetachedContracts() {
        return detachedContracts;
    }

    public void setDetachedContracts(List<SigfoxApiv2ContractInfoMin> detachedContracts) {
        this.detachedContracts = detachedContracts;
    }
}

