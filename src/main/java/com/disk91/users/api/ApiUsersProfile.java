package com.disk91.users.api;

import com.disk91.common.api.interfaces.ActionResult;
import com.disk91.common.tools.exceptions.ITParseException;
import com.disk91.common.tools.exceptions.ITRightException;
import com.disk91.users.api.interfaces.UserBasicProfileResponse;
import com.disk91.users.api.interfaces.UserLoginBody;
import com.disk91.users.api.interfaces.UserLoginResponse;
import com.disk91.users.services.UserProfileService;
import com.disk91.users.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
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

@Tag( name = "Users profile management API", description = "Users module profile management API" )
@CrossOrigin
@RequestMapping(value = "/users/1.0/profile")
@RestController
public class ApiUsersProfile {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected UserProfileService userProfileService;

    /**
     * Basic Profile endpoint
     *
     * This is a minimalist profile structure you can get from the user information, mostly used for interface
     * display. The First and Last name may be empty until the user set it up. The ACL and groups are also returned
     * for front-end menu activation.
     *
     * This endpoint needs to have a completed signup process
     */
    @Operation(
            summary = "User basic profile",
            description = "Returns the minimal information about the user to manage a front-end structure, the First & Last name " +
                    "of the user may be empty until set. The structure contains user Roles and ACL to manage the front-end menus and feature " +
                    "switch based on the user profile.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User basic profile", content = @Content(schema = @Schema(implementation = UserBasicProfileResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Failure", content = @Content(schema = @Schema(implementation = ActionResult.class)))
            }
    )
    @RequestMapping(
            value = "/basic",
            produces = "application/json",
            method = RequestMethod.GET
    )
    @PreAuthorize("hasAnyRole('ROLE_LOGIN_COMPLETE')")
    // ----------------------------------------------------------------------
    public ResponseEntity<?> getSlefBasicProfile(
            HttpServletRequest request
    ) {
        try {
            UserBasicProfileResponse r = userProfileService.getMyUserBasicProfile(request.getUserPrincipal().getName(),request.getUserPrincipal().getName());
            return new ResponseEntity<>(r, HttpStatus.OK);
        } catch ( ITRightException e) {
            return new ResponseEntity<>(ActionResult.BADREQUEST(e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }


}
