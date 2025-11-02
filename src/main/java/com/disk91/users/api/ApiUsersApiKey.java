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

package com.disk91.users.api;

import com.disk91.common.api.interfaces.ActionResult;
import com.disk91.common.tools.exceptions.ITParseException;
import com.disk91.common.tools.exceptions.ITRightException;
import com.disk91.groups.tools.GroupsHierarchySimplified;
import com.disk91.users.api.interfaces.UserApiTokenCreationBody;
import com.disk91.users.api.interfaces.UserApiTokenResponse;
import com.disk91.users.services.UserApiTokenService;
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

@Tag( name = "Users ApiKey management API", description = "Users module apikey management" )
@CrossOrigin
@RequestMapping(value = "/users/1.0/apikey")
@RestController
public class ApiUsersApiKey {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected UserApiTokenService userApiTokenService;


    /**
     * Create a new API Key for a given user
     *
     * This endpoints allows a user to create an apikey attached to his account. This apikey can reduce the user permissions
     * and have a longer validity than a standard users session token. It is also untouched by sign out operations and can be revoked
     * separately.
     *
     * This endpoint requires to have a completed signup process, the Apicreation authorization and must not be an API.
     */
    @Operation(
            summary = "Create a new API Key for a given user",
            description = "This endpoints allows a user to create an apikey attached to his account. This apikey can reduce the user permissions " +
                    "and have a longer validity than a standard users session token. It is also untouched by sign out operations and can be revoked " +
                    "separately. Users must have ROLE_LOGIN_COMPLETE, ROLE_USER_APIKEY and must not have ROLE_LOGIN_API",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Api Key created", content = @Content(schema = @Schema(implementation = ActionResult.class))),
                    @ApiResponse(responseCode = "400", description = "Failed to create apikey", content = @Content(schema = @Schema(implementation = ActionResult.class)))
            }
    )
    @RequestMapping(
            value = "/",
            produces = "application/json",
            method = RequestMethod.PUT
    )
    @PreAuthorize("hasRole('ROLE_LOGIN_COMPLETE') and hasRole('ROLE_USER_APIKEY') and !hasRole('ROLE_LOGIN_API')")
    // ----------------------------------------------------------------------
    public ResponseEntity<?> putUserApiKeyCreation(
            HttpServletRequest request,
            @RequestBody(required = true) UserApiTokenCreationBody body
    ) {
        try {
            userApiTokenService.createApiToken(
                    request,
                    request.getUserPrincipal().getName(),
                    request.getUserPrincipal().getName(),
                    body
            );
            return new ResponseEntity<>(ActionResult.CREATED("user-apikey-created"), HttpStatus.CREATED);
        } catch (ITRightException | ITParseException e) {
            return new ResponseEntity<>(ActionResult.BADREQUEST(e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Get API Key list for a given user
     *
     * This endpoint allows to list the apikey associated to that user.
     *
     * This endpoint requires to have a completed signup process and must not be an API.
     */
    @Operation(
            summary = "Get API Key list for a given user",
            description = "This endpoints allows to list the apikey associated to a user." +
                    " Users must have ROLE_LOGIN_COMPLETE and must not have ROLE_LOGIN_API",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User API keys",
                            content = @Content(array = @ArraySchema(schema = @Schema( implementation = UserApiTokenResponse.class)))),
                    @ApiResponse(responseCode = "400", description = "Failed to list apikey", content = @Content(schema = @Schema(implementation = ActionResult.class))),
                    @ApiResponse(responseCode = "403", description = "Not Authorized", content = @Content(schema = @Schema(implementation = ActionResult.class)))
            }
    )
    @RequestMapping(
            value = "/",
            produces = "application/json",
            method = RequestMethod.GET
    )
    @PreAuthorize("hasRole('ROLE_LOGIN_COMPLETE') and !hasRole('ROLE_LOGIN_API')")
    // ----------------------------------------------------------------------
    public ResponseEntity<?> getUserApiKeyList(
            HttpServletRequest request
    ) {
        try {
            List<UserApiTokenResponse> ret = userApiTokenService.getUserApiTokens(
                    request,
                    request.getUserPrincipal().getName(),
                    request.getUserPrincipal().getName()
            );
            return new ResponseEntity<>(ret, HttpStatus.OK);
        } catch (ITParseException e) {
            return new ResponseEntity<>(ActionResult.BADREQUEST(e.getMessage()), HttpStatus.BAD_REQUEST);
        } catch (ITRightException e) {
            return new ResponseEntity<>(ActionResult.FORBIDDEN(e.getMessage()), HttpStatus.FORBIDDEN);
        }
    }

    /**
     * Delete One of the API Key for a given user
     *
     * This endpoint allows to delete one apikey for user API key list
     *
     * This endpoint requires to have a completed signup process and must not be an API.
     */
    @Operation(
            summary = "Delete One of the API Key for a given user",
            description = "This endpoint allows to delete one apikey for user API key list." +
                    " Users must have ROLE_LOGIN_COMPLETE and must not have ROLE_LOGIN_API",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Api key deleted", content = @Content(schema = @Schema(implementation = ActionResult.class))),
                    @ApiResponse(responseCode = "400", description = "Failed to delete apikey", content = @Content(schema = @Schema(implementation = ActionResult.class))),
                    @ApiResponse(responseCode = "403", description = "Not Authorized", content = @Content(schema = @Schema(implementation = ActionResult.class)))
            }
    )
    @RequestMapping(
            value = "/{keyId}/",
            produces = "application/json",
            method = RequestMethod.DELETE
    )
    @PreAuthorize("hasRole('ROLE_LOGIN_COMPLETE') and !hasRole('ROLE_LOGIN_API')")
    // ----------------------------------------------------------------------
    public ResponseEntity<?> deleteUserApiKey(
            HttpServletRequest request,
            @Parameter(required = true, name = "keyId", description = "key identifier to delete")
            @PathVariable String keyId
    ) {
        try {
            userApiTokenService.deleteUserApiTokens(
                    request,
                    request.getUserPrincipal().getName(),
                    request.getUserPrincipal().getName(),
                    keyId
            );
            return new ResponseEntity<>(ActionResult.OK("user-apikey-delete-success"), HttpStatus.OK);
        } catch (ITParseException e) {
            return new ResponseEntity<>(ActionResult.BADREQUEST(e.getMessage()), HttpStatus.BAD_REQUEST);
        } catch (ITRightException e) {
            return new ResponseEntity<>(ActionResult.FORBIDDEN(e.getMessage()), HttpStatus.FORBIDDEN);
        }
    }


    /**
     * Get JWT's API Key for a given user api key
     *
     * This endpoint allows to get a JWT token for a given user API key.
     *
     * This endpoint requires to have a completed signup process and must not be an API.
     */
    @Operation(
            summary = "Get JWT's API Key for a given user api key",
            description = "This endpoint allows to get a JWT token for a given user API key." +
                    " Users must have ROLE_LOGIN_COMPLETE and must not have ROLE_LOGIN_API",
            responses = {
                    @ApiResponse(responseCode = "200", description = "JWT as ActionRequest via message field", content = @Content(schema = @Schema(implementation = ActionResult.class))),
                    @ApiResponse(responseCode = "400", description = "Failed to list apikey", content = @Content(schema = @Schema(implementation = ActionResult.class))),
                    @ApiResponse(responseCode = "403", description = "Not Authorized", content = @Content(schema = @Schema(implementation = ActionResult.class)))
            }
    )
    @RequestMapping(
            value = "/{keyId}/jwt/",
            produces = "application/json",
            method = RequestMethod.GET
    )
    @PreAuthorize("hasRole('ROLE_LOGIN_COMPLETE') and !hasRole('ROLE_LOGIN_API')")
    // ----------------------------------------------------------------------
    public ResponseEntity<?> getUserApiKeyJWT(
            HttpServletRequest request,
            @Parameter(required = true, name = "keyId", description = "api key identifier")
            @PathVariable String keyId
    ) {
        try {
            String ret = userApiTokenService.getJWTForApiKey(
                    request,
                    request.getUserPrincipal().getName(),
                    request.getUserPrincipal().getName(),
                    keyId
            );
            return new ResponseEntity<>(ActionResult.OK(ret), HttpStatus.OK);
        } catch (ITParseException e) {
            return new ResponseEntity<>(ActionResult.BADREQUEST(e.getMessage()), HttpStatus.BAD_REQUEST);
        } catch (ITRightException e) {
            return new ResponseEntity<>(ActionResult.FORBIDDEN(e.getMessage()), HttpStatus.FORBIDDEN);
        }
    }


}
