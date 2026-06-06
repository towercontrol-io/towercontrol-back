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
import com.disk91.capture.interfaces.AbstractProcessor;
import com.disk91.common.interfaces.sigfox.SigfoxCommonMessage;
import com.disk91.common.interfaces.sigfox.apiv2.models.SigfoxApiv2Device;
import com.disk91.common.interfaces.sigfox.apiv2.services.ITSigfoxConnectionException;
import com.disk91.common.interfaces.sigfox.apiv2.wrappers.DeviceWrapper;
import com.disk91.capture.interfaces.AbstractProtocol;
import com.disk91.capture.interfaces.CaptureDataPivot;
import com.disk91.capture.interfaces.CaptureIngestResponse;
import com.disk91.capture.interfaces.sub.*;
import com.disk91.capture.mdb.entities.CaptureEndpoint;
import com.disk91.capture.mdb.entities.ProtocolIds;
import com.disk91.capture.mdb.entities.Protocols;
import com.disk91.capture.mdb.entities.sub.IdStateEnum;
import com.disk91.capture.services.CaptureEndpointCache;
import com.disk91.capture.services.CaptureEndpointService;
import com.disk91.capture.services.CaptureIdsService;
import com.disk91.common.config.CommonConfig;
import com.disk91.common.interfaces.chirpstack.ChirpstackV4HeliumPayload;
import com.disk91.common.tools.*;
import com.disk91.common.tools.computeLocation.ComputeLocation;
import com.disk91.common.tools.computeLocation.Location;
import com.disk91.common.tools.exceptions.*;
import com.disk91.devices.interfaces.DeviceState;
import com.disk91.devices.mdb.entities.Device;
import com.disk91.devices.mdb.entities.sub.DevAttribute;
import com.disk91.devices.mdb.entities.sub.DevGroupAssociated;
import com.disk91.devices.mdb.entities.sub.DevHardwareId;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Enumeration;
import java.util.List;

import static com.disk91.common.interfaces.sigfox.apiv2.wrappers.DeviceWrapper.NEWDEVICE_REGISTER_SUCCESS;
import static com.disk91.capture.interfaces.CaptureDataPivot.CaptureStatus.CAP_STATUS_SUCCESS;
import static com.disk91.capture.interfaces.sub.CaptureError.CaptureErrorLevel.CAP_ERROR_WARNING;
import static com.disk91.capture.mdb.entities.sub.IdStateEnum.UNKNOWN;

@Service
public class SigfoxV2Driver extends AbstractProtocol {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    // Init once
    protected ObjectMapper mapper;
    protected H3Core h3;

    @Autowired
    protected CommonConfig commonConfig;

    @Autowired
    protected DevicesNwkCache devicesNwkCache;

    @Autowired
    protected UserCommon userCommon;

    @Autowired
    protected GroupsServices groupsServices;

    @Autowired
    protected CaptureIdsService captureIdsService;

    @Autowired
    protected CaptureEndpointService captureEndpointService;

    @Autowired
    protected CaptureEndpointCache captureEndpointCache;

    @PostConstruct
    private void initSigfoxV2Driver() {
        log.info("[SigfoxV2Driver] Initializing Sigfox V2 Protocol Driver");
        mapper = new ObjectMapper();
        mapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);
        mapper.enable(JsonReadFeature.ALLOW_TRAILING_COMMA.mappedFeature());
        mapper.enable(JsonReadFeature.ALLOW_SINGLE_QUOTES.mappedFeature());
        try {
            // get the hex corresponding in a resolution of 14 - 3m2
            h3 = H3Core.newInstance();
        } catch (IOException ioException) {
            h3 = null;
            log.error("[SigfoxV2Driver] Failed to initialize H3Core: {}", ioException.getMessage());
        }

    }

    // =================================================================================================================
    // toPivot
    // =================================================================================================================

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

        // convert to Sigfox Payload
        SigfoxCommonMessage payload;
        try {
            String json = new String(rawData);
            log.debug(json);
            payload = mapper.readValue(json, SigfoxCommonMessage.class);
        } catch (JsonProcessingException x) {
            // failed to parse
            endpoint.incTotalBadPayloadFormat();
            log.debug("[capture][sigfoxv2] Conversion failed {}", x.getMessage());
            throw new ITParseException("capture-driver-sigfox-v2-failed-to-parse-json");
        }

        // Compute a network ID for frame cache and completion over multiple callback
        // This is not a unique Id
        String nwkId = payload.getDevice() + "-" + payload.getSeq();
        String nwkName = payload.getDevice().toLowerCase();

        // Get the associated device if exists
        Device d = null;
        try {
            d = devicesNwkCache.getDevice("Sigfox", "deveui", nwkName);
        } catch (ITNotFoundException x) {
            // This device is not known, if the endpoint allows auto-creation, let's do it
            boolean deviceOk = false;
            try {
                if (endpoint.getOneField("protocol-sigfox-auto-create").startsWith("true")) {
                    String group = endpoint.getOneField("protocol-sigfox-auto-create-group");
                    if (!group.startsWith("__none__")) {
                        // We have a default group identified for creation.
                        userCommon.getUserWithRolesAndGroups(
                                jwtUser,
                                UsersRolesCache.StandardRoles.ROLE_DEVICE_ADMIN.getRoleName(),
                                null,
                                group,
                                true
                        );
                        // It's Ok, we can create the new device.
                        Device dev = Device.newDevice(user.getLogin());
                        dev.getHardwareIds().add(DevHardwareId.newDevHardwareId("SIGFOX",nwkName));
                        dev.setDataStreamId(nwkName+"-"+Now.formatToYYYYMMDDUtc(Now.NowUtcMs()));
                        dev.setName("Sigfox-"+nwkName);
                        dev.setDevState(DeviceState.OPEN);
                        dev.setLastSeenDateMs(Now.NowUtcMs());
                        dev.addOneCommunicationId("Sigfox", "deveui", nwkName);
                        // @TODO - We will need to add the information herited from the devce type profile
                        dev.getAssociatedGroups().add(DevGroupAssociated.newGroupAssociated(group));
                        d = devicesNwkCache.createDevice(dev,"Sigfox", "deveui", nwkName);
                        if ( d != null  ) deviceOk = true;
                    }
                }
            } catch (ITNotFoundException | ITRightException ignore) {} // auto-creat => false or no group defined
            if ( !deviceOk ) {
                // else
                endpoint.incTotalBadDeviceRight();
                throw new ITRightException("capture-driver-sigfox-v2-unknown-device");
            }
        }

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
            throw new ITHackerException("capture-driver-sigfox-v2-no-rights-on-device");
        }

        // Once on this point, the authorization is OK
        final boolean encryptionRequired = endpoint.isEncrypted() || d.isDataEncrypted();

        // Manage Payload, it is HexString encoded and wil be base64 encoded
        if ( payload.getData() == null ) payload.setData("");
        String toStoreData = Base64.getEncoder().encodeToString(HexCodingTools.getByteArrayFromHexString(payload.getData()));
        if (encryptionRequired) {
            toStoreData = "$" + EncryptionHelper.encrypt(toStoreData, IV, commonConfig.getEncryptionKey());
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
                e.setMessage("capture-driver-sigfox-v2-invalid-base64-payload");
                p.getErrors().add(e);
                p.setPayloadSize(-1);
            }
        } else p.setPayloadSize(0);

        // No decoded payload on sigfox
        p.setDecodedPayload("");

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

        // No Sigfox network ID, use a non uniq id based on deveui + seqeui, purpose is to link multiple message in a short time windows
        meta.setNwkUuid(nwkId);
        meta.setNwkTimestamp(payload.getTimeMs());
        meta.setNwkTimeNs(0);
        meta.setNwkDeviceId(payload.getDevice());
        meta.setDeviceId(d.getId());
        meta.setDataStreamId(d.getDataStreamId());
        meta.setSessionCounter(0);
        meta.setFrameCounterUp(payload.getSeq());
        meta.setFrameCounterDwn(-1);
        meta.setFramePort(0);
        meta.setConfirmReq(payload.isAck()); // @TODO - this is only available with uplink frames...
        meta.setConfirmed(false); // default to false
        meta.setDownlinkReq(payload.isAck()); // @TODO - this is only available with uplink frames...
        meta.setDownlinkResp(false);

        CaptureRadioMetadata crmeta = meta.getRadioMetadata();
        crmeta.setAddress(payload.getDevice());
        crmeta.setFrequency(0);         // @TODO - based on RC ... we could put something here
        crmeta.setDataRate("");         // @TODO - based on RC ... we could put something here (but not enough)
        crmeta.setCustomParams(new ArrayList<>());
        crmeta.getCustomParams().add(CustomField.of("lqi",payload.getLqi()));
        crmeta.getCustomParams().add(CustomField.of("operatorName",payload.getOperatorName()));
        crmeta.getCustomParams().add(CustomField.of("countryCode",""+payload.getCountryCode()));

        // Network stations
        ArrayList<Location> locs = new ArrayList<>();
        if ( payload.getDuplicates() != null ) {
            payload.getDuplicates().forEach(ri -> {
                CaptureNwkStation ns = new CaptureNwkStation();
                ns.setCustomParams(new ArrayList<>());
                ns.setNkwTimestamp(payload.getTimeMs());
                ns.setNkwTimeNs(0);
                ns.setStationId(ri.getBsId());
                CaptureCalcLocation loc = new CaptureCalcLocation();
                loc.setLatitude(0.0);
                loc.setLongitude(0.0);
                loc.setEncrypted(false);
                loc.setAccuracy(-1);
                loc.setAltitude(0);
                loc.setHexagonId("");
                loc.setSource(CaptureLocationSource.NONE);
                ns.setStationLocation(loc);
                ns.setRssi((int)ri.getRssi());
                ns.setSnr(ri.getSnr());
                ns.setCustomParams(new ArrayList<>());
                ns.getCustomParams().add(CustomField.of("repeat",""+ri.getNbRep()));
                p.getNwkStations().add(ns);
            });

            CaptureCalcLocation ccmeta = new CaptureCalcLocation();
            ccmeta.setAccuracy(0);
            ccmeta.setAltitude(0);
            ccmeta.setLatitude(0.0);
            ccmeta.setLatitude(0.0);
            ccmeta.setAccuracy(-1);
            ccmeta.setSource(CaptureLocationSource.NONE);
            ccmeta.setHexagonId("");
            if ( payload.getComputedLocation() != null && GeolocationTools.isAValidCoordinate(payload.getComputedLocation().getLat(), payload.getComputedLocation().getLng()) ) {
                ccmeta.setLatitude(payload.getComputedLocation().getLat());
                ccmeta.setLongitude(payload.getComputedLocation().getLng());
                ccmeta.setAccuracy(payload.getComputedLocation().getRadius());
                ccmeta.setAltitude(0);
                switch (payload.getComputedLocation().getSource()) {
                    case 1: ccmeta.setSource(CaptureLocationSource.GPS); break;
                    case 2: ccmeta.setSource(CaptureLocationSource.NETWORK_RSSI); break;
                    case 6: ccmeta.setSource(CaptureLocationSource.WIFI_RSSI); break;
                    default: ccmeta.setSource(CaptureLocationSource.UNKNOWN); break;
                }
                if (encryptionRequired && h3 != null) {
                    ccmeta.setHexagonId("$"+EncryptionHelper.encrypt(h3.latLngToCellAddress(ccmeta.getLatitude(), ccmeta.getLongitude(), 15), IV, commonConfig.getEncryptionKey()));
                    ccmeta.setLatitude(0.0);
                    ccmeta.setLongitude(0.0);
                    ccmeta.setEncrypted(true);
                } else if ( h3 != null ){
                    ccmeta.setHexagonId(h3.latLngToCellAddress(ccmeta.getLatitude(), ccmeta.getLongitude(),14));
                } else ccmeta.setHexagonId("");
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

    // =================================================================================================================
    // ID management
    // =================================================================================================================

    public ProtocolIds checkId(
            CaptureEndpoint endpoint,           // Corresponding endpoint
            ProtocolIds _id
    ) throws ITOverQuotaException {

        try {
            String api = endpoint.getOneField("protocol-sigfox-api-endpoint");
            String user = endpoint.getOneField("protocol-sigfox-api-user");
            String pass = captureEndpointService.decrypteField(endpoint.getOneField("protocol-sigfox-api-password"));

            DeviceWrapper deviceWrapper = new DeviceWrapper(api, user, pass);
            String sigfoxId = _id.getOneField("sigfox-id");

            try {
                SigfoxApiv2Device dev = deviceWrapper.getRegisteredDeviceDetails(sigfoxId);
                // dev exists
                boolean modified = false;
                String pac = captureIdsService.decrypteField(_id.getOneField("sigfox-pac"));
                // decrypt pac
                if (!dev.getPac().equals(pac)) {
                    _id.getCustomConfig().removeIf(cf -> cf.getName().equals("sigfox-pac"));
                    CustomField cf = new CustomField();
                    cf.setName("sigfox-pac");
                    cf.setValue(captureIdsService.encrypteField(dev.getPac()));
                    _id.getCustomConfig().add(cf);
                    modified = true;
                }

                if ( _id.getState() == IdStateEnum.UNKNOWN && dev.getState() == 0 /* OK */ ) {
                    if ( dev.getToken() == null || dev.getToken().getState() == 2 /* NOT SEEN */ ) {
                        _id.setState(IdStateEnum.ASSIGNED);
                        modified = true;
                    } else {
                        // Other cases, the device State may be Not OK
                        _id.setState(IdStateEnum.IN_USE);
                        modified = true;
                    }
                } else if ( _id.getState() == IdStateEnum.ASSIGNED && dev.getToken() != null && dev.getToken().getState() != 2  ) {
                    _id.setState(IdStateEnum.IN_USE);
                    modified = true;
                } else if ( (_id.getState() == IdStateEnum.IN_USE || _id.getState() == IdStateEnum.WAITING_RENEWAL) && dev.getState() != 0 ) {
                    _id.setState(IdStateEnum.EXPIRED_IN_USE);
                    modified = true;
                } else if ( _id.getState() == IdStateEnum.WAITING_RENEWAL && dev.getState() == 0) {
                    // @TODO - we want to check if the device has been renewed, this is obtain with a subscription end or start changed
                    // once it's changed, the status is back to IN_USE and the renewal flag may be updated based on the setup

                } else if ( _id.getState() == IdStateEnum.RETURNED && dev.getState() != 0 ) {
                    _id.setState(IdStateEnum.EXPIRED_RETURNED);
                    modified = true;
                }

                // Assignment time
                long subscriptionStart = dev.getActivationTime(); // 0 (absent) when not activated
                long subscriptionEnd = 0;
                if ( dev.getUnsubscriptionTime() > 0 /* expired */) subscriptionEnd = dev.getUnsubscriptionTime();
                else {
                    if ( dev.getActivationTime() > 0 /* activated */) subscriptionEnd = Now.addOneYear(dev.getActivationTime());
                    else subscriptionEnd = Now.addOneYear(dev.getCreationTime());
                }
                // In case we are In_USE and subscription END is in the past, the subscription may have been renewed
                // Not sure if it works that way ... to be verifier
                while ( subscriptionEnd < Now.NowUtcMs() && dev.getState() == 0 /* OK */) {
                    subscriptionEnd = Now.addOneYear(subscriptionEnd);
                }

                if ( _id.getSubscriptionStartMs() != subscriptionStart ) {
                    _id.setSubscriptionStartMs(subscriptionStart);
                    modified = true;
                }
                if ( _id.getSubscriptionEndMs() != subscriptionEnd ) {
                    _id.setSubscriptionEndMs(subscriptionEnd);
                    modified = true;
                }

                if (modified) return _id;
                return null;
            } catch (ITNotFoundException x) {
                // One of the field is not existing on ID, this is not a normal situation
                log.warn("[capture][sigfoxv2] Missing mandatory field for sigfox id {}, stopping processing", sigfoxId);
                return null;
            } catch (ITSigfoxConnectionException e) {
                if ( e.getStatus() == HttpStatus.FORBIDDEN )  {
                    if ( _id.getState() != IdStateEnum.NOT_ASSIGNED && _id.getState() != IdStateEnum.REMOVED ) {
                        // The resource is not authorized, either because it has not yet been created in the backend system,
                        // or because it belongs to another user. Not blocking, process the next one. But to update the status
                        // it's important to check it's not the apikey that has been expired
                        if (!deviceWrapper.getFullListOfDevices(null, null, 1).isEmpty()) {
                            // The device is not in the backend
                            if (_id.getState() == IdStateEnum.UNKNOWN ) {
                                _id.setState(IdStateEnum.NOT_ASSIGNED);
                            } else {
                                _id.setState(IdStateEnum.REMOVED);
                            }
                            return _id;
                        } else {
                            // The apikey seems to not be valid, stop
                            log.warn("[capture][sigfoxv2] Sigfox Backed not responding, check credentials");
                            throw new ITOverQuotaException("capture-driver-backend-rate-limitation");
                        }
                    } else return null;
                } else if (e.getStatus() == HttpStatus.TOO_MANY_REQUESTS) {
                    throw new ITOverQuotaException("capture-driver-backend-rate-limitation");
                } else {
                    // Other error, we can retry later, but log it
                    log.error("[capture][sigfoxv2] Error from Sigfox API: status={} message={}", e.getStatus(), e.getErrorMessage());
                    return null;
                }
            }
        } catch ( ITNotFoundException x) {
            log.warn("[capture][sigfoxv2] Missing mandatory field for endpoint {}, stopping processing", endpoint.getRef());
        }
        throw new ITOverQuotaException("capture-driver-not-implemented");
    }


    // =================================================================================================================
    // Subscription management
    // =================================================================================================================


    // @TODO - we need to manage the quantity of subscription available in the "endpoint" and automatically close it
    // when there is no more subscription available

    public synchronized ProtocolIds subscribe(
            CaptureEndpoint endpoint,           // Current Endpoint
            List<CaptureEndpoint> endpoints,    // Possible other endpoints to search
            ProtocolIds id,                     // ID to subscribe - null to create a new one
            String familyId,                    // Device family Id (can be null)
            Long subscriptionEnd                // Subscription end in ms, the subscription must be valid until this date
    ) throws ITOverQuotaException, ITTooManyException, ITParseException {

        String sigfoxId = "";
        try {
            sigfoxId = id.getOneField("sigfox-id");
        } catch (ITNotFoundException e) {
            log.error("[kavale] ID {} does not have a sigfox-id field, unable to validate subscription", id.getCaptureId());
            throw new ITParseException("capture-driver-missing-sigfox-id");
        }

        if ( id.getState() == UNKNOWN || id.getLastScanMs() < Now.NowUtcMs() - 30*Now.ONE_FULL_DAY  ) {
            // rescan it
            try {
                id = captureIdsService.refreshOneId(endpoint,id);
            } catch (ITOverQuotaException e) {
                // not a big problem, the risk will be to try to recreate an existing device
                log.error("[kavale] Failed to rescan ID {} - {}", sigfoxId, e.getMessage());
            }
        }

        try {
            // make sure the endpoint is the right one for new allocation
            String api = null;
            String devType = null;
            String user = null;
            String pass = null;
            int renewal = 1; // default-false
            CaptureEndpoint targetEndpoint = endpoint;

            // Make sure we use a endpoint ready for subscription
            if ( ! endpoint.getOneField("protocol-sigfox-subscription-enable").startsWith("true") ) {
                // The endpoint is closed for subscription, we can try another on in the list
                targetEndpoint = null;
                for ( CaptureEndpoint endp : endpoints ) {
                    if ( endpoint.getOneField("protocol-sigfox-subscription-enable").startsWith("true") ) {
                        // get the first available
                        targetEndpoint = endp;
                        break;
                    }
                }
                if ( targetEndpoint == null ) {
                    log.error("[capture][sigfoxv2] No endpoint available for sigfox subscription");
                    throw new ITTooManyException("capture-driver-no-endpoint-available-for-subscription");
                }
            }
            // Get the required information
            api = targetEndpoint.getOneField("protocol-sigfox-api-endpoint");
            devType = targetEndpoint.getOneField("protocol-sigfox-device-type");
            user = targetEndpoint.getOneField("protocol-sigfox-api-user");
            pass = captureEndpointService.decrypteField(targetEndpoint.getOneField("protocol-sigfox-api-password"));
            try {
                String sRenew = targetEndpoint.getOneField("protocol-sigfox-renewal");
                if ( sRenew.startsWith("true") )  {
                    renewal =0; // true
                } else if ( sRenew.startsWith("default-false") ) {
                    renewal =1;
                } else if ( sRenew.startsWith("force-false") ) {
                    renewal =2;
                }
            } catch ( ITNotFoundException ignored) {
                // default is false
            }

            DeviceWrapper deviceWrapper = new DeviceWrapper(api, user, pass);

            // we may have a fresh ID information but this is not sure, considering UNKNOWN as NOT_ASSIGNED
            switch (id.getState()) {
                case UNKNOWN, NOT_ASSIGNED -> {
                    // Get the device family information
                    String certificationId = null;
                    if ( familyId != null ) {
                        // @TODO LATER
                        // get certificationId
                    }
                    try {
                        // register the device
                        String pac = captureIdsService.decrypteField(id.getOneField("sigfox-pac"));
                        if ( deviceWrapper.registerNewSigfoxDevice(
                                sigfoxId,
                                pac,
                                devType,
                                certificationId,
                                (renewal == 0)
                              ) == NEWDEVICE_REGISTER_SUCCESS )
                        {
                            id.setState(IdStateEnum.ASSIGNED);

                            // Update the subscription available
                            try {
                                String subs_s = targetEndpoint.getOneField("protocol-sigfox-subscriptions");
                                int subs = Integer.parseInt(subs_s);
                                if ( subs > 0 ) {
                                    subs--;
                                    targetEndpoint.setOneField("protocol-sigfox-subscriptions", ""+subs);
                                    if ( subs == 0 ) {
                                        targetEndpoint.setOneField("protocol-sigfox-subscription-enable", "false");
                                    }
                                    // save
                                    captureEndpointCache.save(targetEndpoint);
                                } else if ( subs == -2 ) {
                                    // @TODO call the backend to query the number of available subscription
                                    // not sure viable when the subscription is delayed ... but to be seen later
                                    log.error("[capture][sigfoxv2] backend driven subscription limit not yet implemented");
                                }
                            } catch (ITNotFoundException | NumberFormatException ignored) {
                                // invalid, lets assume default (0), no limit
                            }
                            return id;
                        } else {
                            // problem
                            log.warn("[capture][sigfoxv2] Failed to register sigfox device {}, check the backend for more details", sigfoxId);
                            throw new ITOverQuotaException("capture-driver-sigfox-registration-failure");
                        }
                    } catch ( ITNotFoundException x ) {
                        // Missing PAC
                        log.error("[capture][sigfoxv2] Missing sigfox device {} PAC key, registration failed", sigfoxId);
                        throw new ITParseException("capture-driver-missing-pac-key");
                    }
                }
                case ASSIGNED, IN_USE, WAITING_RENEWAL, RETURNED -> {
                    // @TODO - check the duration and verify is authorized to go to the end
                    log.info("[capture][sigfoxv2] Subscribe type renewal for {}", sigfoxId);
                    return null;
                }
                case EXPIRED_RETURNED, EXPIRED_IN_USE -> {
                    // @TODO - extends to the new expiration date
                    log.info("[capture][sigfoxv2] Subscribe type renewal with a expired subscription {}", sigfoxId);
                    return null;
                }
                case REMOVED -> {
                    // @TODO - this is a problem
                    log.error("[capture][sigfoxv2] Subscribe called for a REMOVED device {}, this is prohibited", sigfoxId);
                    throw new ITParseException("capture-driver-subscription-for-removed-device");
                }
                default -> {
                    // we should never be here until a case is missing in the switch
                    throw new ITParseException("capture-driver-incoherent-situation");
                }
            }

        } catch (ITNotFoundException x) {
            // Missing configuration
            log.error("[capture][sigfoxv2] Missing mandatory field in endpoint : {}", x.getMessage());
            throw new ITParseException("capture-driver-missing-configuration-field");
        }
    }


    public  ProtocolIds unsubscribe(
            CaptureEndpoint endpoint,           // Corresponding endpoint
            ProtocolIds _id                     // ID to subscribe
    ) throws
            ITOverQuotaException,               // In case the backend refuses the creation for a technical reason (retry later)
            ITParseException {                  // In case of a syntax error where retrial is not expected until fix

        throw new ITOverQuotaException("capture-driver-not-implemented");

    }

}
