/*
 * Copyright (c) - Paul Pinault (aka disk91) - 2024.
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
package com.disk91.capture.services;

import com.disk91.capture.mdb.entities.CaptureEndpoint;
import com.disk91.common.api.interfaces.ActionResult;
import com.disk91.common.tools.exceptions.ITNotFoundException;
import com.disk91.common.tools.exceptions.ITParseException;
import com.disk91.common.tools.exceptions.ITRightException;
import com.disk91.common.tools.exceptions.ITTooManyException;
import com.disk91.users.mdb.entities.User;
import com.disk91.users.services.UserCommon;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class CaptureIngestService {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected CaptureEndpointCache captureEndpointCache;

    @Autowired
    protected UserCommon userCommon;

    /**
     * This generic endpoint allows ingesting data from any source; data are received as a string which in most cases
     * will be JSON and will be interpreted by the module adapted to the source. The parameter captureId indicates
     * which module should be used for the ingestion; at the same time we will verify that the user and the capture
     * module match and belong to the same person. This can be a technical (API) account or a regular user account
     * with an API key.
     *
     * @param req
     * @param body
     * @param captureId
     * @return
     * @throws ITParseException
     * @throws ITTooManyException
     * @throws ITNotFoundException
     * @throws ITRightException
     */
    public ResponseEntity<?> ingestData(
            HttpServletRequest req,
            String body,
            String captureId
    ) throws ITParseException, ITTooManyException, ITNotFoundException, ITRightException {

        try {
            // Get the endpoint information
            CaptureEndpoint e = captureEndpointCache.getCaptureEndpoint(captureId);
            // Check the ownership
            User u = userCommon.getUser(req.getUserPrincipal().getName());
            if ( e.getOwner().compareTo(u.getLogin()) != 0) {
                log.debug("[capture] Ingest data failed, right error for captureId {} and user {}", captureId, u.getLogin());
                throw new ITRightException("capture-ingest-right-error");
            }

            // Right match, we can proceed
            // later we will check the device right with that user too
            // @TODO




        } catch (ITNotFoundException e) {
            log.debug("[capture] Ingest data failed, unknown captureId {}", captureId);
            throw new ITNotFoundException("capture-endpoint-unknown");
        }
        return new ResponseEntity<>(ActionResult.OK("capture-ingest-ok"), HttpStatus.OK);
    }
}
