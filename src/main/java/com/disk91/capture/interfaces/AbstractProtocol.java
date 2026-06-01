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
package com.disk91.capture.interfaces;

import com.disk91.capture.api.interfaces.CaptureResponseItf;
import com.disk91.capture.mdb.entities.CaptureEndpoint;
import com.disk91.capture.mdb.entities.ProtocolIds;
import com.disk91.capture.mdb.entities.Protocols;
import com.disk91.common.tools.exceptions.*;
import com.disk91.users.mdb.entities.User;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.UUID;

public abstract class AbstractProtocol {

    // Add a driver seed
    public static final String IV = "90f7adcf874990333cf159c1857fe539";


    /**
     * This function is used to create a unique RX UUID that will be used to trace the frame over the different
     * processing steps and splits.
     *
     * @return
     */
    protected UUID getRxUUID() {
        return UUID.randomUUID();
    }

    /**
     * Used when filling the header part of the pivot objet, to decide which headers to keep or not
     * Basically removing the network headers.
     * @param headerName
     * @return
     */
    protected boolean keepHeader(String headerName) {
        // List of headers to refuse (no interest to keep them)
        String [] headersReject = new String[] {
                "Authorization",
                "X-Forwarded-For",
                "X-Real-IP",
                "Content-Length",
                "Host",
                "Connection",
                "Accept",
                "User-Agent",
                "x-forwarded-proto",
                "accept-language",
                "accept-encoding",
                "accept-charset",
                "content-type",
        };
        for ( String h : headersReject ) {
            if ( h.equalsIgnoreCase(headerName) ) return false;
        }
        return true;
    }

    /**
     * Convert a data ingested from any endpoint into a pivot objet we can manipulate in a generic way later.
     * This method depends on the protocol (tuple of ingestion method, source, protocol, etc) and must be
     * instantiated in the corresponding class.
     * When an Exception is raised, the frame will not be stored. When you want to store a frame as an error frame,
     * do not report an exception, but use the status field and coreDump fields to store the raw frame in error when
     * expected.
     *
     * @param user User calling the endpoint
     * @param endpoint Corresponding endpoint
     * @param protocol Corresponding protocol
     * @param rawData Raw data received (byte array, data + metadata from the source)
     * @param request HTTP Request info
     * @return
     * @throws ITParseException When the data received does not match the expected format, frame won't be stored
     * @throws ITRightException When a right issue is detected, frame won't be stored
     * @throws ITHackerException When a hacking attempt is detected, frame won't be stored
     */
    public abstract CaptureIngestResponse toPivot(
            String jwtUser,                     // User in the Jwt token (may be an api key)
            User user,                          // User calling the endpoint
            CaptureEndpoint endpoint,           // Corresponding endpoint
            Protocols protocol,                 // Corresponding protocol
            byte[] rawData,                     // Raw data received
            HttpServletRequest request          // HTTP Request info
    ) throws
            ITParseException,                   // Will generate a trace
            ITRightException,                   // No trace
            ITHackerException;                  // Will generated a trace in audit (with caching to not overload)


    /**
     * When an injection cannot be handled because it cannot be executed, for example because
     * the service is shutting down, this method provides a generic response. It may also trigger
     * an interruption to fall back to a standard error format for the endpoint.
     *
     * @param ingestResponse
     * @return
     */

    public abstract CaptureResponseItf fallbackResponse(
            CaptureIngestResponse ingestResponse
    ) throws
            ITNotFoundException;

    /**
     * This function will be called to review the IDs associated with Endpoint and, therefore, with the protocol.
     * IDs are reviewed regularly to ensure that expirations and other parameters have not passed or changed.
     * The review frequency is not managed in the driver; it is managed at a higher level, regardless of the protocol.
     *
     * At the driver level, an `ITOverQuotaException` will be thrown to indicate that processing must stop on this
     * connector. This can also be an implementation choice for the function when there is no need to review IDs.
     * In that case, processing on this connector will stop after this exception is received.
     *
     * This exception may also be raised simply because the backend does not accept any further communication.
     * `checkId` will modify the provided ID and return it, or null when no change.
     * It is up to the upper layer to decide whether to persist it in order to apply the changes.
     *
     * @param _id to review
     * @return the reviewed ID, or null if no change
     * @throws ITOverQuotaException - to stop the processing for this connector
     */

    public abstract ProtocolIds checkId(
            CaptureEndpoint endpoint,           // Corresponding endpoint
            ProtocolIds _id
    ) throws
            ITOverQuotaException;


    /**
     * This function will allow a subscription to be created in the back end if it does not already exist. It will be
     * responsible for checking whether it is necessary to create the subscription based on the provided subscription
     * end date.
     * The endpoint may not be open for creating new subscriptions. In that case, a list of endpoints can also be
     * passed to the function, allowing it to find another endpoint where the subscription will be created. The
     * protocol ID is updated and returned by the function based on the elements that were obtained.
     *
     * @param endpoint - where ProtocolId is currently attached
     * @param endpoints - list of possible endpointIds where to create the new subscriptions if the first one is closed for new subscriptions
     * @param id - Id to associate, when null a new Id is created (depends on protocol, some will refuse, some will be default)
     * @param familyId - Identifier of the device family, some information may be required
     * @param subscriptionEnd - subscription end in ms, the subscription must be valid until this date, the function will decide whether to create a new subscription or not based on this date and the current subscription status
     * @return ProtocolIDs updated or created (no db save) / null when unchanged
     * @throws ITOverQuotaException In case the backend refuses the creation for a technical reason (retry later)
     * @throws ITTooManyException In case the backend refuses the creation due to a contractual limit
     * @throws ITParseException In case of a syntax error where retrial is not expected until fix
     */
    public abstract ProtocolIds subscribe(
            CaptureEndpoint endpoint,           // Current Endpoint
            List<CaptureEndpoint> endpoints,    // Possible other endpoints to search
            ProtocolIds id,                     // ID to subscribe - null to create a new one
            String familyId,                    // Device family Id (can be null)
            Long subscriptionEnd                // Subscription end in ms, the subscription must be valid until this date
    ) throws
            ITOverQuotaException,               // In case the backend refuses the creation for a technical reason (retry later)
            ITTooManyException,                 // In case the backend refuses the creation due to a contractual limit
            ITParseException;                   // In case of a syntax error where retrial is not expected until fix

    /**
     * This function will make it possible to terminate an active subscription by releasing it on the back end. This
     * ID will be updated, and according to the protocol, things will be implemented. The ID may be revoked or may be
     * placed in a state so that it does not renew automatically.
     *
     * @param endpoint - Endpoint currently associated with the ID
     * @param id - Id to be unsubscribed
     * @return ProtocolIDs updated (no db save)
     * @throws ITOverQuotaException In case the backend refuses the creation for a technical reason (retry later)
     * @throws ITParseException In case of a syntax error where retrial is not expected until fix
     */
    public abstract ProtocolIds unsubscribe(
            CaptureEndpoint endpoint,           // Corresponding endpoint
            ProtocolIds id                      // ID to subscribe
    ) throws
            ITOverQuotaException,               // In case the backend refuses the creation for a technical reason (retry later)
            ITParseException;                   // In case of a syntax error where retrial is not expected until fix




}
