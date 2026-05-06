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
package com.disk91.audit.api;

import com.disk91.audit.api.interfaces.AuditSearchBody;
import com.disk91.audit.api.interfaces.AuditSearchResponse;
import com.disk91.audit.services.AuditQueryService;
import com.disk91.common.api.interfaces.ActionResult;
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

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Tag(name = "Audit module search API", description = "API for querying audit log entries")
@CrossOrigin
@RequestMapping(value = "/audit/1.0/logs")
@RestController
public class ApiAuditSearch {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected AuditQueryService auditQueryService;

    /**
     * Search and retrieve audit log entries with an optional free-text filter applied
     * simultaneously on service name, action name and owner (OR match), with date range
     * and pagination support.
     * Authenticated users with ROLE_AUDIT_RD see obfuscated parameters (replaced by ***).
     * Users with ROLE_AUDIT_RD_CLEAR see decrypted parameter values.
     * The search string is passed Base64-encoded.
     * Default is the 50 most recent log entries.
     */
    @Operation(
            summary = "Search audit log entries",
            description = "Retrieve audit log entries with an optional free-text search applied as a case-insensitive partial match " +
                    "on service name, action name and owner simultaneously (OR condition). " +
                    "The search string must be Base64-encoded when provided. " +
                    "Date range and pagination filters are also available. " +
                    "Logs are returned from most recent to oldest. " +
                    "Users with ROLE_AUDIT_RD see log parameters replaced by ***. " +
                    "Users with ROLE_AUDIT_RD_CLEAR see decrypted parameter values. " +
                    "When no database target is configured, status 'audit-log-non-database' is returned with an empty list. " +
                    "By default the 50 most recent entries are returned.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Search result with audit log entries",
                            content = @Content(schema = @Schema(implementation = AuditSearchResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Parse Error",
                            content = @Content(schema = @Schema(implementation = ActionResult.class))),
                    @ApiResponse(responseCode = "403", description = "Forbidden",
                            content = @Content(schema = @Schema(implementation = ActionResult.class))),
            }
    )
    @RequestMapping(
            value = "/search",
            produces = "application/json",
            method = RequestMethod.GET
    )
    @PreAuthorize("hasRole('ROLE_LOGIN_COMPLETE') and hasAnyRole('ROLE_AUDIT_RD','ROLE_AUDIT_RD_CLEAR')")
    // ----------------------------------------------------------------------
    public ResponseEntity<?> getAuditSearch(
            HttpServletRequest request,
            @Parameter(description = "Free-text search applied on service, action and owner fields - Base64 encoded (optional)")
            @RequestParam(required = false) String search,
            @Parameter(description = "Start date filter, ms since epoch, inclusive (optional, 0 = no bound)")
            @RequestParam(required = false, defaultValue = "0") long startMs,
            @Parameter(description = "End date filter, ms since epoch, inclusive (optional, 0 = no bound)")
            @RequestParam(required = false, defaultValue = "0") long endMs,
            @Parameter(description = "Page number, 0-based (optional, default 0)")
            @RequestParam(required = false, defaultValue = "0") int page,
            @Parameter(description = "Number of elements per page, max 200 (optional, default 50)")
            @RequestParam(required = false, defaultValue = "50") int pageSize
    ) {
        try {
            // Decode the Base64-encoded search string; reject malformed input immediately
            AuditSearchBody body = new AuditSearchBody();
            body.setSearch(decodeBase64Param("search", search));
            body.setStartMs(startMs);
            body.setEndMs(endMs);
            body.setPage(page);
            body.setPageSize(pageSize);

            // Determine if the caller has cleartext access to decrypted params
            boolean clearAccess = request.isUserInRole("ROLE_AUDIT_RD_CLEAR");
            AuditSearchResponse response = auditQueryService.searchAuditLogs(
                    request.getUserPrincipal().getName(),
                    body,
                    clearAccess,
                    request
            );
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (ITParseException x) {
            return new ResponseEntity<>(ActionResult.BADREQUEST(x.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Decode a Base64-encoded query parameter. Returns null when the parameter is absent.
     * Throws ITParseException when the value is present but not valid Base64.
     * @param paramName - name of the parameter (used in log message)
     * @param value     - raw query parameter value, may be null
     * @return decoded string or null
     * @throws ITParseException - when the base64 decoding fails
     */
    private String decodeBase64Param(String paramName, String value) throws ITParseException {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            log.warn("[audit] Invalid Base64 encoding for parameter '{}'", paramName);
            throw new ITParseException("audit-search-invalid-base64-param");
        }
    }

}


