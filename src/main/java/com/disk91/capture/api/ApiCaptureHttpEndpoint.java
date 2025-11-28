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
package com.disk91.capture.api;

import com.disk91.capture.api.interfaces.CaptureResponseItf;
import com.disk91.capture.services.CaptureIngestService;
import com.disk91.common.api.interfaces.ActionResult;
import com.disk91.common.tools.exceptions.ITNotFoundException;
import com.disk91.common.tools.exceptions.ITParseException;
import com.disk91.common.tools.exceptions.ITRightException;
import com.disk91.common.tools.exceptions.ITTooManyException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag( name = "Http(s) Endpoint for data integration", description = "Endpoint for reception of data from an external platform, network server with HTTP/HTTPs" )
@CrossOrigin
@RequestMapping(value = "/capture/1.0/ingest")
@RestController
public class ApiCaptureHttpEndpoint {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected CaptureIngestService captureIngestService;

    /**
     * Http(s) endpoint to receive data from external platforms
     *
     * This endpoint is accepting any connection with a valid Token and the ROLE_BACKEND_CAPTURE role. This is the
     * requirement to ensure general security. The function will manage the data ingestion and the transformation
     * according to the capture endpoint information. This capture endpoint reference is a parameter to add in the
     * url call.
     *
     */
    @Operation(
            summary = "Http(s) endpoint to receive data from external platforms",
            description = "This endpoint is accepting any connection with a valid Token and the ROLE_BACKEND_CAPTURE role. This is the " +
                    "requirement to ensure general security. The function will manage the data ingestion and the transformation " +
                    "according to the capture endpoint information. This capture endpoint reference is a parameter to add in the " +
                    "url call. The response to the call depends on the underlying layer and what the external platform is expecting. ",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Data accepted", content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode = "204", description = "Data accepted", content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode = "400", description = "Parsing error", content = @Content(schema = @Schema(implementation = ActionResult.class))),
                    @ApiResponse(responseCode = "403", description = "Right error", content = @Content(schema = @Schema(implementation = ActionResult.class))),
                    @ApiResponse(responseCode = "503", description = "Service Unavailable", content = @Content(schema = @Schema(implementation = ActionResult.class)))
            }
    )
    @RequestMapping(
            value = "/{captureId}/",
            method = RequestMethod.POST,
            produces = MediaType.ALL_VALUE,
            consumes = MediaType.ALL_VALUE
    )
    @PreAuthorize("hasRole('ROLE_BACKEND_CAPTURE')")
    // ----------------------------------------------------------------------
    public ResponseEntity<?> postIngestData(
            HttpServletRequest request,
            @Parameter(required = true, name = "captureId", description = "Capture endpoint unique identifier")
            @PathVariable("captureId") String captureId,
            @RequestBody(required = true) byte[] body
    ) {
        try {
            CaptureResponseItf r = captureIngestService.ingestData(request, body, captureId);
            if ( r.getStatus() == HttpStatus.NO_CONTENT ) {
                return new ResponseEntity<>(null, r.getHeaders(), r.getStatus());
            }
            return new ResponseEntity<>(r.getData(), r.getHeaders(), r.getStatus());

        } catch ( ITParseException | ITNotFoundException x) {
            HttpHeaders h = new HttpHeaders();
            h.add("Content-Type", "application/json");
            return new ResponseEntity<>(ActionResult.BADREQUEST("capture-ingest-parse-error"), h, HttpStatus.BAD_REQUEST);
        } catch ( ITTooManyException x ) {
            HttpHeaders h = new HttpHeaders();
            h.add("Content-Type", "application/json");
            return new ResponseEntity<>(ActionResult.PARTIAL(x.getMessage()), h, HttpStatus.SERVICE_UNAVAILABLE);
        } catch ( ITRightException x) {
            HttpHeaders h = new HttpHeaders();
            h.add("Content-Type", "application/json");
            return new ResponseEntity<>(ActionResult.FORBIDDEN("capture-ingest-forbidden"), h, HttpStatus.FORBIDDEN);
        }
    }

}
