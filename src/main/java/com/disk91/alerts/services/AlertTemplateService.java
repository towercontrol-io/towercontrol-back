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
package com.disk91.alerts.services;

import com.disk91.alerts.api.interfaces.AlertTemplateListResponseItf;
import com.disk91.alerts.api.interfaces.AlertTemplateResponseItf;
import com.disk91.alerts.api.interfaces.AlertTemplateUpsertBody;
import com.disk91.alerts.config.ActionCatalog;
import com.disk91.alerts.mdb.entities.AlertTemplate;
import com.disk91.alerts.mdb.entities.sub.AlertBehavior;
import com.disk91.alerts.mdb.entities.sub.AlertLocaleMessage;
import com.disk91.alerts.mdb.entities.sub.AlertMedium;
import com.disk91.alerts.mdb.entities.sub.AlertMediumMessage;
import com.disk91.alerts.mdb.repositories.AlertTemplateRepository;
import com.disk91.audit.integration.AuditIntegration;
import com.disk91.common.config.ModuleCatalog;
import com.disk91.common.tools.RandomString;
import com.disk91.common.tools.exceptions.ITNotFoundException;
import com.disk91.common.tools.exceptions.ITParseException;
import com.disk91.common.tools.exceptions.ITRightException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AlertTemplateService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected AlertTemplateRepository alertTemplateRepository;

    @Autowired
    protected AlertTemplateCache alertTemplateCache;

    @Autowired
    protected AuditIntegration auditIntegration;

    // ================================================================================================================

    /**
     * Create or update an alert template, validating all required fields.
     * When body.getShortId() is set the request is an update; ownership is enforced.
     * Only ROLE_ALERTS_ADMIN can create or update global templates.
     * ROLE_ALERTS_TEMPLATE can only modify their own templates.
     * @param requester - login of the requesting user
     * @param isAdmin   - true when the requester has ROLE_ALERTS_ADMIN
     * @param body      - upsert body
     * @param req       - HTTP request for IP tracing
     * @return the persisted template as a response object
     * @throws ITParseException    when the body is incomplete or contains invalid data
     * @throws ITRightException    when the requester does not have the right to perform the operation
     * @throws ITNotFoundException when the template to update does not exist
     */
    public AlertTemplateResponseItf upsertAlertTemplate(
            String requester,
            boolean isAdmin,
            AlertTemplateUpsertBody body,
            HttpServletRequest req
    ) throws ITParseException, ITRightException, ITNotFoundException {

        // Only ROLE_ALERTS_ADMIN can create or update a global template
        if (body.isGlobal() && !isAdmin) {
            log.warn("[alerts] User {} attempted to create a global template without ROLE_ALERTS_ADMIN", requester);
            throw new ITRightException("alerts-template-global-forbidden");
        }

        // Validate name
        if (body.getName() == null || body.getName().isBlank()) {
            throw new ITParseException("alerts-template-name-required");
        }
        if (body.getName().length() > 100) {
            throw new ITParseException("alerts-template-name-too-long");
        }

        // Validate description length when provided
        if (body.getDescription() != null && body.getDescription().length() > 500) {
            throw new ITParseException("alerts-template-description-too-long");
        }

        // Validate behavior
        if (body.getBehavior() == null || body.getBehavior() == AlertBehavior.UNKNOWN) {
            throw new ITParseException("alerts-template-behavior-required");
        }

        // Validate retry fields (only meaningful in FIRE_TO_END mode, but must be non-negative)
        if (body.getRetryTimes() < 0) {
            throw new ITParseException("alerts-template-retry-times-invalid");
        }
        if (body.getRetryMs() < 0) {
            throw new ITParseException("alerts-template-retry-ms-invalid");
        }

        // Validate that at least one open locale is provided with at least one medium message
        if (body.getOpen() == null || body.getOpen().isEmpty()) {
            throw new ITParseException("alerts-template-open-required");
        }
        for (AlertLocaleMessage locale : body.getOpen()) {
            if (locale.getLocale() == null || locale.getLocale().isBlank()) {
                throw new ITParseException("alerts-template-locale-required");
            }
            if (locale.getMediums() == null || locale.getMediums().isEmpty()) {
                throw new ITParseException("alerts-template-medium-required");
            }
            for (AlertMediumMessage mm : locale.getMediums()) {
                if (mm.getMessage() == null || mm.getMessage().isBlank()) {
                    throw new ITParseException("alerts-template-message-required");
                }
                if ( (mm.getTitle() == null || mm.getTitle().isBlank())
                    && ( mm.getMedium() == AlertMedium.EMAIL || mm.getMedium() == AlertMedium.PUSH || mm.getMedium() == AlertMedium.DEFAULT )
                ) {
                    throw new ITParseException("alerts-template-message-title-required");
                }
            }
        }

        boolean isUpdate = body.getShortId() != null && !body.getShortId().isBlank();
        AlertTemplate template;

        if (isUpdate) {
            // Load the existing template from cache (falls back to DB on miss)
            try {
                template = alertTemplateCache.getTemplateByShortId(body.getShortId());
            } catch (ITNotFoundException e) {
                log.warn("[alerts] Template {} not found for update requested by {}", body.getShortId(), requester);
                throw new ITNotFoundException("alerts-template-not-found");
            }

            // Non-admin can only update their own templates
            if (!isAdmin && !template.getOwner().equals(requester)) {
                log.warn("[alerts] User {} attempted to update template {} owned by {}",
                        requester, template.getShortId(), template.getOwner());
                throw new ITRightException("alerts-template-update-forbidden");
            }

            // Apply updates
            template.setName(body.getName());
            template.setDescription(body.getDescription() != null ? body.getDescription() : "");
            template.setGlobal(body.isGlobal());
            template.setParameters(body.getParameters() != null ? body.getParameters() : new ArrayList<>());
            template.setOpen(body.getOpen());
            template.setClose(body.getClose() != null ? body.getClose() : new ArrayList<>());
            template.setBehavior(body.getBehavior());
            template.setPreferred(body.getPreferred() != null ? body.getPreferred() : new ArrayList<>());
            template.setDurationMs(body.getDurationMs());
            template.setCriticality(body.getCriticality());
            // retryTimes and retryMs are only meaningful in FIRE_TO_END mode; zero them out otherwise
            boolean isFireToEnd = body.getBehavior() == AlertBehavior.FIRE_TO_END;
            template.setRetryTimes(isFireToEnd ? body.getRetryTimes() : 0);
            template.setRetryMs(isFireToEnd ? body.getRetryMs() : 0);

            // Audit log the update event
            auditIntegration.auditLog(
                    ModuleCatalog.Modules.ALERTS,
                    ActionCatalog.getActionName(ActionCatalog.Actions.AUDIT_TEMPLATE_UPDATE),
                    requester,
                    "Template {0} '{1}' updated (global={2})",
                    new String[]{template.getShortId(), template.getName(), String.valueOf(template.isGlobal())}
            );
        } else {
            // Build a new template
            template = AlertTemplate.newAlertTemplate(
                    body.getName(),
                    body.getDescription() != null ? body.getDescription() : "",
                    requester,
                    body.isGlobal(),
                    body.getBehavior()
            );
            template.setParameters(body.getParameters() != null ? body.getParameters() : new ArrayList<>());
            template.setOpen(body.getOpen());
            template.setClose(body.getClose() != null ? body.getClose() : new ArrayList<>());
            template.setPreferred(body.getPreferred() != null ? body.getPreferred() : new ArrayList<>());
            template.setDurationMs(body.getDurationMs());
            template.setCriticality(body.getCriticality());
            // retryTimes and retryMs are only meaningful in FIRE_TO_END mode; zero them out otherwise
            boolean isFireToEnd = body.getBehavior() == AlertBehavior.FIRE_TO_END;
            template.setRetryTimes(isFireToEnd ? body.getRetryTimes() : 0);
            template.setRetryMs(isFireToEnd ? body.getRetryMs() : 0);

            // Generate a unique shortId, retry up to 10 times in case of collision
            String shortId = null;
            for (int attempt = 0; attempt < 10; attempt++) {
                String candidate = RandomString.getRandomString(6);
                if (alertTemplateRepository.findOneAlertTemplateByShortId(candidate) == null) {
                    shortId = candidate;
                    break;
                }
            }
            if (shortId == null) {
                log.error("[alerts] Failed to generate a unique shortId for new template after 10 attempts");
                throw new ITParseException("alerts-template-shortid-generation-failed");
            }
            template.setShortId(shortId);

            // Audit log the creation event
            auditIntegration.auditLog(
                    ModuleCatalog.Modules.ALERTS,
                    ActionCatalog.getActionName(ActionCatalog.Actions.AUDIT_TEMPLATE_CREATION),
                    requester,
                    "Template '{0}' created (global={1})",
                    new String[]{template.getName(), String.valueOf(template.isGlobal())}
            );
        }

        // Persist and invalidate cache entry on update
        template = alertTemplateCache.saveTemplate(template);

        AlertTemplateResponseItf response = new AlertTemplateResponseItf();
        response.buildFrom(template);
        return response;
    }

    // ================================================================================================================

    /**
     * Delete an alert template by shortId, enforcing ownership rules.
     * ROLE_ALERTS_ADMIN can delete any template; ROLE_ALERTS_TEMPLATE only their own.
     * @param requester - login of the requesting user
     * @param isAdmin   - true when the requester has ROLE_ALERTS_ADMIN
     * @param shortId   - short functional identifier of the template to delete
     * @param req       - HTTP request for IP tracing
     * @throws ITNotFoundException when the template does not exist
     * @throws ITRightException    when the requester does not own the template and is not admin
     */
    public void deleteAlertTemplate(
            String requester,
            boolean isAdmin,
            String shortId,
            HttpServletRequest req
    ) throws ITNotFoundException, ITRightException {

        // Load the existing template from cache (falls back to DB on miss)
        AlertTemplate template;
        try {
            template = alertTemplateCache.getTemplateByShortId(shortId);
        } catch (ITNotFoundException e) {
            log.warn("[alerts] Template {} not found for deletion requested by {}", shortId, requester);
            throw new ITNotFoundException("alerts-template-not-found");
        }

        // Non-admin can only delete their own templates
        if (!isAdmin && !template.getOwner().equals(requester)) {
            log.warn("[alerts] User {} attempted to delete template {} owned by {}",
                    requester, shortId, template.getOwner());
            throw new ITRightException("alerts-template-delete-forbidden");
        }

        // Evict from cache before deleting from database
        alertTemplateCache.flushTemplate(shortId);
        alertTemplateRepository.deleteById(template.getId());

        // Audit log the deletion event
        auditIntegration.auditLog(
                ModuleCatalog.Modules.ALERTS,
                ActionCatalog.getActionName(ActionCatalog.Actions.AUDIT_TEMPLATE_DELETE),
                requester,
                "Template {0} '{1}' deleted",
                new String[]{shortId, template.getName()}
        );
    }

    // ================================================================================================================

    /**
     * List alert templates visible to the requesting user: templates they own plus all global ones.
     * An optional search string applies a case-insensitive filter on the template name.
     * @param requester - login of the requesting user
     * @param search    - optional name filter; null or blank means no filter
     * @param req       - HTTP request for IP tracing
     * @return list response with templates and total count
     */
    public AlertTemplateListResponseItf listAlertTemplates(
            String requester,
            String search,
            HttpServletRequest req
    ) {
        // Retrieve templates visible to this user (owned or global)
        List<AlertTemplate> templates = alertTemplateRepository.findTemplatesByOwnerOrGlobal(requester);

        // Apply optional name filter (in-memory after query to support mixed source)
        boolean hasSearch = search != null && !search.isBlank();
        String lowerSearch = hasSearch ? search.toLowerCase() : null;

        ArrayList<AlertTemplateResponseItf> result = new ArrayList<>();
        for (AlertTemplate t : templates) {
            if (hasSearch && !t.getName().toLowerCase().contains(lowerSearch)) {
                continue;
            }
            AlertTemplateResponseItf r = new AlertTemplateResponseItf();
            r.buildFrom(t);
            result.add(r);
        }

        AlertTemplateListResponseItf response = new AlertTemplateListResponseItf();
        response.setTemplates(result);
        response.setTotal(result.size());
        return response;
    }
}
