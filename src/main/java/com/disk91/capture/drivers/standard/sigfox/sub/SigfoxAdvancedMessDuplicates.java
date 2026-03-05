/*
 * Copyright (c) - Paul Pinault (aka disk91) - 2019.
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

package com.disk91.capture.drivers.standard.sigfox.sub;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SigfoxAdvancedMessDuplicates {

    @Schema(
            description = "Base Station ID",
            example = "0DC0",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String bsId;

    @Schema(
            description = "Rssi",
            example = "-122.0",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected double rssi;

    @Schema(
            description = "Repetition number",
            example = "3",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected int nbRep;


    @Schema(
            description = "Snr",
            example = "6.3",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected double snr;


    // ============================================================================
    // Generated Getter & Setters
    // ============================================================================


    public String getBsId() {
        return bsId;
    }

    public void setBsId(String bsId) {
        this.bsId = bsId;
    }

    public double getRssi() {
        return rssi;
    }

    public void setRssi(double rssi) {
        this.rssi = rssi;
    }

    public int getNbRep() {
        return nbRep;
    }

    public void setNbRep(int nbRep) {
        this.nbRep = nbRep;
    }

    public double getSnr() {
        return snr;
    }

    public void setSnr(double snr) {
        this.snr = snr;
    }
}
