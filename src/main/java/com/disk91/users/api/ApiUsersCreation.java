package com.disk91.users.api;

import com.disk91.common.api.interfaces.ActionResult;
import com.disk91.common.tools.exceptions.ITParseException;
import com.disk91.common.tools.exceptions.ITRightException;
import com.disk91.common.tools.exceptions.ITTooManyException;
import com.disk91.users.api.interfaces.UserAccountCreationBody;
import com.disk91.users.services.UserCreationService;
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
import org.springframework.web.bind.annotation.*;

@Tag( name = "Users creation public API", description = "Users module account creation API" )
@CrossOrigin
@RequestMapping(value = "/users/1.0/creation")
@RestController
public class ApiUsersCreation {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected UserCreationService userCreationService;

    /**
     * Creation endpoint
     * A user with a validation code can self register to create its own account when the
     * self registration is enabled. When the auto validation is enabled, the user will
     * be able to login directly after the registration. Otherwise, admin will have to
     * validate the account creation.
     *
     * Public endpoint
     */
    @Operation(
            summary = "Public Self Creation",
            description = "A user with a previous registration will be able to request for account creation. The system verify the registration token " +
                    "validity, the password rules the user unity, the eula validation, based on service parameters... User is created and can login, " +
                    "based on the self validation parameter or needs to be validated by an admin. The login/email will be the one used for registration, " +
                    "it is not mandatory in the body. ",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User created", content = @Content(schema = @Schema(implementation = ActionResult.class))),
                    @ApiResponse(responseCode = "400", description = "User creation failed", content = @Content(schema = @Schema(implementation = ActionResult.class)))
            }
    )
    @RequestMapping(
            value = "/create",
            produces = "application/json",
            consumes = "application/json",
            method = RequestMethod.POST
    )
    // ----------------------------------------------------------------------
    public ResponseEntity<?> postSelfCreation(
            HttpServletRequest request,
            @RequestBody(required = true) UserAccountCreationBody body
    ) {
        try {
            userCreationService.createUserSelf(body,request);
            return new ResponseEntity<>(ActionResult.OK("user-creation-created"), HttpStatus.OK);
        } catch (ITTooManyException | ITParseException | ITRightException e) {
            return new ResponseEntity<>(ActionResult.BADREQUEST(e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }


}
