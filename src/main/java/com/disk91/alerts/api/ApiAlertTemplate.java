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

import com.disk91.alerts.api.interfaces.AlertTemplateListResponseItf;
import com.disk91.alerts.api.interfaces.AlertTemplateResponseItf;
import com.disk91.alerts.api.interfaces.AlertTemplateUpsertBody;
import com.disk91.alerts.services.AlertTemplateService;
import com.disk91.common.api.interfaces.ActionResult;
import com.disk91.common.tools.exceptions.ITNotFoundException;
import com.disk91.common.tools.exceptions.ITParseException;
import com.disk91.common.tools.exceptions.ITRightException;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Alert Template API", description = "Alert template management endpoints")
@CrossOrigin
@RequestMapping(value = "/alerts/1.0/template")
@RestController
public class ApiAlertTemplate {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected AlertTemplateService alertTemplateService;

    // ================================================================================================================
    // UPSERT
    // ================================================================================================================

    /**
     * Create or update an alert template.
     * When the body contains an id the request is an update; otherwise a new template is created.
     * ROLE_ALERTS_ADMIN can create/update global templates and modify any template.
     * ROLE_ALERTS_TEMPLATE can only create/update non-global templates and modify their own.
     */
    @Operation(
            summary = "Create or update an alert template",
            description = "When 'id' is present in the body the request is an update, otherwise a new template is created. " +
                    "ROLE_ALERTS_ADMIN may create global templates and modify any template. " +
                    "ROLE_ALERTS_TEMPLATE may only create non-global templates and modify their own templates.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Template updated",
                            content = @Content(schema = @Schema(implementation = AlertTemplateResponseItf.class))),
                    @ApiResponse(responseCode = "201", description = "Template created",
                            content = @Content(schema = @Schema(implementation = AlertTemplateResponseItf.class))),
                    @ApiResponse(responseCode = "400", description = "Validation error",
                            content = @Content(schema = @Schema(implementation = ActionResult.class))),
                    @ApiResponse(responseCode = "403", description = "Insufficient rights or template not found",
                            content = @Content(schema = @Schema(implementation = ActionResult.class)))
            }
    )
    @RequestMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
    @PreAuthorize("hasRole('ROLE_LOGIN_COMPLETE') and hasAnyRole('ROLE_ALERTS_ADMIN','ROLE_ALERTS_TEMPLATE')")
    // ----------------------------------------------------------------------
    public ResponseEntity<?> postUpsertAlertTemplate(
            HttpServletRequest request,
            @RequestBody(required = true) AlertTemplateUpsertBody body
    ) {
        boolean isAdmin = request.isUserInRole("ROLE_ALERTS_ADMIN");
        boolean isCreate = (body.getId() == null || body.getId().isBlank());
        try {
            AlertTemplateResponseItf result = alertTemplateService.upsertAlertTemplate(
                    request.getUserPrincipal().getName(), isAdmin, body, request
            );
            return new ResponseEntity<>(result, isCreate ? HttpStatus.CREATED : HttpStatus.OK);
        } catch (ITParseException e) {
            return new ResponseEntity<>(ActionResult.BADREQUEST(e.getMessage()), HttpStatus.BAD_REQUEST);
        } catch (ITRightException | ITNotFoundException e) {
            return new ResponseEntity<>(ActionResult.FORBIDDEN(e.getMessage()), HttpStatus.FORBIDDEN);
        }
    }

    // ================================================================================================================
    // DELETE
    // ================================================================================================================

    /**
     * Delete an alert template by id.
     * ROLE_ALERTS_ADMIN can delete any template; ROLE_ALERTS_TEMPLATE only their own.
     */
    @Operation(
            summary = "Delete an alert template",
            description = "ROLE_ALERTS_ADMIN can delete any template. " +
                    "ROLE_ALERTS_TEMPLATE can only delete templates they own.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Template deleted",
                            content = @Content(schema = @Schema(implementation = ActionResult.class))),
                    @ApiResponse(responseCode = "403", description = "Insufficient rights or template not found",
                            content = @Content(schema = @Schema(implementation = ActionResult.class)))
            }
    )
    @RequestMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.DELETE)
    @PreAuthorize("hasRole('ROLE_LOGIN_COMPLETE') and hasAnyRole('ROLE_ALERTS_ADMIN','ROLE_ALERTS_TEMPLATE')")
    // ----------------------------------------------------------------------
    public ResponseEntity<?> deleteAlertTemplate(
            HttpServletRequest request,
            @Parameter(description = "Id of the template to delete") @PathVariable String id
    ) {
        boolean isAdmin = request.isUserInRole("ROLE_ALERTS_ADMIN");
        try {
            alertTemplateService.deleteAlertTemplate(
                    request.getUserPrincipal().getName(), isAdmin, id, request
            );
            return new ResponseEntity<>(ActionResult.OK("alerts-template-deleted"), HttpStatus.OK);
        } catch (ITRightException | ITNotFoundException e) {
            return new ResponseEntity<>(ActionResult.FORBIDDEN(e.getMessage()), HttpStatus.FORBIDDEN);
        }
    }

    // ================================================================================================================
    // LIST
    // ================================================================================================================

    /**
     * List alert templates visible to the connected user.
     * Returns templates owned by the user and all global templates.
     * An optional search parameter filters by name (case-insensitive LIKE).
     */
    @Operation(
            summary = "List alert templates visible to the connected user",
            description = "Returns templates created by the requesting user and all templates marked as global. " +
                    "An optional 'search' parameter applies a case-insensitive filter on the template name. " +
                    "Accessible to any connected user.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "List of visible templates",
                            content = @Content(schema = @Schema(implementation = AlertTemplateListResponseItf.class))),
                    @ApiResponse(responseCode = "204", description = "No templates found")
            }
    )
    @RequestMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.GET)
    @PreAuthorize("hasRole('ROLE_LOGIN_COMPLETE')")
    // ----------------------------------------------------------------------
    public ResponseEntity<?> getListAlertTemplates(
            HttpServletRequest request,
            @Parameter(description = "Optional case-insensitive name filter")
            @RequestParam(value = "search", required = false) String search
    ) {
        AlertTemplateListResponseItf result = alertTemplateService.listAlertTemplates(
                request.getUserPrincipal().getName(), search, request
        );
        if (result.getTotal() == 0) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}

