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

package com.disk91.capture.drivers.standard.sigfox.apiv2.wrappers;

import com.disk91.capture.drivers.standard.sigfox.apiv2.models.*;
import com.disk91.capture.drivers.standard.sigfox.apiv2.services.ITSigfoxConnection;
import com.disk91.capture.drivers.standard.sigfox.apiv2.services.ITSigfoxConnectionException;
import com.disk91.capture.drivers.standard.sigfox.apiv2.wrappers.sub.DeviceTypeCreation;
import com.disk91.capture.drivers.standard.sigfox.apiv2.wrappers.sub.DeviceTypeCreationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DeviceTypeWrapper {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    protected String apiLogin;
    protected String apiPassword;

    public DeviceTypeWrapper(
            String _apiLogin,
            String _apiPassword
    ) {
        this.apiLogin = _apiLogin;
        this.apiPassword = _apiPassword;
    }

    /**
     * Get the DeviceTypeId from a given name
     * Return null when the deviceType is not found
     * @param name
     * @param contract
     * @param exact
     * @return
     */
    public String getSigfoxDeviceTypeIdFromName(
            String name,
            String contract,
            boolean exact
    ) {
        if ( !exact ) log.error("[capture][sigfox] Use of exact = false in the Wrapper, this is not anymore supported");

        try {

            ITSigfoxConnection<String, SigfoxApiv2DeviceTypeListResponse> request = new ITSigfoxConnection<>(
                    this.apiLogin,
                    this.apiPassword
            );

            SigfoxApiv2DeviceTypeListResponse devicetypes = request.execute(
                    "GET",
                    "/api/v2/device-types/",
                    "name="+name+"contractId="+contract,
                    null,
                    null,
                    SigfoxApiv2DeviceTypeListResponse.class
            );

            if ( devicetypes != null ) {
                for (SigfoxApiv2DeviceTypeGlobal dt : devicetypes.getData() ) {
                    if ( dt.getName().compareToIgnoreCase(name) == 0 ) {
                        // found
                        return dt.getId();
                    }
                }
                return null;
            }
            return null;
        } catch (ITSigfoxConnectionException x) {
            log.warn("[capture][sigfox] Problem during Sigfox connection : {}",x.errorMessage );
            return null;
        }

    }

    /**
     * Get a device type information from the deviceTypeId
     * The callback list is not completed in the API v2
     * @param id
     * @return
     */
    public SigfoxApiv2DeviceTypeGlobal getSigfoxDeviceTypeById(String id) {

        try {

            ITSigfoxConnection<String, SigfoxApiv2DeviceTypeGlobal> request = new ITSigfoxConnection<>(
                    this.apiLogin,
                    this.apiPassword
            );

            SigfoxApiv2DeviceTypeGlobal dt = request.execute(
                    "GET",
                    "/api/v2/device-types/"+id,
                    null,
                    null,
                    null,
                    SigfoxApiv2DeviceTypeGlobal.class
            );
            return dt;
        } catch (ITSigfoxConnectionException x) {
            log.warn("[capture][sigfox] Problem during Sigfox connection :{}",x.errorMessage );
            return null;
        }

    }

    public SigfoxApiv2DeviceTypeListResponse getAccessibleDeviceType() {
        try {

            ITSigfoxConnection<String, SigfoxApiv2DeviceTypeListResponse> request = new ITSigfoxConnection<>(
                    this.apiLogin,
                    this.apiPassword
            );

            return request.execute(
                    "GET",
                    "/api/v2/device-types/",
                    null,
                    null,
                    null,
                    SigfoxApiv2DeviceTypeListResponse.class
            );

        } catch (ITSigfoxConnectionException x) {
            log.warn("[capture][sigfox] Problem during Sigfox connection :{}",x.errorMessage );
            return null;
        }

    }

    public boolean isSigfoxDeviceTypeExists(String name, String contract, boolean exact) {
        return (getSigfoxDeviceTypeIdFromName(name,contract, exact) != null);
    }

    public boolean isSigfoxDeviceTypeByIdExists(String id) {
        return (getSigfoxDeviceTypeById(id) != null);
    }


    public DeviceTypeCreation publishSigfoxDeviceType(DeviceTypeCreation dtc) {

        try {
            ITSigfoxConnection<SigfoxApiv2DeviceTypeCreate, SigfoxApiv2DeviceTypeId> request = new ITSigfoxConnection<>(
                    this.apiLogin,
                    this.apiPassword
            );

            SigfoxApiv2DeviceTypeId _t = request.execute(
                    "POST",
                    "/api/v2/device-types/",
                    null,
                    null,
                    dtc.getDeviceType(),
                    SigfoxApiv2DeviceTypeId.class
            );
            log.debug("[capture][sigfox] Devicetype creation success ({})", _t.getId());
            dtc.setDeviceTypeId(_t.getId());

            // Create the associated callback
            try {
                for (DeviceTypeCreationCallback callback : dtc.getCallbacks()) {

                    ITSigfoxConnection<SigfoxApiv2CallbackCreation, SigfoxApiv2CallbackId> reqCb = new ITSigfoxConnection<>(
                            this.apiLogin,
                            this.apiPassword
                    );

                    SigfoxApiv2CallbackId _c = reqCb.execute(
                            "POST",
                            "/api/v2/device-types/" + dtc.getDeviceTypeId() + "/callbacks",
                            null,
                            null,
                            callback.getCallback(),
                            SigfoxApiv2CallbackId.class
                    );
                    callback.setCallBackId(_c.getId());

                    // Assign for downlink
                    if ( callback.isDownlinkCallback() ) {
                        ITSigfoxConnection<String, String> dwn = new ITSigfoxConnection<>(
                                this.apiLogin,
                                this.apiPassword
                        );
                        String _dwn = dwn.execute(
                                "PUT",
                                "/api/v2/device-types/" + dtc.getDeviceTypeId() + "/callbacks/" + callback.getCallBackId() + "/downlink",
                                null,
                                null,
                                null,
                                String.class
                        );
                    }
                    log.debug("[capture][sigfox] Downlink {} created type({}) sub({})", _c.getId(), callback.getCallback().getCallbackType(), callback.getCallback().getCallbackSubtype());
                }
                log.info("[capture][sigfox] DeviceType {} created", dtc.getDeviceTypeId());
            } catch  (ITSigfoxConnectionException x) {
                log.warn("[capture][sigfox] Problem during Sigfox connection :{}", x.errorMessage);
                // Need to cancel all the existing callbacks
                for (DeviceTypeCreationCallback callback : dtc.getCallbacks()) {
                    if (callback.getCallBackId() != null && !callback.getCallBackId().isEmpty()) {
                        ITSigfoxConnection<String, String> reqCb = new ITSigfoxConnection<>(
                                this.apiLogin,
                                this.apiPassword
                        );
                        String _c = reqCb.execute(
                                "DELETE",
                                "/api/v2/device-types/" + dtc.getDeviceTypeId() + "/callbacks/"+callback.getCallBackId(),
                                null,
                                null,
                                null,
                                String.class
                        );
                    }
                }
                // Need to cancel the device type
                ITSigfoxConnection<String, String> reqCb = new ITSigfoxConnection<>(
                        this.apiLogin,
                        this.apiPassword
                );
                String _c = reqCb.execute(
                        "DELETE",
                        "/api/v2/device-types/" + dtc.getDeviceTypeId(),
                        null,
                        null,
                        null,
                        String.class
                );
                return null;
            }
        } catch (ITSigfoxConnectionException x) {
            log.warn("[capture][sigfox] Problem during Sigfox connection :{}", x.errorMessage);
            return null;
        }
        return dtc;
    }

    public boolean deleteSigfoxDeviceType(String id) {

        try {
            ITSigfoxConnection<String, String> request = new ITSigfoxConnection<>(
                    this.apiLogin,
                    this.apiPassword
            );

            request.execute(
                    "DELETE",
                    "/api/v2/device-types/"+id,
                    null,
                    null,
                    null,
                    String.class
            );


            return true;
        } catch (ITSigfoxConnectionException x) {
            log.warn("[capture][sigfox] Problem during Sigfox connection :{}", x.errorMessage);
            return false;
        }

    }

}
