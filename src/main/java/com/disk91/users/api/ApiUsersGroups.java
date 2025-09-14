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
import com.disk91.users.api.interfaces.UserAccessibleRolesResponse;
import com.disk91.users.services.UserGroupRolesService;
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

@Tag( name = "Users module groups API", description = "Users module groups API" )
@CrossOrigin
@RequestMapping(value = "/users/1.0/groups")
@RestController
public class ApiUsersGroups {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected UserGroupRolesService userGroupRolesService;

    /**
     * User module list the groups accessible for a given user endpoint
     *
     * To display the groups a user can access, in the form of a hierarchy,
     * this API provides all the information needed to represent them. This
     * API does not provide information about the rights associated with each group but traverses the entire hierarchy.
     *
     * Private endpoint accessible to user with an active session
     */
    @Operation(
            summary = "Get user accessible groups",
            description = "To display the groups a user can access, in the form of a hierarchy, " +
                    "this API provides all the information needed to represent them. This " +
                    "API does not provide information about the rights associated with each group but traverses the entire hierarchy. " +
                    "This endpoint is accessible to registered users.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User accessible groups",
                            content = @Content(array = @ArraySchema(schema = @Schema( implementation = GroupsHierarchySimplified.class)))),
                    @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ActionResult.class))),
                    @ApiResponse(responseCode = "400", description = "Parse error in processing groups", content = @Content(schema = @Schema(implementation = ActionResult.class))),
            }
    )
    @RequestMapping(
            value = "",
            produces = "application/json",
            method = RequestMethod.GET
    )
    @PreAuthorize("hasAnyRole('ROLE_LOGIN_COMPLETE')")
    // ----------------------------------------------------------------------
    public ResponseEntity<?> getUserAccessibleGroups(
            HttpServletRequest request
    ) {
        try {
            List<GroupsHierarchySimplified> r = userGroupRolesService.getAvailableGroups(request.getUserPrincipal().getName());
            return new ResponseEntity<>(r, HttpStatus.OK);
        } catch ( ITParseException x) {
            return new ResponseEntity<>(ActionResult.FORBIDDEN(x.getMessage()), HttpStatus.FORBIDDEN);
        }
    }

}
