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
package com.disk91.audit.services;

import com.disk91.audit.api.interfaces.AuditResponse;
import com.disk91.audit.api.interfaces.AuditSearchBody;
import com.disk91.audit.api.interfaces.AuditSearchResponse;
import com.disk91.audit.config.AuditConfig;
import com.disk91.audit.integration.AuditIntegration;
import com.disk91.audit.mdb.entities.AuditMdb;
import com.disk91.audit.mdb.repositories.AuditRepositoryMdb;
import com.disk91.audit.pdb.entities.Audit;
import com.disk91.audit.pdb.repositories.AuditRepository;
import com.disk91.common.tools.Now;
import com.disk91.common.tools.Tools;
import com.disk91.common.tools.exceptions.ITParseException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AuditQueryService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    // Maximum allowed page size to prevent abusive queries
    private static final int MAX_PAGE_SIZE = 200;
    // Default page size when not specified
    private static final int DEFAULT_PAGE_SIZE = 50;

    @Autowired
    protected AuditConfig auditConfig;

    @Autowired
    protected AuditService auditService;

    @Autowired
    protected AuditIntegration auditIntegration;

    @Autowired
    protected AuditRepository auditRepository;

    @Autowired
    protected AuditRepositoryMdb auditRepositoryMdb;

    /**
     * Search audit log entries according to given criteria with pagination support.
     * Source (MongoDB or PostgreSQL) is determined by the active audit store configuration.
     * Log parameters are obfuscated (***) for ROLE_AUDIT_RD users and decrypted for ROLE_AUDIT_RD_CLEAR users.
     * When no database backend is configured, an empty result with status "audit-log-non-database" is returned.
     *
     * @param requester  - login of the requesting user (for logging)
     * @param body       - search criteria (may be null for default: last 50 entries)
     * @param clearAccess - true if the caller has ROLE_AUDIT_RD_CLEAR (decrypted params)
     * @param req        - HTTP request for IP tracing
     * @return AuditSearchResponse with pagination info, status and list of log entries
     * @throws ITParseException - When search parameters are invalid
     */
    public AuditSearchResponse searchAuditLogs(
            String requester,
            AuditSearchBody body,
            boolean clearAccess,
            HttpServletRequest req
    ) throws ITParseException {

        // Apply defaults when body is not provided or fields are unset
        if (body == null) {
            body = new AuditSearchBody();
        }
        int pageSize = (body.getPageSize() <= 0) ? DEFAULT_PAGE_SIZE : Math.min(body.getPageSize(), MAX_PAGE_SIZE);
        int page = Math.max(body.getPage(), 0);

        // Sanitize search filter: empty string is treated as no filter (null)
        String searchFilter = (body.getSearch() != null && !body.getSearch().isBlank()) ? body.getSearch().trim() : null;
        long startMs = body.getStartMs();
        long endMs   = body.getEndMs();

        // Validate date range coherence when both bounds are set
        if (startMs > 0 && endMs > 0 && startMs > endMs) {
            log.warn("[audit] Invalid date range requested by {} - startMs {} > endMs {}", requester, startMs, endMs);
            throw new ITParseException("audit-search-invalid-date-range");
        }

        log.debug("[audit] User {} searching audit logs - search={} page={} pageSize={}",
                requester, searchFilter, page, pageSize);

        AuditSearchResponse response = new AuditSearchResponse();
        response.setPageSize(pageSize);

        // Determine which storage backends are active
        List<String> targets = Tools.getStringListFromParam(auditConfig.getAuditStoreMedium());
        boolean mongoActive = false;
        boolean psqlActive = false;
        for ( String target : targets) {
            switch (AuditService.toAuditTarget(target)) {
                case AUDIT_TARGET_MONGO:
                    mongoActive = true;
                    break;
                case AUDIT_TARGET_PSQL:
                    psqlActive = true;
                    break;
            }
        }

        if (!mongoActive && !psqlActive) {
            // No database backend configured - return empty result with specific status
            log.warn("[audit] Audit search requested by {} but no database backend is configured", requester);
            response.setStatus("audit-log-non-database");
            response.setTotal(0);
            response.setTotalPages(0);
            response.setLogs(new ArrayList<>());
            return response;
        }

        List<AuditResponse> entries = new ArrayList<>();
        long total = 0;

        if (mongoActive) {
            // Normalize optional params: empty regex matches all; Long.MAX_VALUE as upper bound matches all
            String effectiveSearch = (searchFilter == null) ? "" : searchFilter;
            long effectiveEndMs    = (endMs == 0) ? Now.NowUtcMs() : endMs;

            // Delegate to the repository native MongoDB query, sorted by actionMs descending
            PageRequest pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "actionMs"));
            Page<AuditMdb> resultPage = auditRepositoryMdb.searchAuditLogs(
                    effectiveSearch, startMs, effectiveEndMs, pageable);

            total = resultPage.getTotalElements();
            for (AuditMdb a : resultPage.getContent()) {
                entries.add(mapToResponse(a.getService(), a.getAction(), a.getActionMs(),
                        a.getOwner(), a.getLogStr(), a.getParams(), clearAccess));
            }

        } else {
            // PostgreSQL path: use JPA repository with dynamic JPQL query and Pageable
            PageRequest pageable = PageRequest.of(page, pageSize);
            Page<Audit> resultPage = auditRepository.searchAuditLogs(
                    searchFilter, startMs, endMs, pageable);

            total = resultPage.getTotalElements();
            for (Audit a : resultPage.getContent()) {
                entries.add(mapToResponse(a.getService(), a.getAction(), a.getActionMs(),
                        a.getOwner(), a.getLogStr(), a.getParams(), clearAccess));
            }
        }

        // Compute total number of pages
        int totalPages = (pageSize > 0 && total > 0) ? (int) Math.ceil((double) total / pageSize) : 0;

        response.setTotal(total);
        response.setTotalPages(totalPages);
        response.setStatus("ok");
        response.setLogs(entries);

        log.debug("[audit] Audit search by {} returned {}/{} entries (page {}/{})",
                requester, entries.size(), total, page, totalPages);

        return response;
    }

    /**
     * Map raw audit fields to an AuditResponse, resolving the log string parameters
     * according to the caller's access level.
     * ROLE_AUDIT_RD_CLEAR: decrypts and inlines the params into the log string.
     * ROLE_AUDIT_RD: replaces all param placeholders {n} with ***.
     * The linkChain field is always set to true (signature chain not yet implemented).
     *
     * @param service     - service name
     * @param action      - action name
     * @param actionMs    - timestamp in ms
     * @param owner       - owner login
     * @param logStr      - raw log string with {n} placeholders
     * @param params      - list of encrypted parameter values
     * @param clearAccess - true to decrypt params, false to obfuscate with ***
     * @return populated AuditResponse
     */
    private AuditResponse mapToResponse(
            String service,
            String action,
            long actionMs,
            String owner,
            String logStr,
            List<String> params,
            boolean clearAccess
    ) {
        AuditResponse r = new AuditResponse();
        r.setService(service);
        r.setAction(action);
        r.setActionMs(actionMs);
        r.setOwner(owner);
        r.setLinkChain(true);

        // Resolve the log string via AuditIntegration to keep IV and crypto logic centralized
        r.setLogStr(auditIntegration.resolveLogStr(logStr, params, clearAccess));
        return r;
    }

}

















