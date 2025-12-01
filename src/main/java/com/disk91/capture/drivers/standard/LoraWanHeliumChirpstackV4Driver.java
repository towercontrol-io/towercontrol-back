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
import com.disk91.capture.mdb.entities.CaptureEndpoint;
import com.disk91.capture.mdb.entities.Protocols;
import com.disk91.common.interfaces.chirpstack.ChirpstackV4HeliumPayload;
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
import org.springframework.stereotype.Service;

import static com.disk91.capture.interfaces.CaptureDataPivot.CaptureStatus.CAP_STATUS_SUCCESS;

@Service
public class LoraWanHeliumChirpstackV4Driver extends AbstractProtocol {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    // Init once
    protected ObjectMapper mapper;

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
        // convert to Chirpstack Payload
        ChirpstackV4HeliumPayload payload;
        try {
            String json = new String(rawData);
            payload = mapper.readValue(json, ChirpstackV4HeliumPayload.class);
        } catch ( JsonProcessingException x) {
            // failed to parse
            log.info("[HeliumChirpstackV4Protocol] Conversion failed {}",x.getMessage()); // @TODO change for debug
            throw new ITParseException("capture-driver-helium-chirpstackv4-failed-to-parse-json");
        }

        log.info("[HeliumChirpstackV4Protocol] Recceived ChirpstackV4Helium payload: {}", payload.getDeduplicationId());
        CaptureDataPivot p = new CaptureDataPivot();
        log.info("[HeliumChirpstackV4Protocol] toPivot called - rawData size: {}", rawData != null ? rawData.length : 0);

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
