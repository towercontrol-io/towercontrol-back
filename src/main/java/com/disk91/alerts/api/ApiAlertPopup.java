/*
 * Copyright (c) - Paul Pinault (aka disk91) - 2026.
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
package com.disk91.alerts.api;

import com.disk91.alerts.api.interfaces.AlertPopupCountResponseItf;
import com.disk91.alerts.api.interfaces.AlertPopupResponseItf;
import com.disk91.alerts.services.AlertPopupService;
import com.disk91.common.api.interfaces.ActionResult;
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

import java.util.List;

@Tag(name = "Alert popup API", description = "In-app popup notification management for the authenticated user")
@CrossOrigin
@RequestMapping(value = "/alerts/1.0/popup")
@RestController
public class ApiAlertPopup {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected AlertPopupService alertPopupService;

    /**
     * Get the popup notification list for the authenticated user.
     * Returns unread popups and read popups from the last two days, capped at 10.
     */
    @Operation(
            summary = "Get popup notifications for the authenticated user",
            description = "Returns up to X popup notifications: all unread ones and read ones from the last X days, " +
                    "ordered by time descending. X depends on configuration, default is 10 popup and 2 days. Requires full authentication.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "List of popup notifications",
                            content = @Content(array = @ArraySchema(schema = @Schema(implementation = AlertPopupResponseItf.class)))),
                    @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ActionResult.class)))
            }
    )
    @RequestMapping(value = "", produces = "application/json", method = RequestMethod.GET)
    @PreAuthorize("hasRole('ROLE_LOGIN_COMPLETE')")
    // ----------------------------------------------------------------------
    public ResponseEntity<?> getPopups(HttpServletRequest request) {
        String userLogin = request.getUserPrincipal().getName();
        List<AlertPopupResponseItf> popups = alertPopupService.getPopupsForUser(userLogin);
        return new ResponseEntity<>(popups, HttpStatus.OK);
    }

    /**
     * Get the unread popup count for the badge display of the authenticated user.
     */
    @Operation(
            summary = "Get unread popup notification count",
            description = "Returns the count of unread popup notifications for the authenticated user. " +
                    "Used to drive the badge indicator on the bell icon. Requires full authentication.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Unread count",
                            content = @Content(schema = @Schema(implementation = AlertPopupCountResponseItf.class))),
                    @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ActionResult.class)))
            }
    )
    @RequestMapping(value = "/count", produces = "application/json", method = RequestMethod.GET)
    @PreAuthorize("hasRole('ROLE_LOGIN_COMPLETE')")
    // ----------------------------------------------------------------------
    public ResponseEntity<?> getUnreadCount(HttpServletRequest request) {
        String userLogin = request.getUserPrincipal().getName();
        AlertPopupCountResponseItf count = alertPopupService.getUnreadCount(userLogin);
        return new ResponseEntity<>(count, HttpStatus.OK);
    }

    /**
     * Mark all popup notifications as viewed for the authenticated user (clears the badge).
     */
    @Operation(
            summary = "Mark all popup notifications as viewed",
            description = "Marks all unread popup notifications as viewed for the authenticated user. " +
                    "This clears the badge counter. Requires full authentication.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "All popups marked as viewed"),
                    @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ActionResult.class)))
            }
    )
    @RequestMapping(value = "/viewed", produces = "application/json", method = RequestMethod.PUT)
    @PreAuthorize("hasRole('ROLE_LOGIN_COMPLETE')")
    // ----------------------------------------------------------------------
    public ResponseEntity<?> markAllViewed(HttpServletRequest request) {
        String userLogin = request.getUserPrincipal().getName();
        alertPopupService.markAllViewed(userLogin);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
