/*
 * Copyright (c) 2018.
 *
 *  This is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  this software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  -------------------------------------------------------------------------------
 *  Author : Paul Pinault aka disk91
 *  See https://www.disk91.com
 *
 *  Commercial license of this software can be obtained contacting disk91.com or ingeniousthings.fr
 *  -------------------------------------------------------------------------------
 *
 */

package com.disk91.capture.drivers.standard.sigfox.apiv2.services;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

public class ITSigfoxConnection<S,T> {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    //private static final String SIGFOX_BACKEND = "https://backend.sigfox.com";

    private static final String[] authorizedHeader = {
            "user-agent", "accept", "origin", "referer"
    };

    private final String sigfoxBasicString;
    private final String apiBackend;

    /**
     * Create a Connection to Sigfox API with a given Login / Password combination
     * @param apiLogin
     * @param apiPassword
     */
    public ITSigfoxConnection(String apiBackend, String apiLogin, String apiPassword) {
        String plainCreds = apiLogin+":"+apiPassword;
        byte[] base64CredsBytes = java.util.Base64.getEncoder().encode(plainCreds.getBytes(StandardCharsets.US_ASCII));
        this.sigfoxBasicString = "Basic " + new String(base64CredsBytes);
        this.apiBackend = apiBackend;
    }


    /**
     * Compose a HttpHeader list from a HttpRequest content
     * @param request
     * @return
     */
    public static HttpHeaders getHeadersFromHttpRequest(HttpServletRequest request) {

        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            for ( String s : authorizedHeader ) {
                if ( s.compareToIgnoreCase(headerName) == 0) {
                    headers.add(headerName, headerValue);
                    break;
                }
            }
        }
        return headers;
    }

    public static HttpMethod getHttpMethodFromString(String method) {
        return switch (method) {
            case "GET" -> HttpMethod.GET;
            case "POST" -> HttpMethod.POST;
            case "DELETE" -> HttpMethod.DELETE;
            case "PUT" -> HttpMethod.PUT;
            default -> HttpMethod.GET;
        };
    }

    /**
     * Compose a request to Sigfox backend.
     * @param apiPath
     * @param queryString
     * @param headers
     * @param body
     * @return
     * @throws ITSigfoxConnectionException
     */
    public T execute(
            String httpMethod,
            String apiPath,
            String queryString,
            HttpHeaders headers,
            S body,
            Class<T> typeRetrunedClass
    )  throws ITSigfoxConnectionException {

        T response = null;

        String url = apiBackend+apiPath;
        if ( queryString != null && !queryString.isEmpty())
            url +='?'+queryString;

        if ( headers == null ) headers = new HttpHeaders();
        headers.add("Authorization", sigfoxBasicString);

        HttpEntity<String> he;
        if (    httpMethod.compareToIgnoreCase("GET")  == 0
                || httpMethod.compareToIgnoreCase("DELETE")  == 0
                ) {
            headers.setContentType(MediaType.TEXT_HTML);
            he = new HttpEntity<String>(headers);
        } else {
            headers.setContentType(MediaType.APPLICATION_JSON);
            try {
                ObjectMapper mapper = new ObjectMapper();
                mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
                he = new HttpEntity<String>(mapper.writeValueAsString(body), headers);
                log.info("body : "+mapper.writeValueAsString(body));

            } catch (JsonProcessingException e) {
                throw new ITSigfoxConnectionException(HttpStatus.BAD_REQUEST,"Body format is invalid - can't be serialized");
            }
        }
        RestTemplate restTemplate = new RestTemplate();

        try {
            ResponseEntity<String> responseEntity =
                    restTemplate.exchange(
                            url,
                            getHttpMethodFromString(httpMethod),
                            he,
                            String.class
                    );
            if (    responseEntity.getStatusCode() != HttpStatus.OK
                    && responseEntity.getStatusCode() != HttpStatus.NO_CONTENT
                    && responseEntity.getStatusCode() != HttpStatus.CREATED
                    ) {
                log.info("Received an error code from Sigfox's backend : " + responseEntity.getStatusCode());
                throw new ITSigfoxConnectionException(
                        HttpStatus.valueOf(responseEntity.getStatusCode().value()),
                        responseEntity.getBody()
                );
            } else {
//  log.info(responseEntity.getBody());
                ObjectMapper mapper = new ObjectMapper();
                try {
                    if ( responseEntity.getBody() != null ) {
                        response = mapper.readValue(responseEntity.getBody(), typeRetrunedClass);
                    } else response = null;
                    log.info("Sigfox Response : "+responseEntity.getStatusCode());
                } catch (IOException e) {
                    log.error(responseEntity.getBody());
                    log.error("Impossible to deserialize Sigfox's backend response");
                    e.printStackTrace();
                    response = null;
                }
            }
        } catch (HttpClientErrorException | HttpServerErrorException e ) {
            log.error("Sigfox's backend communication exception :"+e.getStatusCode()+"["+e.getMessage()+"]");
            throw new ITSigfoxConnectionException(
                    HttpStatus.valueOf(e.getStatusCode().value()),
                    e.getResponseBodyAsString()
            );
        }
        return response;
    }

}
