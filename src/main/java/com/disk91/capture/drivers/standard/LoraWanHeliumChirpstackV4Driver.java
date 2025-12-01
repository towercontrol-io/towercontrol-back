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
import com.disk91.capture.interfaces.sub.CaptureCalcLocation;
import com.disk91.capture.interfaces.sub.CaptureError;
import com.disk91.capture.interfaces.sub.CaptureMetaData;
import com.disk91.capture.interfaces.sub.CaptureRadioMetadata;
import com.disk91.capture.mdb.entities.CaptureEndpoint;
import com.disk91.capture.mdb.entities.Protocols;
import com.disk91.common.config.CommonConfig;
import com.disk91.common.interfaces.chirpstack.ChirpstackV4HeliumPayload;
import com.disk91.common.tools.*;
import com.disk91.common.tools.exceptions.ITHackerException;
import com.disk91.common.tools.exceptions.ITNotFoundException;
import com.disk91.common.tools.exceptions.ITParseException;
import com.disk91.common.tools.exceptions.ITRightException;
import com.disk91.users.mdb.entities.User;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Enumeration;

import static com.disk91.capture.interfaces.CaptureDataPivot.CaptureStatus.CAP_STATUS_SUCCESS;
import static com.disk91.capture.interfaces.sub.CaptureError.CaptureErrorLevel.CAP_ERROR_WARNING;

@Service
public class LoraWanHeliumChirpstackV4Driver extends AbstractProtocol {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    // Init once
    protected ObjectMapper mapper;

    // Add a driver seeed
    private static final String IV = "90f7adcf874990333cf159c1857fe539";

    @Autowired
    protected CommonConfig commonConfig;

    @PostConstruct
    private void initLoraWanHeliumChirpstackV4Driver() {
        log.info("[LoraWanHeliumChirpstackV4Driver] Initializing LoraWan Helium Chirpstack V4 Protocol Driver");
        mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.enable(JsonReadFeature.ALLOW_TRAILING_COMMA.mappedFeature());
        mapper.enable(JsonReadFeature.ALLOW_SINGLE_QUOTES.mappedFeature());
    }

    public CaptureIngestResponse toPivot(
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
        CaptureDataPivot p = new CaptureDataPivot();
        p.setRxTimestampMs(Now.NowUtcMs());
        p.setNwkErrors(new ArrayList<>());
        p.setErrors(new ArrayList<>());
        p.setRxUuid(this.getRxUUID());
        p.setRxCaptureRef(endpoint.getRef());
        p.setNwkStations(new ArrayList<>());

        // convert to Chirpstack Payload
        ChirpstackV4HeliumPayload payload;
        try {
            String json = new String(rawData);
            log.info(json); // @TODO remove after debug
            payload = mapper.readValue(json, ChirpstackV4HeliumPayload.class);
        } catch ( JsonProcessingException x) {
            // @TODO process the ERROR message as an exception

            // failed to parse
            log.info("[HeliumChirpstackV4Protocol] Conversion failed {}",x.getMessage()); // @TODO change for debug
            throw new ITParseException("capture-driver-helium-chirpstackv4-failed-to-parse-json");
        }

        // @TODO remove after debug
        log.info("[HeliumChirpstackV4Protocol] Recceived ChirpstackV4Helium payload: {}", payload.getDeduplicationId());
        log.info("[HeliumChirpstackV4Protocol] toPivot called - rawData size: {}", rawData != null ? rawData.length : 0);

        // Manage Payload, it will be Base64 encoded and ancrypted is required
        String toStoreData = payload.getData();
        if ( endpoint.isEncrypted() ) {
            toStoreData = "$"+EncryptionHelper.encrypt(payload.getData(), IV, commonConfig.getEncryptionKey());
        }
        p.setPayload(toStoreData);
        p.setNwkStatus(CaptureDataPivot.NetworkStatus.NWK_STATUS_SUCCESS);
        p.setCoredDump("");
        p.setIngestOwnerId(user.getLogin());
        p.setFromIp(Tools.getRemoteIp(request));

        // Copy the headers except Authorization
        p.setHeaders(new ArrayList<>());
        Enumeration<String> headerNames = request.getHeaderNames();
        if ( headerNames != null ) {
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                if ( headerName.compareToIgnoreCase("Authorization") != 0 ) {
                    String headerValue = request.getHeader(headerName);
                    CustomField cf = new CustomField();
                    cf.setName(headerName);
                    cf.setValue(headerValue);
                    p.getHeaders().add(cf);
                }
            }
        }

        CaptureMetaData meta = new CaptureMetaData();
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
        }
        meta.setNwkDeviceId(payload.getDeviceInfo().getDevEui());
        // @TODO : device ID
        // @TODO : session Counter
        meta.setFrameCounterUp(payload.getfCnt());
        meta.setFrameCounterDwn(payload.getfCntDown());
        meta.setFramePort(payload.getfPort());
        meta.setConfirmReq(payload.isConfirmed());
        meta.setConfirmed(false); // default to false
        meta.setDownlinkReq(payload.isConfirmed());
        meta.setDownlinkResp(false);

        CaptureRadioMetadata crmeta = new CaptureRadioMetadata();
        crmeta.setAddress(payload.getDevAddr());
        crmeta.setFrequency(0);
        crmeta.setCustomParams(new ArrayList<>());
        crmeta.setDataRate(""+payload.getDr());
        meta.setRadioMetadata(crmeta);

        // @TODO : complete metadata
        CaptureCalcLocation ccmeta = new CaptureCalcLocation();
        ccmeta.setAccuracy(0);
        ccmeta.setAltitude(0);
        ccmeta.setLatitude(0.0);
        ccmeta.setLatitude(0.0);
        ccmeta.setHexagonId("");
        meta.setCalculatedLocation(ccmeta);
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
