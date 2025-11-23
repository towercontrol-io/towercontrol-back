package com.disk91.capture.api;

import com.disk91.capture.services.CaptureIngestService;
import com.disk91.common.api.interfaces.ActionResult;
import com.disk91.common.tools.exceptions.ITNotFoundException;
import com.disk91.common.tools.exceptions.ITParseException;
import com.disk91.common.tools.exceptions.ITRightException;
import com.disk91.common.tools.exceptions.ITTooManyException;
import com.disk91.users.api.interfaces.UserAccountRegistrationBody;
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
import org.springframework.http.HttpStatus;
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
            }
    )
    @RequestMapping(
            value = "/{captureId}/",
            method = RequestMethod.POST
    )
    @PreAuthorize("hasRole('ROLE_BACKEND_CAPTURE')")
    // ----------------------------------------------------------------------
    public ResponseEntity<?> postIngestData(
            HttpServletRequest request,
            @Parameter(required = true, name = "captureId", description = "Capture endpoint unique identifier")
            @PathVariable("captureId") String captureId,
            @RequestBody(required = true) String body
    ) {
        try {
            return captureIngestService.ingestData(request, captureId, body);
        } catch ( ITParseException | ITNotFoundException | ITTooManyException x) {
            return new ResponseEntity<>(ActionResult.BADREQUEST("capture-ingest-parse-error"), HttpStatus.BAD_REQUEST);
        } catch ( ITRightException x) {
            return new ResponseEntity<>(ActionResult.FORBIDDEN("capture-ingest-forbidden"), HttpStatus.FORBIDDEN);
        }
    }

}
