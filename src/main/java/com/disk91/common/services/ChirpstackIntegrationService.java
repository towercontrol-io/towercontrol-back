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
package com.disk91.common.services;

import com.disk91.common.interfaces.chirpstack.ApiCreateDeviceKeyRequest;
import com.disk91.common.interfaces.chirpstack.ApiCreateDeviceRequest;
import com.disk91.common.interfaces.chirpstack.ApiErrorResponse;
import com.disk91.common.tools.HexCodingTools;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

@Component
public class ChirpstackIntegrationService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * Create a device in chirpstack
     * @param name
     * @param description
     * @param devEui
     * @param joinEui
     * @param appKey
     * @param appId
     * @param profile
     * @param skipFCntCheck
     * @param isDisabled
     * @return
     */
    public boolean createChirpstackOTAADevice(
            String name,
            String description,
            String devEui,
            String joinEui,
            String appKey,
            String appId,
            String profile,
            boolean skipFCntCheck,
            boolean isDisabled,
            String apikey,
            String chirpstackRestApiBase
    ) {

        // Create the device
        ApiCreateDeviceRequest request = new ApiCreateDeviceRequest();
        request.setDevice( new ApiCreateDeviceRequest.Device() );
        request.getDevice().setName(name);
        request.getDevice().setDescription(description);
        request.getDevice().setDevEui(devEui);
        request.getDevice().setJoinEui(joinEui);
        request.getDevice().setApplicationId(appId);
        request.getDevice().setDeviceProfileId(profile);
        request.getDevice().setSkipFCntCheck(skipFCntCheck);
        request.getDevice().setDisabled(isDisabled);


        RestTemplate restTemplate = new RestTemplate();
        String json = null;
        String url = null;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.USER_AGENT,"disk91_itc/1.0");
            headers.add(HttpHeaders.CONTENT_TYPE, "application/json");
            headers.add(HttpHeaders.AUTHORIZATION, "Bearer "+apikey);
            url=chirpstackRestApiBase+"/devices";
            ObjectMapper mapper = new ObjectMapper();
            json = mapper.writeValueAsString(request);
            ResponseEntity<ApiErrorResponse> responseEntity =
                    restTemplate.exchange(
                            url,
                            HttpMethod.POST,
                            new HttpEntity<>(json,headers),
                            ApiErrorResponse.class
                    );
            if ( responseEntity.getStatusCode().is2xxSuccessful() ) {
                log.debug("[common] Device {} created", devEui);
                // Add the OTAA key
                ApiCreateDeviceKeyRequest keyRequest = new ApiCreateDeviceKeyRequest();
                keyRequest.setDeviceKeys(new ApiCreateDeviceKeyRequest.DeviceKeys());
                keyRequest.getDeviceKeys().setAppKey(appKey);
                keyRequest.getDeviceKeys().setNwkKey(appKey);
                try {
                    url = chirpstackRestApiBase + "/devices/" + devEui + "/keys";
                    json = mapper.writeValueAsString(keyRequest);
                    ResponseEntity<ApiErrorResponse> responseEntityKey =
                            restTemplate.exchange(
                                    url,
                                    HttpMethod.POST,
                                    new HttpEntity<>(json, headers),
                                    ApiErrorResponse.class
                            );
                    return responseEntityKey.getStatusCode().is2xxSuccessful();
                } catch (HttpClientErrorException e) {
                    log.warn("[common] Device key creation client error - {}", e.getMessage());
                    log.warn("[common] called with - {}", json);
                    return false;
                } catch (HttpServerErrorException e) {
                    log.warn("[common] Device key creation server error - {}", e.getMessage());
                    return false;
                } catch (Exception x ) {
                     log.warn("[common] Device key creation error - {}", x.getMessage());
                     return false;
                }
            } else {
                log.warn("[common] Device creation error - {}", responseEntity.getStatusCode().value());
                return false;
            }
        } catch (HttpClientErrorException e) {
            log.warn("[common] Device creation client error - {}", e.getMessage());
            log.warn("[common] called with - {}", json);
            return false;
        } catch (HttpServerErrorException e) {
            log.warn("[common] Device creation server error - {}", e.getMessage());
            return false;
        } catch (Exception x ) {
            log.warn("[common] Device creation error - {}", x.getMessage());
            return false;
        }

    }



}
