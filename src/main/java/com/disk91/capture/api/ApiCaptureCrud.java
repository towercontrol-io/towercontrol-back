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

import com.disk91.capture.api.interfaces.CaptureEndpointCreationBody;
import com.disk91.capture.api.interfaces.CaptureEndpointResponseItf;
import com.disk91.capture.api.interfaces.CaptureProtocolResponseItf;
import com.disk91.capture.services.CaptureEndpointService;
import com.disk91.common.api.interfaces.ActionResult;
import com.disk91.common.tools.exceptions.ITNotFoundException;
import com.disk91.common.tools.exceptions.ITParseException;
import com.disk91.common.tools.exceptions.ITRightException;
import com.disk91.users.api.interfaces.UserApiTokenCreationBody;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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

import java.util.List;

@Tag( name = "Capture endpoints management", description = "CRUD operation on capture endpoints" )
@CrossOrigin
@RequestMapping(value = "/capture/1.0/endpoint")
@RestController
public class ApiCaptureCrud {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected CaptureEndpointService captureEndpointService;

    /**
     * List the existing endpoints created by this user, or all the users when you are platform admin.
     * @param request
     * @return
     */
    @Operation(
            summary = "List user endpoints",
            description = "This endpoint allows to get the list of capture endpoint created by the user. If the user is platform admin, all the endpoints are returned." +
                    "No specific role required to list the endpoint but creation requires ROLE_BACKEND_CAPTURE.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Protocols definitions",
                            content = @Content(array = @ArraySchema(schema = @Schema( implementation = CaptureEndpointResponseItf.class)))),
                    @ApiResponse(responseCode = "400", description = "Failed to list endpoints", content = @Content(schema = @Schema(implementation = ActionResult.class)))
            }
    )
    @RequestMapping(
            value = "",
            produces = "application/json",
            method = RequestMethod.GET
    )
    @PreAuthorize("hasRole('ROLE_LOGIN_COMPLETE')")
    // ----------------------------------------------------------------------
    public ResponseEntity<?> getCaptureEndpointList(
            HttpServletRequest request
    ) {
        try {
            List<CaptureEndpointResponseItf> endpoints =
                    captureEndpointService.listEndpoints(
                            request.getUserPrincipal().getName()
                    );
            return new ResponseEntity<>(endpoints, HttpStatus.OK);
        } catch (ITRightException e) {
            return new ResponseEntity<>(ActionResult.BADREQUEST(e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Delete One of the Capture Endpoint for a given user
     *
     * This endpoint allows to delete one capture endpoint for user API key list
     *
     * This endpoint requires to have a completed signup process and must not be an API.
     */
    @Operation(
            summary = "Delete One of the Capture Endpoint for a given user",
            description = "This endpoint allows to delete one capture endpoint for user. Endpoint Ref used as a key" +
                    " Users must have ROLE_LOGIN_COMPLETE and own the endpoint",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Capture endpoint deleted", content = @Content(schema = @Schema(implementation = ActionResult.class))),
                    @ApiResponse(responseCode = "400", description = "Failed to delete endpoint", content = @Content(schema = @Schema(implementation = ActionResult.class))),
                    @ApiResponse(responseCode = "403", description = "Not Authorized", content = @Content(schema = @Schema(implementation = ActionResult.class)))
            }
    )
    @RequestMapping(
            value = "/{keyId}/",
            produces = "application/json",
            method = RequestMethod.DELETE
    )
    @PreAuthorize("hasRole('ROLE_LOGIN_COMPLETE')")
    // ----------------------------------------------------------------------
    public ResponseEntity<?> deleteCaptureEndpoint(
            HttpServletRequest request,
            @Parameter(required = true, name = "keyId", description = "key identifier to delete")
            @PathVariable String keyId
    ) {
        try {
            captureEndpointService.deleteCaptureEndpoint(
                    request,
                    request.getUserPrincipal().getName(),
                    keyId
            );
            return new ResponseEntity<>(ActionResult.OK("capture-endpoint-delete-success"), HttpStatus.OK);
        } catch (ITParseException e) {
            return new ResponseEntity<>(ActionResult.BADREQUEST(e.getMessage()), HttpStatus.BAD_REQUEST);
        } catch (ITRightException e) {
            return new ResponseEntity<>(ActionResult.FORBIDDEN(e.getMessage()), HttpStatus.FORBIDDEN);
        }
    }



    /**
     * Create a capture endpoint
     *
     * This endpoint allows a user to create a capture endpoint attached to user account. When apikey is used, the endpoint
     * is associated to the real user.
     *
     * This endpoint requires to have a completed signup process and the right to create endpoint ( ROLE_BACKEND_CAPTURE )
     *
     */
    @Operation(
            summary = "Create a new capture endpoint ",
            description = "This endpoint allows a user with ROLE_BACKEND_CAPTURE to create a capture endpoint to collect data." +
                    " Users must have ROLE_LOGIN_COMPLETE, ROLE_USER_APIKEY, when API created, the role with be associated to the " +
                    " real user behind the API key.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Capture endpoint created", content = @Content(schema = @Schema(implementation = ActionResult.class))),
                    @ApiResponse(responseCode = "400", description = "Failed to create capture endpoint", content = @Content(schema = @Schema(implementation = ActionResult.class))),
                    @ApiResponse(responseCode = "403", description = "Failed to create capture endpoint", content = @Content(schema = @Schema(implementation = ActionResult.class)))
            }
    )
    @RequestMapping(
            value = "",
            produces = "application/json",
            method = RequestMethod.POST
    )
    @PreAuthorize("hasRole('ROLE_LOGIN_COMPLETE') and hasRole('ROLE_BACKEND_CAPTURE')")
    // ----------------------------------------------------------------------
    public ResponseEntity<?> putUserApiKeyCreation(
            HttpServletRequest request,
            @RequestBody(required = true) CaptureEndpointCreationBody body
    ) {
        try {
            captureEndpointService.createCaptureEndpoint(
                    request,
                    body
            );
            return new ResponseEntity<>(ActionResult.CREATED("capture-endpoint-created"), HttpStatus.CREATED);
        } catch (ITNotFoundException | ITParseException e) {
            return new ResponseEntity<>(ActionResult.BADREQUEST(e.getMessage()), HttpStatus.BAD_REQUEST);
        } catch (ITRightException e){
            return new ResponseEntity<>(ActionResult.FORBIDDEN(e.getMessage()), HttpStatus.FORBIDDEN);
        }
    }


}
