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

import com.disk91.common.config.CommonConfig;
import com.disk91.common.interfaces.google.GoogleGeolocationInput;
import com.disk91.common.interfaces.google.GoogleGeolocationOutput;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Component
public class GoogleWiFiGeolocationService {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected CommonConfig commonConfig;

    public GoogleGeolocationOutput position(String mac1, String mac2 ) {
        log.debug("[common] Google geolocation Search for mac {} and mac {}",mac1,mac2);
        ArrayList<String> macs = new ArrayList<>();
        if ( mac1 != null ) macs.add(mac1);
        if ( mac2 != null ) macs.add(mac2);
        return this.position(macs);
    }


    public GoogleGeolocationOutput position(List<String> macs ) {
        log.info("[common] Google geolocation search for {} macs ",macs.size());
        if (macs.size() < 2) return null;
        GoogleGeolocationInput g = new GoogleGeolocationInput();
        g.initWifi();
        for ( String mac : macs ) {
            log.debug("[common] Google geolocation add mac : {}", mac);
            g.addMac(mac);
        }
        ObjectMapper mapper = new ObjectMapper();
        String json = null;
        try {
            json = mapper.writeValueAsString(g);
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<GoogleGeolocationOutput> response =
                    restTemplate.exchange(
                            "https://www.googleapis.com/geolocation/v1/geolocate?key="+commonConfig.getGoogleApiKey(),
                            HttpMethod.POST,
                            new HttpEntity<>(json,headers),
                            GoogleGeolocationOutput.class);
            GoogleGeolocationOutput resp = response.getBody();
            String resps=mapper.writeValueAsString(resp);
            log.debug("[common] return from google geolocation : "+resps);
            return resp;
        } catch ( JsonProcessingException ex ) {
            log.error("[common] Impossible to parse the given object : " + ex.getMessage());
            return null;
        } catch (HttpStatusCodeException e) {
            int statusCode = e.getStatusCode().value();
            log.info("[common] Status code from Google : {}", statusCode);
            if ( statusCode == 404 ) {
                log.info("[common] Position not found by google");
                log.info("with {}",json);
            } else {
                log.info("[common] Error in google call");
                log.info(e.getMessage());
                log.info("with {}",json);
            }
            return null;
        } catch ( Exception e ) {
            log.info("[common] Exception in google call");
            log.info(e.getMessage());
            log.info("with {}",json);
            return null;
        }
    }

}
