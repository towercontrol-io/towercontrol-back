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

package com.disk91.capture.drivers.standard.sigfox;

import com.disk91.capture.drivers.standard.sigfox.sub.SigfoxAdvancedMessDuplicates;
import com.disk91.capture.drivers.standard.sigfox.sub.SigfoxAdvancedMessLocation;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SigfoxAdvancedMessage {

    @Schema(
            description = "Device Id",
            example = "20403C",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String device;

    @Schema(
            description ="UTC Time in S since EPOC",
            example = "154300205",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected long time;

    @Schema(
            description ="Data payload",
            example = "c7000ec1000b2db1",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String data;

    @Schema(
            description ="sequence number",
            example = "1012",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected int seq;

    @Schema(
            description ="Link quality indicator",
            example = "Excellent",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String lqi;

    @Schema(
            description ="country code",
            example = "250",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected int countryCode;

    @Schema(
            description ="operator name",
            example = "SIGFOX_France",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String operatorName;

    @Schema(
            description ="user id",
            example = "qmmkb9wgjqgyudziqhgv57hphswk337pm0",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String user;

    @Schema(
            description ="protocol version",
            example = "3",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected int version = 3;

    @Schema(
            description ="Message Location",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected SigfoxAdvancedMessLocation computedLocation;

    @Schema(
            description ="Message Duplicates",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected List<SigfoxAdvancedMessDuplicates> duplicates;



    // ============================================================================
    // Generated Getter & Setters
    // ============================================================================


    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public String getLqi() {
        return lqi;
    }

    public void setLqi(String lqi) {
        this.lqi = lqi;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public int getSeq() {
        return seq;
    }

    public void setSeq(int seq) {
        this.seq = seq;
    }

    public int getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(int countryCode) {
        this.countryCode = countryCode;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public SigfoxAdvancedMessLocation getComputedLocation() {
        return computedLocation;
    }

    public void setComputedLocation(SigfoxAdvancedMessLocation computedLocation) {
        this.computedLocation = computedLocation;
    }

    public List<SigfoxAdvancedMessDuplicates> getDuplicates() {
        return duplicates;
    }

    public void setDuplicates(List<SigfoxAdvancedMessDuplicates> duplicates) {
        this.duplicates = duplicates;
    }
}
