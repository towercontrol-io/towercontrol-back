package com.disk91.users.api;

import com.disk91.common.api.interfaces.ActionResult;
import com.disk91.common.tools.exceptions.ITNotFoundException;
import com.disk91.users.api.interfaces.UserListElementResponse;
import com.disk91.users.services.UserAdminService;
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

import java.util.ArrayList;
import java.util.List;

@Tag( name = "Users module administration API", description = "Users module administration API" )
@CrossOrigin
@RequestMapping(value = "/users/1.0/admin")
@RestController
public class ApiUsersAdmin {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected UserAdminService userAdminService;

    /**
     * Get the list of users in purgatory
     *
     * Return the list of users in purgatory, return all the users as the list should not be too long.
     * Only super admin and user admin can get that list
     *
     */
    @Operation(
            summary = "Get the list of users in purgatory",
            description = "Return the list of users in purgatory, return all the users as the list should not be too long." +
                    " Only god admin and user admin can get that list, API sessions not allowed. When now users are in purgatory," +
                    " an empty list is returned.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "List of user in purgatory",
                            content = @Content(array = @ArraySchema(schema = @Schema( implementation = UserListElementResponse.class)))),
                    @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ActionResult.class))),
            }
    )
    @RequestMapping(
            value = "/purgatory",
            produces = "application/json",
            method = RequestMethod.GET
    )
    @PreAuthorize("hasRole('ROLE_LOGIN_COMPLETE') and hasAnyRole('ROLE_GOD_ADMIN','ROLE_USER_ADMIN')")
    // ----------------------------------------------------------------------
    public ResponseEntity<?> getUserInPurgatory(
            HttpServletRequest request
    ) {
        try {
            List<UserListElementResponse> r = userAdminService.searchUsersInPurgatory(
                    request.getUserPrincipal().getName(),
                    request
            );
            return new ResponseEntity<>(r, HttpStatus.OK);
        } catch (ITNotFoundException x) {
            return new ResponseEntity<>(new ArrayList<UserListElementResponse>(), HttpStatus.OK);
        }
    }


}
