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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;


public class DeviceWrapper {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    protected String apiLogin;
    protected String apiPassword;
    protected String apiBackend;

    public DeviceWrapper(
            String apiBackend,
            String _apiLogin,
            String _apiPassword
    ) {
        this.apiLogin = _apiLogin;
        this.apiPassword = _apiPassword;
        this.apiBackend = apiBackend;
    }


    public static final int NEWDEVICE_REGISTER_ERROR = 0;
    public static final int NEWDEVICE_REGISTER_DELAYED = 1;
    public static final int NEWDEVICE_REGISTER_SUCESS = 2;

    /**
     * Register a new device into the sigfox backend.
     * This is made on the deviceType selected.
     * @param id
     * @param pac
     * @param deviceTypeId
     * @param certificate
     * @return
     */
    public int registerNewSigfoxDevice(
            String id,
            String pac,
            String deviceTypeId,
            String certificate
    ) {
        try {

            ITSigfoxConnection<SigfoxApiv2DeviceCreation, SigfoxApiv2GenericId> request = new ITSigfoxConnection<>(
                    this.apiBackend,
                    this.apiLogin,
                    this.apiPassword
            );

            SigfoxApiv2DeviceCreation device = new SigfoxApiv2DeviceCreation();
            device.setId(id);
            device.setName(id);
            device.setActivable(true);
            device.setAutomaticRenewal(true);
            device.setLat(0);
            device.setLng(0);
            if ( certificate != null ) {
                SigfoxApiv2ProductCertificate certif = new SigfoxApiv2ProductCertificate();
                certif.setKey(certificate);
                device.setProductCertificate(certif);
                device.setPrototype(false);
            } else {
                device.setPrototype(true);
            }
            device.setDeviceTypeId(deviceTypeId);
            device.setPac(pac);

            SigfoxApiv2GenericId deviceId = request.execute(
                    "POST",
                    "/api/v2/devices/",
                    null,
                    null,
                    device,
                    SigfoxApiv2GenericId.class
            );

            if ( deviceId.getId() != null ) {
                // Sigfox returns the device ID but removed the 0 on the left
                // this is a bit boring to manage, just let's assume if we have
                // an ID, we have the right ID.
               return NEWDEVICE_REGISTER_SUCESS;
            } else {
                log.warn("[capture][sigfox] Sigfox API return a invalid Id after registering the {} device : {}", id, deviceId.getId());
                return NEWDEVICE_REGISTER_ERROR;
            }

        } catch (ITSigfoxConnectionException x) {
            log.warn("[capture][sigfox] Problem during Sigfox connection :{}", x.errorMessage);
            return NEWDEVICE_REGISTER_ERROR;
        }

    }

    public SigfoxApiv2Device getRegisteredDeviceDetails(
            String deviceId
    ) throws ITSigfoxConnectionException {
        ITSigfoxConnection<String, SigfoxApiv2Device> request = new ITSigfoxConnection<>(
                this.apiBackend,
                this.apiLogin,
                this.apiPassword
        );
        return request.execute(
                "GET",
                "/api/v2/devices/"+deviceId,
                null,
                null,
                null,
                SigfoxApiv2Device.class
        );
    }


    public  List<SigfoxApiv2Device> getFullListOfDevices(
            String deviceId,        // Null to get all
            String deviceTypeId,    // Null to get all
            int limit               // Max number of devices to return, 0 for no limit
    ) {
        int counter = 0;
        try {
            ITSigfoxConnection<String, SigfoxApiv2DevicesList> request = new ITSigfoxConnection<>(
                    this.apiBackend,
                    this.apiLogin,
                    this.apiPassword
            );
            String qstring = "";
            if ( limit > 0 ) {
                qstring = "limit="+limit;
            } else {
                qstring = "limit=100"; // max
            }
            if ( deviceId != null ) {
                qstring += "&id="+deviceId;
            }
            if ( deviceTypeId != null ) {
                qstring += "&deviceTypeId="+deviceTypeId;
            }

            ArrayList<SigfoxApiv2Device> devices = new ArrayList<>();
            do {
                SigfoxApiv2DevicesList deviceList = request.execute(
                        "GET",
                        "/api/v2/devices/",
                        qstring,
                        null,
                        null,
                        SigfoxApiv2DevicesList.class
                );
                if ( deviceList != null && deviceList.getData() != null ) {
                    devices.addAll(deviceList.getData());
                } else {
                    // deviceList is null
                    return devices;
                }

                if (deviceList.getPaging().getNext() != null && ( limit == 0 || devices.size() < limit)) {
                    String url = deviceList.getPaging().getNext().split("[?]")[0];
                    url = url.substring(url.indexOf("/api/v2"));
                    String param = deviceList.getPaging().getNext().split("[?]")[1];
                    deviceList = request.execute(
                            "GET",
                            url,
                            param,
                            null,
                            null,
                            SigfoxApiv2DevicesList.class
                    );
                } else {
                    // deviceList is null
                    return devices;
                }

            } while (true);

        } catch (ITSigfoxConnectionException x) {
            log.error("[capture][sigfox] Problem accessing sigfox device list {}", x.errorMessage);
            return new ArrayList<SigfoxApiv2Device>();
        }

    }


    // ========================================================================
    // Change a device type value for a given device
    public boolean SigfoxApiSwitchDeviceType(String did, String dtidDst) {

        log.info("[capture][sigfox] SigfoxApiSwitchDeviceType( dev {} / devType {})", did, dtidDst);
        return SigfoxUpdateDeviceInformation(did, dtidDst, null);

    }

    // ========================================================================
    // Change a device type or device name
    public boolean SigfoxUpdateDeviceInformation(String did, String dtidDst, String name) {

        boolean ret = true;
        // change the device name
        if ( name != null && !name.isEmpty()) {
            try {

                ITSigfoxConnection<SigfoxApiv2DeviceUpdateName, String> request = new ITSigfoxConnection<>(
                        this.apiBackend,
                        this.apiLogin,
                        this.apiPassword
                );

                SigfoxApiv2DeviceUpdateName dn = new SigfoxApiv2DeviceUpdateName();
                dn.setName(name);
                request.execute(
                        "PUT",
                        "/api/v2/devices/"+did,
                        null,
                        null,
                        dn,
                        String.class
                );

            } catch (ITSigfoxConnectionException x) {
                log.error("[capture][sigfox] Problem updating device name {}", x.errorMessage);
                ret = false;
            }
        }

        // change the devicetype
        if ( dtidDst != null && !dtidDst.isEmpty()) {
            try {

                ITSigfoxConnection<SigfoxApiv2DeviceBulkTransferRequest, SigfoxApiv2JobResponse> request = new ITSigfoxConnection<>(
                        this.apiBackend,
                        this.apiLogin,
                        this.apiPassword
                );

                SigfoxApiv2DeviceBulkTransferRequest tr = new SigfoxApiv2DeviceBulkTransferRequest();
                tr.setDeviceTypeId(dtidDst);
                tr.initOne(did);

                SigfoxApiv2JobResponse resp = request.execute(
                        "POST",
                        "/api/v2/devices/bulk/transfer",
                        null,
                        null,
                        tr,
                        SigfoxApiv2JobResponse.class
                );
                log.info("actions:"+resp.getTotal());

            } catch (ITSigfoxConnectionException x) {
                log.error("Problem updating deviceType "+x.errorMessage);
                ret = false;
            }

        }
        return ret;


    }

    /**
     * Get a list of devices by deviceType
     * @param dtid
     * @return
     */
    public List<SigfoxApiv2Device> getSigfoxDevicesForDeviceType(String dtid) {

        return getFullListOfDevices( null, dtid, 0 );

    }

}
