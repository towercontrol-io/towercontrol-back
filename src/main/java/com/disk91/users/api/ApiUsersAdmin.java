package com.disk91.users.api;

import com.disk91.common.api.interfaces.ActionResult;
import com.disk91.common.tools.exceptions.ITNotFoundException;
import com.disk91.common.tools.exceptions.ITParseException;
import com.disk91.common.tools.exceptions.ITRightException;
import com.disk91.users.api.interfaces.*;
import com.disk91.users.services.UserAdminService;
import com.disk91.users.services.UserProfileService;
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

    @Autowired
    protected UserProfileService userProfileService;

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

    /**
     * Get the list of users based on a search criteria
     *
     * Return the list of users based on a search criteria, no filter on the user profile is done.
     * The input is an email, as the email is encrypted, search is based on hash of group of 3 characters
     * from the beginning of the email and the domain name if provided. A minimum of 3 characters is required.
     *
     */
    @Operation(
            summary = "Get the list of users based on a search criteria",
            description = "Return the list of users based on a search criteria, no filter on the user profile is done. " +
                    "The input is an email, as the email is encrypted, search is based on hash of group of 3 characters " +
                    "from the beginning of the email and the domain name if provided. A minimum of 3 characters is required. " +
                    "Returns an empty list when no user are found." +
                    "Only god admin and user admin can get that list, API sessions not allowed.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "List of user corresponding to the search",
                            content = @Content(array = @ArraySchema(schema = @Schema( implementation = UserListElementResponse.class)))),
                    @ApiResponse(responseCode = "400", description = "Parse Error", content = @Content(schema = @Schema(implementation = ActionResult.class))),
                    @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ActionResult.class))),
            }
    )
    @RequestMapping(
            value = "/search",
            produces = "application/json",
            method = RequestMethod.POST
    )
    @PreAuthorize("hasRole('ROLE_LOGIN_COMPLETE') and hasAnyRole('ROLE_GOD_ADMIN','ROLE_USER_ADMIN')")
    // ----------------------------------------------------------------------
    public ResponseEntity<?> postUserSearch(
            HttpServletRequest request,
            @RequestBody(required = true) UserSearchBody body
    ) {
        try {
            List<UserListElementResponse> r = userAdminService.searchUsersByEmail(
                    request.getUserPrincipal().getName(),
                    body,
                    request
            );
            return new ResponseEntity<>(r, HttpStatus.OK);
        } catch (ITNotFoundException x) {
            return new ResponseEntity<>(new ArrayList<UserListElementResponse>(), HttpStatus.OK);
        } catch (ITParseException x) {
            return new ResponseEntity<>(ActionResult.BADREQUEST(x.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Get the list of last connected users
     *
     * Return the list of 10-11 last connected users excluding the requestor when present.
     *
     */
    @Operation(
            summary = "Get the list of last connected users",
            description = "Return the list of 10-11 last connected users excluding the requestor when present. " +
                    "Returns an empty list when no user are found." +
                    "Only god admin and user admin can get that list, API sessions not allowed.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "List of users ordered by last connection date",
                            content = @Content(array = @ArraySchema(schema = @Schema( implementation = UserListElementResponse.class)))),
                    @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ActionResult.class))),
            }
    )
    @RequestMapping(
            value = "/search",
            produces = "application/json",
            method = RequestMethod.GET
    )
    @PreAuthorize("hasRole('ROLE_LOGIN_COMPLETE') and hasAnyRole('ROLE_GOD_ADMIN','ROLE_USER_ADMIN')")
    // ----------------------------------------------------------------------
    public ResponseEntity<?> getUserSearch(
            HttpServletRequest request
    ) {
        List<UserListElementResponse> r = userAdminService.searchLastConnectecUsers(
                request.getUserPrincipal().getName(),
                request
        );
        return new ResponseEntity<>(r, HttpStatus.OK);
    }


    /**
     * Get the list of last registered users
     *
     * Return the list of 50 last registered users excluding the requestor when present.
     *
     */
    @Operation(
            summary = "Get the list of last registered users",
            description = "Return the list of ~50 last registered users excluding the requestor when present. " +
                    "Returns an empty list when no user registered." +
                    "Only god admin and user admin can get that list, API sessions not allowed.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "List of users ordered on last registration",
                            content = @Content(array = @ArraySchema(schema = @Schema( implementation = UserListElementResponse.class)))),
                    @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ActionResult.class))),
            }
    )
    @RequestMapping(
            value = "/registered",
            produces = "application/json",
            method = RequestMethod.GET
    )
    @PreAuthorize("hasRole('ROLE_LOGIN_COMPLETE') and hasAnyRole('ROLE_GOD_ADMIN','ROLE_USER_ADMIN')")
    // ----------------------------------------------------------------------
    public ResponseEntity<?> getUserLastRegistered(
            HttpServletRequest request
    ) {
        List<UserListElementResponse> r = userAdminService.searchLastRegisteredUsers(
                request.getUserPrincipal().getName(),
                request
        );
        return new ResponseEntity<>(r, HttpStatus.OK);
    }


    // ==========================================================================
    // User restoration / deletion by admin
    // ==========================================================================

    /**
     * Request to restore user account from a logical deletion
     *
     * This endpoint allows an admin user to restore a user account that was logically deleted
     * during the purgatory period. The user account is reactivated, but the encryption keys are
     * not restored (impossible), the user will have to login to restore all the personal data
     * access.
     *
     * This endpoint is private
     */
    @Operation(
            summary = "User restore request",
            description = "This endpoint allows an admin user to restore a user account that was logically deleted " +
                    "during the purgatory period. The user account is reactivated, but the encryption keys are " +
                    "not restored (impossible), the user will have to login to restore all the personal data access. " +
                    "Only god admin and user admin can get that list, API sessions not allowed.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User restored", content = @Content(schema = @Schema(implementation = ActionResult.class))),
                    @ApiResponse(responseCode = "400", description = "Restoration failed", content = @Content(schema = @Schema(implementation = ActionResult.class))),
                    @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ActionResult.class)))
            }
    )
    @RequestMapping(
            value = "/restore",
            produces = "application/json",
            consumes = "application/json",
            method = RequestMethod.PUT
    )
    @PreAuthorize("hasRole('ROLE_LOGIN_COMPLETE') and hasAnyRole('ROLE_GOD_ADMIN','ROLE_USER_ADMIN')")
    // ----------------------------------------------------------------------
    public ResponseEntity<?> deleteUserByAdmin(
            HttpServletRequest request,
            @RequestBody(required = true) UserIdentificationBody body
    ) {
        try {
            userProfileService.restoreUser(
                    request.getUserPrincipal().getName(),
                    body.getLogin(),
                    request);
            return new ResponseEntity<>(ActionResult.OK("user-profile-delete-done"), HttpStatus.OK);
        } catch (ITParseException | ITNotFoundException e ) {
            return new ResponseEntity<>(ActionResult.BADREQUEST(e.getMessage()), HttpStatus.BAD_REQUEST);
        } catch (ITRightException e ) {
            return new ResponseEntity<>(ActionResult.FORBIDDEN(e.getMessage()), HttpStatus.FORBIDDEN);
        }
    }

    /**
     * Request to purge user account from a logical deletion
     *
     * This endpoint allows an admin user to purge a user account that was logically deleted
     * during the purgatory period. The user account is definitely destroyed, the associated
     * user elements will be destroyed in cascade.
     *
     * This endpoint is private
     */
    @Operation(
            summary = "User purge request",
            description = "This endpoint allows an admin user to purge a user account that was logically deleted " +
                    "during the purgatory period. The user account is definitely destroyed, the associated user " +
                    "elements will be destroyed in cascade." +
                    "Only god admin and user admin can get that list, API sessions not allowed.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User purged", content = @Content(schema = @Schema(implementation = ActionResult.class))),
                    @ApiResponse(responseCode = "400", description = "Purge failed", content = @Content(schema = @Schema(implementation = ActionResult.class))),
                    @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ActionResult.class)))
            }
    )
    @RequestMapping(
            value = "/purge",
            produces = "application/json",
            consumes = "application/json",
            method = RequestMethod.DELETE
    )
    @PreAuthorize("hasRole('ROLE_LOGIN_COMPLETE') and hasAnyRole('ROLE_GOD_ADMIN','ROLE_USER_ADMIN')")
    // ----------------------------------------------------------------------
    public ResponseEntity<?> purgeUserByAdmin(
            HttpServletRequest request,
            @RequestBody(required = true) UserIdentificationBody body
    ) {
        try {
            userProfileService.deleteUser(
                    request.getUserPrincipal().getName(),
                    body.getLogin(),
                    true,
                    request);
            return new ResponseEntity<>(ActionResult.OK("user-profile-delete-done"), HttpStatus.OK);
        } catch (ITParseException | ITNotFoundException e ) {
            return new ResponseEntity<>(ActionResult.BADREQUEST(e.getMessage()), HttpStatus.BAD_REQUEST);
        } catch (ITRightException e ) {
            return new ResponseEntity<>(ActionResult.FORBIDDEN(e.getMessage()), HttpStatus.FORBIDDEN);
        }
    }

    /**
     * Request to delete user account
     *
     * This endpoint allows an admin to request the deletion of a user account. The account is logically
     * deleted immediately, user won't be able to connect and the session are canceled, personal data
     * access is locked. The user account will be physically deleted based on the purgatory parameter.
     *
     * This endpoint is public
     */
    @Operation(
            summary = "User deletion request",
            description = "Request for admin user deletion. The user account is logically deleted and the data access is locked. " +
                    "The user account will be physically deleted based on the purgatory parameter. It can't be reactivated by the administrator " +
                    "during this period of time. User personal data are all locked and will require user login to be reactivated. " +
                    "Only god admin and user admin can get that list, API sessions not allowed.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User deleted", content = @Content(schema = @Schema(implementation = ActionResult.class))),
                    @ApiResponse(responseCode = "400", description = "Deletion failed", content = @Content(schema = @Schema(implementation = ActionResult.class)))
            }
    )
    @RequestMapping(
            value = "/delete",
            produces = "application/json",
            consumes = "application/json",
            method = RequestMethod.DELETE
    )
    @PreAuthorize("hasRole('ROLE_LOGIN_COMPLETE') and hasAnyRole('ROLE_GOD_ADMIN','ROLE_USER_ADMIN')")
    // ----------------------------------------------------------------------
    public ResponseEntity<?> deleteUserSelf(
            HttpServletRequest request,
            @RequestBody(required = true) UserIdentificationBody body
    ) {
        try {
            userProfileService.deleteUser(
                    request.getUserPrincipal().getName(),
                    body.getLogin(),
                    false,
                    request);
            return new ResponseEntity<>(ActionResult.OK("user-profile-delete-done"), HttpStatus.OK);
        } catch (ITParseException | ITRightException | ITNotFoundException e ) {
            return new ResponseEntity<>(ActionResult.BADREQUEST("user-profile-delete-failed"), HttpStatus.BAD_REQUEST);
        }
    }


    // ==========================================================================
    // User modification by admin
    // ==========================================================================

    /**
     * Switch a user active mode (activate / deactivate)
     *
     * This endpoint allows an admin user set a user as active or not active.
     * A user with self registration with admin validation will be initially not active.
     * A non active user cannot login.
     *
     * This endpoint is private
     */
    @Operation(
            summary = "Switch a user active mode (activate / deactivate)",
            description = "This endpoint allows an admin user set a user as active or not active. " +
                    "A user with self registration with admin validation will be initially not active. " +
                    "A non active user cannot login. " +
                    "Only god admin and user admin can get that list, API sessions not allowed.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User state changed", content = @Content(schema = @Schema(implementation = ActionResult.class))),
                    @ApiResponse(responseCode = "400", description = "State change failed", content = @Content(schema = @Schema(implementation = ActionResult.class))),
                    @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ActionResult.class)))
            }
    )
    @RequestMapping(
            value = "/active",
            produces = "application/json",
            consumes = "application/json",
            method = RequestMethod.PUT
    )
    @PreAuthorize("hasRole('ROLE_LOGIN_COMPLETE') and hasAnyRole('ROLE_GOD_ADMIN','ROLE_USER_ADMIN')")
    // ----------------------------------------------------------------------
    public ResponseEntity<?> activeUserByAdmin(
            HttpServletRequest request,
            @RequestBody(required = true) UserStateSwitchBody body
    ) {
        try {
            userProfileService.activStateChangeUser(
                    request.getUserPrincipal().getName(),
                    body,
                    request);
            return new ResponseEntity<>(ActionResult.OK("user-profile-activation-done"), HttpStatus.OK);
        } catch (ITParseException | ITNotFoundException e ) {
            return new ResponseEntity<>(ActionResult.BADREQUEST(e.getMessage()), HttpStatus.BAD_REQUEST);
        } catch (ITRightException e ) {
            return new ResponseEntity<>(ActionResult.FORBIDDEN(e.getMessage()), HttpStatus.FORBIDDEN);
        }
    }


    /**
     * Switch a user lock mode (activate / deactivate)
     *
     * This endpoint allows an admin user set a user as locked or not locked.
     * A locked user cannot login but all the data stays available.
     *
     * This endpoint is private
     */
    @Operation(
            summary = "Switch a user lock mode (activate / deactivate)",
            description = "This endpoint allows an admin user set a user as locked or not locked. " +
                    "A locked user cannot login but all the data stays available. " +
                    "Only god admin and user admin can get that list, API sessions not allowed.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User state changed", content = @Content(schema = @Schema(implementation = ActionResult.class))),
                    @ApiResponse(responseCode = "400", description = "State change failed", content = @Content(schema = @Schema(implementation = ActionResult.class))),
                    @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ActionResult.class)))
            }
    )
    @RequestMapping(
            value = "/lock",
            produces = "application/json",
            consumes = "application/json",
            method = RequestMethod.PUT
    )
    @PreAuthorize("hasRole('ROLE_LOGIN_COMPLETE') and hasAnyRole('ROLE_GOD_ADMIN','ROLE_USER_ADMIN')")
    // ----------------------------------------------------------------------
    public ResponseEntity<?> lockUserByAdmin(
            HttpServletRequest request,
            @RequestBody(required = true) UserStateSwitchBody body
    ) {
        try {
            userProfileService.lockStateChangeUser(
                    request.getUserPrincipal().getName(),
                    body,
                    request);
            return new ResponseEntity<>(ActionResult.OK("user-profile-lock-state-done"), HttpStatus.OK);
        } catch (ITParseException | ITNotFoundException e ) {
            return new ResponseEntity<>(ActionResult.BADREQUEST(e.getMessage()), HttpStatus.BAD_REQUEST);
        } catch (ITRightException e ) {
            return new ResponseEntity<>(ActionResult.FORBIDDEN(e.getMessage()), HttpStatus.FORBIDDEN);
        }
    }


    /**
     * Disable user two factor authentication
     *
     * This endpoint allows to manually disable a user two factor authentication when that user
     * is not able to restore the two factor by himself.
     *
     * This endpoint is private
     */
    @Operation(
            summary = "Disable user two factor authentication",
            description = "This endpoint allows to manually disable a user two factor authentication when that user " +
                    "is not able to restore the two factor by himself. " +
                    "Only god admin and user admin can get that list, API sessions not allowed.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User 2fa disabled", content = @Content(schema = @Schema(implementation = ActionResult.class))),
                    @ApiResponse(responseCode = "400", description = "2fa change failed", content = @Content(schema = @Schema(implementation = ActionResult.class))),
                    @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ActionResult.class)))
            }
    )
    @RequestMapping(
            value = "/2fa/disable",
            produces = "application/json",
            consumes = "application/json",
            method = RequestMethod.PUT
    )
    @PreAuthorize("hasRole('ROLE_LOGIN_COMPLETE') and hasAnyRole('ROLE_GOD_ADMIN','ROLE_USER_ADMIN')")
    // ----------------------------------------------------------------------
    public ResponseEntity<?> diable2faUserByAdmin(
            HttpServletRequest request,
            @RequestBody(required = true) UserStateSwitchBody body
    ) {
        try {
            userProfileService.twoFaStateChangeUser(
                    request.getUserPrincipal().getName(),
                    body,
                    request);
            return new ResponseEntity<>(ActionResult.OK("user-two-fa-deactivated"), HttpStatus.OK);
        } catch (ITParseException | ITNotFoundException e ) {
            return new ResponseEntity<>(ActionResult.BADREQUEST(e.getMessage()), HttpStatus.BAD_REQUEST);
        } catch (ITRightException e ) {
            return new ResponseEntity<>(ActionResult.FORBIDDEN(e.getMessage()), HttpStatus.FORBIDDEN);
        }
    }

}
