package com.disk91.users.api;

import com.disk91.common.api.interfaces.ActionResult;
import com.disk91.common.tools.exceptions.ITNotFoundException;
import com.disk91.common.tools.exceptions.ITParseException;
import com.disk91.common.tools.exceptions.ITRightException;
import com.disk91.users.api.interfaces.UserAccountCreationBody;
import com.disk91.users.api.interfaces.UserListElementResponse;
import com.disk91.users.api.interfaces.UserRestoreBody;
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
            @RequestBody(required = true) UserRestoreBody body
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
            @RequestBody(required = true) UserRestoreBody body
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

}
