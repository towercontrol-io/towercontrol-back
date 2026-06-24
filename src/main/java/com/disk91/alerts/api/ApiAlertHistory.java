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

import com.disk91.alerts.api.interfaces.AlertUserHistoryListResponseItf;
import com.disk91.alerts.services.AlertService;
import com.disk91.common.api.interfaces.ActionResult;
import com.disk91.common.tools.exceptions.ITNotFoundException;
import com.disk91.common.tools.exceptions.ITParseException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

@Tag(name = "Alert history API", description = "Personal alert history for the authenticated user")
@CrossOrigin
@RequestMapping(value = "/alerts/1.0/history")
@RestController
public class ApiAlertHistory {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected AlertService alertService;

    /**
     * Return a paginated list of alerts where the authenticated user appears in the delivery records.
     * Only the user's own delivery entry is included; no other user's data is exposed.
     * Optional filter by template identifier(s).
     */
    @Operation(
            summary = "Get the authenticated user's personal alert history",
            description = "Returns a paginated list of alerts where the authenticated user has a delivery record. " +
                    "Results are ordered by event time descending (newest first). " +
                    "ROLE_GOD_ADMIN receives the full sent list and targetedGroups. " +
                    "Regular users receive only their own delivery entry and empty targetedGroups. " +
                    "id and publicAccessId are never exposed. Page size is capped at 100. Requires full authentication.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Paginated alert history (empty list when no results)",
                            content = @Content(schema = @Schema(implementation = AlertUserHistoryListResponseItf.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid page or size parameter",
                            content = @Content(schema = @Schema(implementation = ActionResult.class))),
                    @ApiResponse(responseCode = "403", description = "Forbidden",
                            content = @Content(schema = @Schema(implementation = ActionResult.class)))
            }
    )
    @RequestMapping(value = "", produces = "application/json", method = RequestMethod.GET)
    @PreAuthorize("hasRole('ROLE_LOGIN_COMPLETE')")
    // ----------------------------------------------------------------------
    public ResponseEntity<?> getAlertHistory(
            HttpServletRequest request,
            @Parameter(description = "0-based page number", example = "0")
            @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "Page size, 1–100", example = "20")
            @RequestParam(value = "size", defaultValue = "20") int size,
            @Parameter(description = "Optional filter on one or more alertTemplateId values")
            @RequestParam(value = "templateId", required = false) List<String> templateId
    ) {
        try {
            String userLogin = request.getUserPrincipal().getName();
            AlertUserHistoryListResponseItf result = alertService.getUserAlertHistory(
                    userLogin, page, size, templateId
            );
            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (ITNotFoundException x) {
            return new ResponseEntity<>(ActionResult.BADREQUEST(x.getMessage()), HttpStatus.BAD_REQUEST);
        } catch (ITParseException x) {
            return new ResponseEntity<>(ActionResult.BADREQUEST(x.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }
}
