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
import com.disk91.capture.mdb.entities.Protocols;
import com.disk91.common.tools.RandomString;
import com.disk91.common.tools.exceptions.*;
import com.disk91.users.mdb.entities.User;
import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

public abstract class AbstractProtocol {

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

}
