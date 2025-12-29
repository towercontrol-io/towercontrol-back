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
package com.disk91.capture.drivers.standard;

import com.disk91.capture.api.interfaces.CaptureResponseItf;
import com.disk91.capture.interfaces.AbstractProtocol;
import com.disk91.capture.interfaces.CaptureDataPivot;
import com.disk91.capture.interfaces.CaptureIngestResponse;
import com.disk91.capture.interfaces.sub.*;
import com.disk91.capture.mdb.entities.CaptureEndpoint;
import com.disk91.capture.mdb.entities.Protocols;
import com.disk91.common.config.CommonConfig;
import com.disk91.common.interfaces.chirpstack.ChirpstackV4HeliumPayload;
import com.disk91.common.tools.*;
import com.disk91.common.tools.computeLocation.ComputeLocation;
import com.disk91.common.tools.computeLocation.Location;
import com.disk91.common.tools.exceptions.ITHackerException;
import com.disk91.common.tools.exceptions.ITNotFoundException;
import com.disk91.common.tools.exceptions.ITParseException;
import com.disk91.common.tools.exceptions.ITRightException;
import com.disk91.devices.mdb.entities.Device;
import com.disk91.devices.mdb.entities.sub.DevGroupAssociated;
import com.disk91.devices.services.DevicesNwkCache;
import com.disk91.groups.services.GroupsServices;
import com.disk91.users.mdb.entities.User;
import com.disk91.users.services.UserCommon;
import com.disk91.users.services.UsersRolesCache;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uber.h3core.H3Core;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Enumeration;

import static com.disk91.capture.interfaces.CaptureDataPivot.CaptureStatus.CAP_STATUS_SUCCESS;
import static com.disk91.capture.interfaces.sub.CaptureError.CaptureErrorLevel.CAP_ERROR_WARNING;

@Service
public class LoraWanHeliumChirpstackV4Driver extends AbstractProtocol {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    // Init once
    protected ObjectMapper mapper;
    protected H3Core h3;

    // Add a driver seeed
    private static final String IV = "90f7adcf874990333cf159c1857fe539";

    @Autowired
    protected CommonConfig commonConfig;

    @Autowired
    protected DevicesNwkCache devicesNwkCache;

    @Autowired
    protected UserCommon userCommon;

    @Autowired
    protected GroupsServices groupsServices;

    @PostConstruct
    private void initLoraWanHeliumChirpstackV4Driver() {
        log.info("[LoraWanHeliumChirpstackV4Driver] Initializing LoraWan Helium Chirpstack V4 Protocol Driver");
        mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.enable(JsonReadFeature.ALLOW_TRAILING_COMMA.mappedFeature());
        mapper.enable(JsonReadFeature.ALLOW_SINGLE_QUOTES.mappedFeature());
        try {
            // get the hex corresponding in a resolution of 14 - 3m2
            h3 = H3Core.newInstance();
        } catch (IOException ioException) {
            h3 = null;
            log.error("[LoraWanHeliumChirpstackV4Driver] Failed to initialize H3Core: {}", ioException.getMessage());
        }

    }

    public CaptureIngestResponse toPivot(
            String jwtUser,                     // User in the Jwt token (may be an api key)
            User user,                          // User calling the endpoint
            CaptureEndpoint endpoint,           // Corresponding endpoint
            Protocols protocol,                 // Corresponding protocol
            byte[] rawData,                     // Raw data received
            HttpServletRequest request          // HTTP Request info
    ) throws
            ITParseException,                   // Will generate a trace
            ITRightException,                   // No trace
            ITHackerException                   // Will generated a trace in audit (with caching to not overload)
    {
        endpoint.incTotalInDriver();

        CaptureDataPivot p = CaptureDataPivot.initPivot();
        p.setRxUuid(this.getRxUUID());
        p.setRxCaptureRef(endpoint.getRef());

        // convert to Chirpstack Payload
        ChirpstackV4HeliumPayload payload;
        try {
            String json = new String(rawData);
            log.debug(json);
            payload = mapper.readValue(json, ChirpstackV4HeliumPayload.class);
        } catch (JsonProcessingException x) {
            // failed to parse
            endpoint.incTotalBadPayloadFormat();
            log.debug("[HeliumChirpstackV4Protocol] Conversion failed {}", x.getMessage());
            throw new ITParseException("capture-driver-helium-chirpstackv4-failed-to-parse-json");
        }

        // Get the associated device if exists
        Device d = null;
        try {
            d = devicesNwkCache.getDevice("LoRa", "deveui", payload.getDeviceInfo().getDevEui().toLowerCase());

            // Check rights on device
            // When the JWT user is in group ROLE_GLOBAL_CAPTURE it has global access on devices
            boolean authorized = false;
            try {
                userCommon.getUserWithRolesAndGroups(
                        jwtUser,
                        UsersRolesCache.StandardRoles.ROLE_GLOBAL_CAPTURE.getRoleName(),
                        null,
                        null,
                        false
                );
                authorized = true;
            } catch (ITNotFoundException | ITRightException x) {
                // Not a global capture user, we need to check device groups

                // When the JWT user is an apikey we need to check the apikey rights searching for the groups of the
                // device and ensuring the apikey has write rights on at least one of them.
                for (DevGroupAssociated g : d.getAssociatedGroups()) {
                    try {
                        userCommon.getUserWithRolesAndGroups(
                                jwtUser,
                                UsersRolesCache.StandardRoles.ROLE_DEVICE_WRITE.getRoleName(),
                                null,
                                g.getGroupId(),
                                true
                        );
                        authorized = true;
                        break; // found, no need to continue
                    } catch (ITNotFoundException | ITRightException ignore) {
                        // not in this group, try the next one
                    }
                }
            }
            if (!authorized) {
                endpoint.incTotalBadDeviceRight();
                throw new ITHackerException("capture-driver-helium-chirpstackv4-no-rights-on-device");
            }

        } catch (ITNotFoundException x) {
            // This device is not known
            endpoint.incTotalBadDeviceRight();
            throw new ITRightException("capture-driver-helium-chirpstackv4-unknown-device");
        }
        // Once on this point, the authorization is OK
        final boolean encryptionRequired = endpoint.isEncrypted() || d.isDataEncrypted();

        // Manage Payload, it will be Base64 encoded and encrypted is required
        String toStoreData = payload.getData();
        if (encryptionRequired) {
            toStoreData = "$" + EncryptionHelper.encrypt(payload.getData(), IV, commonConfig.getEncryptionKey());
        }
        p.setPayload(toStoreData);
        if ( payload.getData() != null && !payload.getData().isEmpty()) {
            try {
                byte[] decodedPayload = Base64.getDecoder().decode(payload.getData());
                p.setPayloadSize(decodedPayload.length);
            } catch (IllegalArgumentException x) {
                CaptureError e = new CaptureError();
                e.setCode("004");
                e.setLevel(CAP_ERROR_WARNING);
                e.setMessage("capture-driver-helium-chirpstackv4-invalid-base64-payload");
                p.getErrors().add(e);
                p.setPayloadSize(-1);
            }
        } else p.setPayloadSize(0);


        if (payload.getObject() != null && !payload.getObject().isEmpty()) {
            if (encryptionRequired) {
                p.setDecodedPayload("$" + EncryptionHelper.encrypt(payload.getObject(), IV, commonConfig.getEncryptionKey()));
            } else {
                p.setDecodedPayload(Base64.getEncoder().encodeToString(payload.getObject().getBytes()));
            }
        } else {
            p.setDecodedPayload("");
        }

        p.setNwkStatus(CaptureDataPivot.NetworkStatus.NWK_STATUS_SUCCESS);
        p.setCoredDump("");
        p.setIngestOwnerId(user.getLogin());
        // IP source - potential personal data
        String _ip = EncryptionHelper.encrypt(Tools.getRemoteIp(request), IV, commonConfig.getEncryptionKey());
        p.setFromIp(_ip);
        p.setProcessingChainClass(endpoint.getProcessingClassName());

        // Copy the headers except Authorization and some others
        Enumeration<String> headerNames = request.getHeaderNames();
        if ( headerNames != null ) {
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                if ( this.keepHeader(headerName) ) {
                    String headerValue = request.getHeader(headerName);
                    CustomField cf = new CustomField();
                    cf.setName(headerName);
                    cf.setValue(headerValue);
                    p.getHeaders().add(cf);
                }
            }
        }

        CaptureMetaData meta = p.getMetadata();
        meta.setNwkUuid(payload.getDeduplicationId());
        try {
            meta.setNwkTimestamp(DateConverters.StringDateToMs(payload.getTime()));
            meta.setNwkTimeNs(0);
        } catch (ITParseException x) {
            CaptureError e = new CaptureError();
            e.setCode("001");
            e.setLevel(CAP_ERROR_WARNING);
            e.setMessage("capture-driver-helium-chirpstackv4-bad-timestamp-format");
            p.getErrors().add(e);
            meta.setNwkTimestamp(Now.NowUtcMs());
            meta.setNwkTimeNs(0);
        }
        meta.setNwkDeviceId(payload.getDeviceInfo().getDevEui());
        meta.setDeviceId(d.getId());
        meta.setSessionCounter(0);
        meta.setFrameCounterUp(payload.getfCnt());
        meta.setFrameCounterDwn(payload.getfCntDown());
        meta.setFramePort(payload.getfPort());
        meta.setConfirmReq(payload.isConfirmed());
        meta.setConfirmed(false); // default to false
        meta.setDownlinkReq(payload.isConfirmed());
        meta.setDownlinkResp(false);

        CaptureRadioMetadata crmeta = meta.getRadioMetadata();
        crmeta.setAddress(payload.getDevAddr());
        crmeta.setFrequency(0);
        crmeta.setDataRate(""+payload.getDr());
        if ( payload.getTxInfo() != null ) {
            crmeta.setFrequency(payload.getTxInfo().getFrequency());
        }

        // Network stations
        ArrayList<Location> locs = new ArrayList<>();
        if ( payload.getRxInfo() != null ) {
            payload.getRxInfo().forEach(ri -> {
                CaptureNwkStation ns = new CaptureNwkStation();
                ns.setCustomParams(new ArrayList<>());
                try {
                    if ( ri.getNsTime() != null && !ri.getNsTime().isEmpty() ) {
                        ns.setNkwTimestamp(DateConverters.StringNsDateToMs(ri.getNsTime()));
                        ns.setNkwTimeNs(DateConverters.StringNsDateToNsRemaining(ri.getNsTime()));
                    } else {
                        ns.setNkwTimestamp(DateConverters.StringDateToMs(ri.getTime()));
                        ns.setNkwTimeNs(0);
                    }
                } catch (ITParseException x) {
                    CaptureError e = new CaptureError();
                    e.setCode("002");
                    e.setLevel(CAP_ERROR_WARNING);
                    e.setMessage("capture-driver-helium-chirpstackv4-bad-timestamp-format");
                    p.getErrors().add(e);
                    ns.setNkwTimestamp(0);
                    ns.setNkwTimeNs(0);
                }
                if ( ri.getMetadata() != null ) {
                    if ( ri.getMetadata().getGateway_id() != null ) {
                        ns.setStationId(ri.getMetadata().getGateway_id());
                    } else {
                        CaptureError e = new CaptureError();
                        e.setCode("003");
                        e.setLevel(CAP_ERROR_WARNING);
                        e.setMessage("capture-driver-helium-chirpstackv4-missing-gateway-id");
                        p.getErrors().add(e);
                        ns.setStationId(ri.getGatewayId()); // use mac as backup
                    }
                    CaptureCalcLocation loc = new CaptureCalcLocation();
                    loc.setLatitude(0.0);
                    loc.setLongitude(0.0);
                    loc.setEncrypted(false);
                    try {
                        if ( ri.getMetadata().getGateway_lat() != null && ri.getMetadata().getGateway_long() != null ) {
                            loc.setLatitude(Double.parseDouble(ri.getMetadata().getGateway_lat()));
                            loc.setLongitude(Double.parseDouble(ri.getMetadata().getGateway_long()));
                        }
                    } catch (NumberFormatException ignored) {}
                    loc.setAccuracy(300);
                    loc.setAltitude(0);
                    if ( GeolocationTools.isAValidCoordinate(loc.getLatitude(),loc.getLongitude())) {
                        // add until encryption
                        locs.add( new Location(loc.getLatitude(), loc.getLongitude(), 300, ri.getRssi()) );
                    }
                    if (encryptionRequired && h3 != null) {
                        loc.setHexagonId(EncryptionHelper.encrypt(h3.latLngToCellAddress(loc.getLatitude(), loc.getLongitude(), 15), IV, commonConfig.getEncryptionKey()));
                        loc.setLatitude(0.0);
                        loc.setLongitude(0.0);
                        loc.setEncrypted(true);
                    } else {
                        loc.setHexagonId(ri.getMetadata().getGateway_h3index());
                    }
                    ns.setStationLocation(loc);
                    ns.getCustomParams().add(new CustomField("gateway-name", ri.getMetadata().getGateway_name()));
                    ns.getCustomParams().add(new CustomField("gateway-region", ri.getMetadata().getRegi()));
                }
                ns.setRssi(ri.getRssi());
                ns.setSnr(ri.getSnr());
                p.getNwkStations().add(ns);
            });

            CaptureCalcLocation ccmeta = new CaptureCalcLocation();
            if ( locs.isEmpty() ) {
                ccmeta.setAccuracy(0);
                ccmeta.setAltitude(0);
                ccmeta.setLatitude(0.0);
                ccmeta.setLatitude(0.0);
                ccmeta.setHexagonId("");
            } else {
                try {
                    Location l = ComputeLocation.computeLocation(locs);
                    ccmeta.setLatitude(l.lat);
                    ccmeta.setLongitude(l.lng);
                    ccmeta.setAccuracy(l.radius);
                    ccmeta.setAltitude(0);
                    if (encryptionRequired && h3 != null) {
                        ccmeta.setHexagonId(EncryptionHelper.encrypt(h3.latLngToCellAddress(ccmeta.getLatitude(), ccmeta.getLongitude(), 15), IV, commonConfig.getEncryptionKey()));
                        ccmeta.setLatitude(0.0);
                        ccmeta.setLongitude(0.0);
                        ccmeta.setEncrypted(true);
                    } else if ( h3 != null ){
                        ccmeta.setHexagonId(h3.latLngToCellAddress(ccmeta.getLatitude(), ccmeta.getLongitude(),14));
                    } else ccmeta.setHexagonId("");
                } catch (ITNotFoundException x) {
                    ccmeta.setAccuracy(0);
                    ccmeta.setAltitude(0);
                    ccmeta.setLatitude(0.0);
                    ccmeta.setLongitude(0.0);
                    ccmeta.setHexagonId("");
                }
            }
            meta.setCalculatedLocation(ccmeta);
        }
        p.setStatus(CAP_STATUS_SUCCESS);

        return new CaptureIngestResponse(
                p,
                new CaptureResponseItf() // just a 204 No Content right now
        );
    }


    public  CaptureResponseItf fallbackResponse(
            CaptureIngestResponse ingestResponse
    ) throws ITNotFoundException {

        // For now, just return a 204 No Content
        return new CaptureResponseItf();

    }
}
