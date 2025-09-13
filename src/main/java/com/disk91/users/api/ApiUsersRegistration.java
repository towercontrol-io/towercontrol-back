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
package com.disk91.users.api;

import com.disk91.common.api.interfaces.ActionResult;
import com.disk91.common.tools.exceptions.ITParseException;
import com.disk91.common.tools.exceptions.ITRightException;
import com.disk91.common.tools.exceptions.ITTooManyException;
import com.disk91.users.api.interfaces.UserAccountRegistrationBody;
import com.disk91.users.services.UserRegistrationService;
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

@Tag( name = "Users Registration public API", description = "Users module registration API" )
@CrossOrigin
@RequestMapping(value = "/users/1.0/registration")
@RestController
public class ApiUsersRegistration {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected UserRegistrationService userRegistrationService;

    /**
     * Registration endpoint
     *
     * A user can request to register on the platform by providing an email address.
     * It will get a link to validate the email address and create an account.
     * All the registration information will be requested later once the email is validated.
     *
     * It's also possible to add an invitation code to restrict the email self registration
     * to people having this code.
     *
     * Public endpoint
     */
    @Operation(
            summary = "Public Self Registration",
            description = "A new user is requesting for registration, it provides an email address and eventually a registration code. An email with a link will be sent to the user for proceeding with the registration." +
                    "No error are returned on this endpoint. It always succeed, whatever happen.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Registration request received", content = @Content(schema = @Schema(implementation = ActionResult.class))),
            }
    )
    @RequestMapping(
            value = "/register",
            produces = "application/json",
            consumes = "application/json",
            method = RequestMethod.POST
    )
    // ----------------------------------------------------------------------
    public ResponseEntity<?> postSelfRegistration(
            HttpServletRequest request,
            @RequestBody(required = true) UserAccountRegistrationBody body
    ) {
        try {
            userRegistrationService.requestAccountCreation(body,request);
            return new ResponseEntity<>(ActionResult.OK("user-registration-received"), HttpStatus.OK);
        } catch (ITTooManyException | ITParseException | ITRightException e) {
            return new ResponseEntity<>(ActionResult.OK("user-registration-received"), HttpStatus.OK);
        }
    }


}
