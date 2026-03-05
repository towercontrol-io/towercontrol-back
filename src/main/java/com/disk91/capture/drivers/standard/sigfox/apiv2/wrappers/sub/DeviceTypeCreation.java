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

public class DeviceTypeCreation {

    // Device type to be created
    protected SigfoxApiv2DeviceTypeCreate deviceType;
    // Id of the device type created in Sigfox backend ( empty on create, returned update)
    protected String deviceTypeId;
    // Callbacks to be created and linked to this device type
    protected List<DeviceTypeCreationCallback> callbacks;

    public SigfoxApiv2DeviceTypeCreate getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(SigfoxApiv2DeviceTypeCreate deviceType) {
        this.deviceType = deviceType;
    }

    public String getDeviceTypeId() {
        return deviceTypeId;
    }

    public void setDeviceTypeId(String deviceTypeId) {
        this.deviceTypeId = deviceTypeId;
    }

    public List<DeviceTypeCreationCallback> getCallbacks() {
        return callbacks;
    }

    public void setCallbacks(List<DeviceTypeCreationCallback> callbacks) {
        this.callbacks = callbacks;
    }
}
