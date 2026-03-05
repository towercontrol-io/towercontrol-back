/*
 * Copyright (c) - Paul Pinault (aka disk91) - 2025.
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
package com.disk91.capture.drivers.standard.sigfox.apiv2.wrappers.sub;

import com.disk91.capture.drivers.standard.sigfox.apiv2.models.SigfoxApiv2CallbackCreation;
import com.disk91.capture.drivers.standard.sigfox.apiv2.models.SigfoxApiv2DeviceTypeCreate;

import java.util.List;

public class DeviceTypeCreationCallback {

    // Callback to be created
    protected SigfoxApiv2CallbackCreation callback;
    // True when this callback is selected for downlinks
    protected boolean downlinkCallback = false;
    // Id of the callback created in Sigfox backend ( empty on create, returned update)
    protected String callBackId;

    public boolean isDownlinkCallback() {
        return downlinkCallback;
    }

    public void setDownlinkCallback(boolean downlinkCallback) {
        this.downlinkCallback = downlinkCallback;
    }

    public SigfoxApiv2CallbackCreation getCallback() {
        return callback;
    }

    public void setCallback(SigfoxApiv2CallbackCreation callback) {
        this.callback = callback;
    }

    public String getCallBackId() {
        return callBackId;
    }

    public void setCallBackId(String callBackId) {
        this.callBackId = callBackId;
    }
}
