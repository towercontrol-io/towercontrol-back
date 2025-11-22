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
package com.disk91.capture.interfaces.sub;

import com.disk91.common.tools.CloneableObject;
import com.disk91.common.tools.CustomField;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

public class CaptureRadioMetadata implements CloneableObject<CaptureRadioMetadata>  {

    @Schema(
            description = "Frequency in MHz",
            example = "868.3",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected double frequency;

    @Schema(
            description = "Data rate description",
            example = "SF7BW125",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String dataRate;

    @Schema(
            description = "Network address, like IP Address or DevAddr",
            example = "10.0.0.1",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String address;

    @Schema(
            description = "Protocol dependant custom parameters as key-value pairs",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected List<CustomField> customParams;

    // ======================================
    @Override
    public CaptureRadioMetadata clone() {
        CaptureRadioMetadata o = new CaptureRadioMetadata();
        o.frequency = this.frequency;
        o.dataRate = this.dataRate;
        o.address = this.address;
        o.customParams = new ArrayList<>();
        if (this.customParams != null) {
            for (CustomField f : this.customParams) {
                o.customParams.add(f.clone());
            }
        }
        return o;
    }

    // ======================================

    public double getFrequency() {
        return frequency;
    }

    public void setFrequency(double frequency) {
        this.frequency = frequency;
    }

    public String getDataRate() {
        return dataRate;
    }

    public void setDataRate(String dataRate) {
        this.dataRate = dataRate;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public List<CustomField> getCustomParams() {
        return customParams;
    }

    public void setCustomParams(List<CustomField> customParams) {
        this.customParams = customParams;
    }
}
