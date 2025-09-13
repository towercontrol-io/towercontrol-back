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
import com.disk91.common.tools.exceptions.ITRightException;
import com.disk91.custom.quakecore.api.interfaces.QcDeviceOwnedBody;
import com.disk91.users.api.interfaces.UserAccessibleRolesResponse;
import com.disk91.users.api.interfaces.UserConfigResponse;
import com.disk91.users.services.UserGroupRolesService;
import com.disk91.users.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag( name = "Users module role API", description = "Users module role API" )
@CrossOrigin
@RequestMapping(value = "/users/1.0/roles")
@RestController
public class ApiUsersRoles {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected UserGroupRolesService userGroupRolesService;

    /**
     * User module list the role accessible for a given user endpoint
     *
     * When a user wants to create an API or create a new user, he can only affect the
     * Roles and group he have access on. So we need to make sure the role he sees in
     * the front-end corresponds to really available roles. This gives the list of
     * accessible roles.
     *
     * Private endpoint accessible to user with an active session
     */
    @Operation(
            summary = "Get user accessible roles",
            description = "When a user wants to create an API or create a new user, he can only affect the " +
                    "Roles and group he have access on. So we need to make sure the role he sees in " +
                    "the front-end corresponds to really available roles. This gives the list of " +
                    "accessible roles.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User accessible roles",
                            content = @Content(array = @ArraySchema(schema = @Schema( implementation = UserAccessibleRolesResponse.class)))),
                    @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ActionResult.class))),
            }
    )
    @RequestMapping(
            value = "",
            produces = "application/json",
            method = RequestMethod.GET
    )
    @PreAuthorize("hasAnyRole('ROLE_LOGIN_COMPLETE')")
    // ----------------------------------------------------------------------
    public ResponseEntity<?> getUserAccessibleRoles(
            HttpServletRequest request
    ) {
        try {
            List<UserAccessibleRolesResponse> r = userGroupRolesService.getAvailableRoles(request.getUserPrincipal().getName());
            return new ResponseEntity<>(r, HttpStatus.OK);
        } catch ( ITRightException x) {
            return new ResponseEntity<>(ActionResult.FORBIDDEN(x.getMessage()), HttpStatus.FORBIDDEN);
        }
    }

}
