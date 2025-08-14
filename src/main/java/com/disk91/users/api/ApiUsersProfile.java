package com.disk91.users.api;

import com.disk91.common.api.interfaces.ActionResult;
import com.disk91.common.tools.exceptions.ITNotFoundException;
import com.disk91.common.tools.exceptions.ITParseException;
import com.disk91.common.tools.exceptions.ITRightException;
import com.disk91.users.api.interfaces.*;
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



    /**
     * Change Password endpoint - logged user
     *
     * This endpoint allows a user to change its password after beeing logged. It can be used for regular password change but also for
     * password expiration. In this case the user will have to upgrade its token to access the full APIs.
     *
     * This endpoint needs to have a 1FA completed process at least
     */
    @Operation(
            summary = "User password self update",
            description = "Change the user own password. It can be done on regular situation or on password expiration after a 1FA authentication.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User password updated", content = @Content(schema = @Schema(implementation = ActionResult.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid password, see reason in response", content = @Content(schema = @Schema(implementation = ActionResult.class))),
                    @ApiResponse(responseCode = "403", description = "Insufficient rights", content = @Content(schema = @Schema(implementation = ActionResult.class))),
            }
    )
    @RequestMapping(
            value = "/password/change",
            produces = "application/json",
            consumes = "application/json",
            method = RequestMethod.PUT
    )
    @PreAuthorize("hasAnyRole('ROLE_LOGIN_1FA')")
    // ----------------------------------------------------------------------
    public ResponseEntity<?> changeSlefPasswordProfile(
            HttpServletRequest request,
            @RequestBody(required = true) UserPasswordChangeBody body
            ) {
        try {
            userProfileService.userPasswordChange(request.getUserPrincipal().getName(),request.getUserPrincipal().getName(), body.getPassword());
            return new ResponseEntity<>(ActionResult.OK("user-profile-password-changed"), HttpStatus.OK);
        } catch ( ITRightException e ) {
            return new ResponseEntity<>(ActionResult.FORBIDDEN(e.getMessage()), HttpStatus.FORBIDDEN);
        } catch ( ITParseException e ) {
            return new ResponseEntity<>(ActionResult.BADREQUEST(e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Change Password endpoint - lost password - public endpoint
     *
     * This endpoint allows a user to change its password when its password has been lost. This is a public
     * endpoint, the user is identified by a unique key generated with the password loss API.
     *
     * This endpoint is public
     */
    @Operation(
            summary = "User password self update",
            description = "Change the user own password with a link received by email (password lost procedure)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User password updated", content = @Content(schema = @Schema(implementation = ActionResult.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid password, see reason in response", content = @Content(schema = @Schema(implementation = ActionResult.class))),
                    @ApiResponse(responseCode = "403", description = "Insufficient rights", content = @Content(schema = @Schema(implementation = ActionResult.class))),
            }
    )
    @RequestMapping(
            value = "/password/reset",
            produces = "application/json",
            consumes = "application/json",
            method = RequestMethod.PUT
    )
    // ----------------------------------------------------------------------
    public ResponseEntity<?> resetSlefPasswordProfile(
            HttpServletRequest request,
            @RequestBody(required = true) UserPasswordChangeBody body
    ) {
        try {
            userProfileService.userPublicPasswordChange(request, body);
            return new ResponseEntity<>(ActionResult.OK("user-profile-password-changed"), HttpStatus.OK);
        } catch ( ITRightException e ) {
            return new ResponseEntity<>(ActionResult.FORBIDDEN(e.getMessage()), HttpStatus.FORBIDDEN);
        } catch ( ITParseException e ) {
            return new ResponseEntity<>(ActionResult.BADREQUEST(e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }


    /**
     * Request to change password - lost password - public endpoint
     *
     * This endpoint allows a user to request an email message to change its password with a validation Key
     * this endpoint will not return anything for security reason. If the user exists an email will be sent.
     *
     * This endpoint is public
     */
    @Operation(
            summary = "User password reset request",
            description = "Request a password lost email to be sent to the user to change its password. The email will be sent to the user email address." +
                    "Returns success always, no error is returned for security reason.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Password lost link send", content = @Content(schema = @Schema(implementation = ActionResult.class)))
            }
    )
    @RequestMapping(
            value = "/password/request",
            produces = "application/json",
            consumes = "application/json",
            method = RequestMethod.POST
    )
    // ----------------------------------------------------------------------
    public ResponseEntity<?> passwordLostRequest(
            HttpServletRequest request,
            @RequestBody(required = true) UserPasswordLostBody body
    ) {
        try {
            userProfileService.userPublicPasswordLost(request, body);
            return new ResponseEntity<>(ActionResult.OK("user-profile-password-lost-done"), HttpStatus.OK);
        } catch ( ITParseException e ) {
            return new ResponseEntity<>(ActionResult.OK("user-profile-password-lost-done"), HttpStatus.OK);
        }
    }


    /**
     * Request to delete user account
     *
     * This endpoint allows a user to request the deletion of its account. The account is logically
     * deleted immediately, user won't be able to connect and the session are canceled, personnal data
     * access is locked. The user account will be physically deleted based on the purgatory parameter.
     *
     * This endpoint is public
     */
    @Operation(
            summary = "User deletion request",
            description = "Request for user self deletion. The user account is logically deleted and the data access is locked. " +
                    "The user account will be physically deleted based on the purgatory parameter. It can be reactivated by the administator " +
                    "during this period of time. User personal data are all locked and will require user login to be reactivated.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User deleted", content = @Content(schema = @Schema(implementation = ActionResult.class))),
                    @ApiResponse(responseCode = "400", description = "Deletion failed", content = @Content(schema = @Schema(implementation = ActionResult.class)))
            }
    )
    @RequestMapping(
            value = "/",
            produces = "application/json",
            consumes = "application/json",
            method = RequestMethod.DELETE
    )
    @PreAuthorize("hasAnyRole('ROLE_LOGIN_COMPLETE')")
    // ----------------------------------------------------------------------
    public ResponseEntity<?> deleteUserSelf(
            HttpServletRequest request
    ) {
        try {
            userProfileService.deleteUser(
                    request.getUserPrincipal().getName(),
                    request.getUserPrincipal().getName(),
                    request);
            return new ResponseEntity<>(ActionResult.OK("user-profile-delete-done"), HttpStatus.OK);
        } catch (ITParseException | ITRightException | ITNotFoundException e ) {
            return new ResponseEntity<>(ActionResult.BADREQUEST("user-profile-delete-failed"), HttpStatus.BAD_REQUEST);
        }
    }

    // ============================================
    // Enable 2FA
    // ============================================

    /**
     * Request to configure 2FA method
     *
     * This endpoint allows a user to setup a 2FA method. The 2FA is directly applicable, so it is recommended to
     * to use the verification endpoint to check the 2FA configuration before logout with a risk of being locked out.
     * This is on the front duty.
     *
     * This endpoint requires to have LOGIN COMPLETED first
     */
    @Operation(
            summary = "User 2FA configuration",
            description = "Request to enable/disable the 2FA configuration. The 2FA is directly applicable, so it is recommended to " +
                    "to use the verification endpoint to check the 2FA configuration before logout with a risk of being locked out. " +
                    "This is on the front duty. For Authenticator, the secret is generated and returned to the user to compute & display " +
                    "the related QRCode.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User 2FA setup applied", content = @Content(schema = @Schema(implementation = UserTwoFaResponse.class))),
                    @ApiResponse(responseCode = "400", description = "User 2FA setup failed", content = @Content(schema = @Schema(implementation = ActionResult.class)))
            }
    )
    @RequestMapping(
            value = "/2fa",
            produces = "application/json",
            consumes = "application/json",
            method = RequestMethod.PUT
    )
    @PreAuthorize("hasAnyRole('ROLE_LOGIN_COMPLETE')")
    // ----------------------------------------------------------------------
    public ResponseEntity<?> setupUser2Fa(
            HttpServletRequest request,
            @RequestBody(required = true) UserTwoFaBody body
    ) {
        try {
            UserTwoFaResponse r = userProfileService.setupSecondFactor(
                    request.getUserPrincipal().getName(),
                    request.getUserPrincipal().getName(),
                    body,
                    request);
            return new ResponseEntity<>(r, HttpStatus.OK);
        } catch (ITParseException | ITRightException e ) {
            return new ResponseEntity<>(ActionResult.BADREQUEST(e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    // -----------------------------------------
    // User Conditions

    /**
     * Request to accept the user conditions
     *
     * This endpoint is to accept the user conditions ; it is mostly used when the condition change and the
     * user needs to re-accept them to log into the system.
     *
     * This endpoint requires to have LOGIN_1FA completed first
     */
    @Operation(
            summary = "User Condition Acceptance",
            description = "Request to accept the user conditions ; it is mostly used when the condition change and the " +
                    "user needs to re-accept them to log into the system. This endpoint just needs to be called to accept the conditions.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User condition accepted", content = @Content(schema = @Schema(implementation = ActionResult.class))),
                    @ApiResponse(responseCode = "400", description = "User condition change failed", content = @Content(schema = @Schema(implementation = ActionResult.class)))
            }
    )
    @RequestMapping(
            value = "/eula",
            consumes = "application/json",
            method = RequestMethod.PUT
    )
    @PreAuthorize("hasAnyRole('ROLE_LOGIN_1FA')")
    // ----------------------------------------------------------------------
    public ResponseEntity<?> userConditionAccept(
            HttpServletRequest request
    ) {
        try {
            userProfileService.userConditionAcceptation(
                    request.getUserPrincipal().getName(),
                    request.getUserPrincipal().getName(),
                    request);
            return new ResponseEntity<>(ActionResult.OK("user-profile-eula-accepted"), HttpStatus.OK);
        } catch (ITParseException | ITRightException e ) {
            return new ResponseEntity<>(ActionResult.BADREQUEST(e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }


}
