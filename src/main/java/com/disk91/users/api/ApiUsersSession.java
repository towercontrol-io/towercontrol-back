package com.disk91.users.api;

import com.disk91.common.api.interfaces.ActionResult;
import com.disk91.common.tools.exceptions.ITParseException;
import com.disk91.common.tools.exceptions.ITRightException;
import com.disk91.common.tools.exceptions.ITTooManyException;
import com.disk91.users.api.interfaces.UserAccountCreationBody;
import com.disk91.users.api.interfaces.UserLoginBody;
import com.disk91.users.api.interfaces.UserLoginResponse;
import com.disk91.users.services.UserCreationService;
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

@Tag( name = "Users session management API", description = "Users module account session management API" )
@CrossOrigin
@RequestMapping(value = "/users/1.0/session")
@RestController
public class ApiUsersSession {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected UserService userService;

    @Autowired
    protected UserProfileService userProfileService;

    /**
     * Signin endpoint
     *
     * The user with a login and password will log into the system as 1st factor. If a single factor is configured, the user will be logged in
     * if the condition are met. User validity, password not expired, user condition accepted and not updated... If conditions are not met,
     * the user will be logged with the needed roles to upgrade the session later.
     * The endpoint returns a JWT token to be used for authentication in a structure giving the required information to route the user in the
     * right directions
     *
     * Public endpoint
     */
    @Operation(
            summary = "Public Self Creation",
            description = "A user with a valid login (email) and password will be able to get a JWT token to access the next step of the login process or " +
                    "the full services based on the situation. A user with an expired password or user condition not validated will be restricted to the associated " +
                    "endpoints to fix the situation and upgrade its session. A user with a 2FA enabled will be able to access the 2nd authentication factor. No error is " +
                    "returned on failure as a security mechanism.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Session created", content = @Content(schema = @Schema(implementation = UserLoginResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Session creation failed", content = @Content(schema = @Schema(implementation = ActionResult.class)))
            }
    )
    @RequestMapping(
            value = "/signin",
            produces = "application/json",
            consumes = "application/json",
            method = RequestMethod.POST
    )
    // ----------------------------------------------------------------------
    public ResponseEntity<?> postSelfCreation(
            HttpServletRequest request,
            @RequestBody(required = true) UserLoginBody body
    ) {
        try {
            UserLoginResponse r = userService.userLogin(body,request);
            return new ResponseEntity<>(r, HttpStatus.OK);
        } catch ( ITParseException | ITRightException e) {
            return new ResponseEntity<>(ActionResult.BADREQUEST("user-login-refused"), HttpStatus.BAD_REQUEST);
        }
    }


    /**
     * Signout endpoint
     *
     * The user signs out by calling this endpoint. This requires to have a session open and it updates the sessionSecret for
     * that user. So all the user sessions are immediately invalidated.
     *
     * User need to have a valid JWT with ROLE_REGISTERED_USER
     */
    @Operation(
            summary = "User Sign Out",
            description = "A user with a valid JWT token will be able to sign out from the system. This will invalidate all " +
                    "the user sessions and update the session secret for that user.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Signed out", content = @Content(schema = @Schema(implementation = ActionResult.class))),
                    @ApiResponse(responseCode = "400", description = "Sign out failed", content = @Content(schema = @Schema(implementation = ActionResult.class)))
            }
    )
    @RequestMapping(
            value = "/signout",
            produces = "application/json",
            method = RequestMethod.GET
    )
    @PreAuthorize("hasAnyRole('ROLE_REGISTERED_USER')")
    // ----------------------------------------------------------------------
    public ResponseEntity<?> getSelfSignOut(
            HttpServletRequest request
    ) {
        try {
            userProfileService.userSignOut(request.getUserPrincipal().getName(),request.getUserPrincipal().getName(),request);
            return new ResponseEntity<>(ActionResult.OK("user-signed-out"), HttpStatus.OK);
        } catch ( ITParseException | ITRightException e) {
            return new ResponseEntity<>(ActionResult.BADREQUEST(e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

}
