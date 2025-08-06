package com.disk91.users.api;

import com.disk91.common.api.interfaces.ActionResult;
import com.disk91.common.tools.exceptions.ITParseException;
import com.disk91.common.tools.exceptions.ITRightException;
import com.disk91.common.tools.exceptions.ITTooManyException;
import com.disk91.users.api.interfaces.UserAccountCreationBody;
import com.disk91.users.api.interfaces.UserLoginBody;
import com.disk91.users.api.interfaces.UserLoginResponse;
import com.disk91.users.api.interfaces.UserTwoFaResponse;
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

import java.util.Optional;

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
     * User need to have a valid JWT with ROLE_REGISTERED_USER and ROLE_LOGIN_1FA
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

    /**
     * Upgrade session endpoint
     *
     * When everything is valid, the user have a non expired password, the user has accepted the
     * latest condition and eventually the user has passed the 2nd factor authentication. Then
     * the user can upgrade its session and get a new JWT with the full access. This endpoint
     * can also be used to get a new JWT token with a longer expiration time.
     *
     * User need to have a valid JWT with ROLE_LOGIN_1FA
     */
    @Operation(
            summary = "User Session Upgrade",
            description = "This endpoint allows to renew / upgrade the JWT token. It's used to extend the " +
                    "JWT duration or to upgrade the roles of the user when password has expired, user condition did " +
                    "not met or the 2nd factor authentication is required.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Session renewed", content = @Content(schema = @Schema(implementation = UserLoginResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Sign out failed", content = @Content(schema = @Schema(implementation = ActionResult.class)))
            }
    )
    @RequestMapping(
            value = "/upgrade",
            produces = "application/json",
            method = RequestMethod.GET
    )
    @PreAuthorize("hasAnyRole('ROLE_LOGIN_1FA')")
    // ----------------------------------------------------------------------
    public ResponseEntity<?> getSelfSessionUpgrade(
            HttpServletRequest request,
            @RequestParam(value = "secondFactor", required = false) Optional<String> secondFactor
    ) {
        try {
            UserLoginResponse r = userService.upgradeSession(
                    request.getUserPrincipal().getName(),
                    secondFactor.orElse(""),
                    request
            );
            return new ResponseEntity<>(r, HttpStatus.OK);
        } catch ( ITParseException | ITRightException e) {
            return new ResponseEntity<>(ActionResult.BADREQUEST(e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Request to verify 2FA
     *
     * This endpoint is here to verify a TOTP (Authenticator) code. It's better to rollback the configuration on the front
     * side in case the code is not verified or the user won't be able to login anymore. This works for all the code, TOTP
     * email or SMS
     *
     * This endpoint requires to have LOGIN COMPLETED first
     */
    @Operation(
            summary = "User 2FA verification",
            description = "Request to verify the 2FA code. The 2FA is directly applicable, so it is recommended to " +
                    "to use the verification endpoint to check the 2FA configuration before logout with a risk of being locked out. " +
                    "This is on the front duty. For Authenticator, the secret is sent by the user ad verifed by the server",
            responses = {
                    @ApiResponse(responseCode = "200", description = "TOTP challenge success", content = @Content(schema = @Schema(implementation = ActionResult.class))),
                    @ApiResponse(responseCode = "400", description = "TOTP challenge failed", content = @Content(schema = @Schema(implementation = ActionResult.class)))
            }
    )
    @RequestMapping(
            value = "/2fa",
            produces = "application/json",
            method = RequestMethod.GET
    )
    @PreAuthorize("hasAnyRole('ROLE_LOGIN_COMPLETE')")
    // ----------------------------------------------------------------------
    public ResponseEntity<?> verifyUser2Fa(
            HttpServletRequest request,
            @RequestParam(value = "secondFactor", required = false) Optional<String> secondFactor
    ) {
        try {
            if ( secondFactor.isPresent() ) {
                boolean result = userService.verifyTOTPCode(
                        request.getUserPrincipal().getName(),
                        secondFactor.get(),
                        request
                );
                if ( result ) {
                    return new ResponseEntity<>(ActionResult.OK("user-session-2fa-code-valid"), HttpStatus.OK);
                } else {
                    return new ResponseEntity<>(ActionResult.BADREQUEST("user-session-2fa-code-invalid"), HttpStatus.BAD_REQUEST);
                }
            } else {
                return new ResponseEntity<>(ActionResult.BADREQUEST("user-session-2fa-code-missing"), HttpStatus.BAD_REQUEST);
            }
        } catch (ITParseException | ITRightException e ) {
            return new ResponseEntity<>(ActionResult.BADREQUEST(e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }


}
