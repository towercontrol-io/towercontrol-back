package com.disk91.users.api;

import com.disk91.common.api.interfaces.ActionResult;
import com.disk91.common.tools.exceptions.ITParseException;
import com.disk91.common.tools.exceptions.ITRightException;
import com.disk91.common.tools.exceptions.ITTooManyException;
import com.disk91.users.api.interfaces.UserAccountRegistrationBody;
import com.disk91.users.api.interfaces.UserConfigResponse;
import com.disk91.users.services.UserRegistrationService;
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
import org.springframework.web.bind.annotation.*;

@Tag( name = "Users module configuration public API", description = "Users module configuration API" )
@CrossOrigin
@RequestMapping(value = "/users/1.0/config")
@RestController
public class ApiUsersConfig {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected UserService userService;

    /**
     * User module configuration endpoint
     *
     * The frontend behavior depends on the user module configuration.This public API endpoint
     * allows to get the module configuration to apply this on the front end,
     *
     * Public endpoint
     */
    @Operation(
            summary = "Public Get user module configuration",
            description = "Get the user module configuration to apply this on the front end, showing and hiding the features based on " +
                    "the configuration.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User module configuration", content = @Content(schema = @Schema(implementation = UserConfigResponse.class))),
            }
    )
    @RequestMapping(
            value = "",
            produces = "application/json",
            method = RequestMethod.GET
    )
    // ----------------------------------------------------------------------
    public ResponseEntity<?> getUserConfiguration(
            HttpServletRequest request
    ) {
        UserConfigResponse r = userService.getUserModuleConfig();
        return new ResponseEntity<>(r, HttpStatus.OK);
    }


}
