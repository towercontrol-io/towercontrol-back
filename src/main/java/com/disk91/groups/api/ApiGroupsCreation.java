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
package com.disk91.groups.api;

import com.disk91.common.api.interfaces.ActionResult;
import com.disk91.common.tools.exceptions.ITNotFoundException;
import com.disk91.common.tools.exceptions.ITParseException;
import com.disk91.common.tools.exceptions.ITRightException;
import com.disk91.groups.api.interfaces.GroupCreationBody;
import com.disk91.groups.services.GroupsChangeServices;
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
import org.springframework.web.bind.annotation.*;

@Tag( name = "Users module groups API", description = "Users module groups API" )
@CrossOrigin
@RequestMapping(value = "/groups/1.0/creation")
@RestController
public class ApiGroupsCreation {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected GroupsChangeServices groupsChangeServices;

    /**
     * Group creation API for creation of subgroups
     *
     * A user with a Local Admin Role has the right to create a subgroup to organize their devices.
     * This subgroup must be attached to a group they own, their default group, or a third-party group
     * on which they have an ACL granting them the Group Local Admin right. The group is added to the hierarchy.
     * Root groups can only be created by Group Admins and a different API is used for this.
     *
     * Private endpoint accessible to user with an active session and Local Admin right on a group.
     */
    @Operation(
            summary = "Group creation API for creation of subgroups",
            description = "A user with a Local Admin Role has the right to create a subgroup to organize their devices. " +
                    "This subgroup must be attached to a group they own, their default group, or a third-party group " +
                    "on which they have an ACL granting them the Group Local Admin right. The group is added to the hierarchy. " +
                    "Root groups can only be created by Group Admins and a different API is used for this. " +
                    "Private endpoint accessible to user with an active session and Local Admin right on a group.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Group created", content = @Content(schema = @Schema(implementation = ActionResult.class))),
                    @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ActionResult.class))),
                    @ApiResponse(responseCode = "400", description = "Parse error in processing group creation", content = @Content(schema = @Schema(implementation = ActionResult.class))),
            }
    )
    @RequestMapping(
            value = "",
            produces = "application/json",
            method = RequestMethod.POST
    )
    @PreAuthorize("hasAnyRole('ROLE_LOGIN_COMPLETE')")
    // ----------------------------------------------------------------------
    public ResponseEntity<?> createSubGroup (
            HttpServletRequest request,
            @RequestBody(required = true) GroupCreationBody body
    ) {
        try {
            groupsChangeServices.createSubGroup(request.getUserPrincipal().getName(), body);
            return new ResponseEntity<>(ActionResult.CREATED("groups-group-creation-success"), HttpStatus.CREATED);

            //@TODO - manage the exception properly
        } catch (ITRightException | ITParseException | ITNotFoundException x) {
            return new ResponseEntity<>(ActionResult.FORBIDDEN("groups-group-creation-refused"), HttpStatus.FORBIDDEN);
        }
    }

}
