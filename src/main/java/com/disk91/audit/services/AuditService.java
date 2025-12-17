/*
 * Copyright (c) - Paul Pinault (aka disk91) - 2024.
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
import com.disk91.audit.config.AuditConfig;
import com.disk91.audit.integration.AuditIntegration;
import com.disk91.audit.integration.AuditMessage;
import com.disk91.common.api.interfaces.ActionResult;
import com.disk91.common.config.ModuleCatalog;
import com.disk91.common.tools.Now;
import com.disk91.common.tools.exceptions.ITNotFoundException;
import com.disk91.common.tools.exceptions.ITOverQuotaException;
import com.disk91.common.tools.exceptions.ITParseException;
import com.disk91.common.tools.exceptions.ITTooManyException;
import com.disk91.integration.api.interfaces.IntegrationCallback;
import com.disk91.integration.api.interfaces.IntegrationQuery;
import com.disk91.integration.services.IntegrationService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public enum AuditTarget {
        AUDIT_TARGET_LOGS,
        AUDIT_TARGET_FILES,
        AUDIT_TARGET_MONGO,
        AUDIT_TARGET_PSQL
    }

    @Autowired
    protected AuditConfig auditConfig;

    @Autowired
    protected IntegrationService integrationService;

    @Autowired
    protected AuditIntegration auditIntegration;

    @PostConstruct
    public void init() {
        log.info("[audit] Service initialized, target: {}", auditConfig.getAuditStoreTarget().name());
        try {
            integrationService.registerCallback(
                    ModuleCatalog.Modules.AUDIT,
                    new IntegrationCallback() {
                        @Override
                        public void onIntegrationEvent(IntegrationQuery q) {
                            AuditMessage auditMessage = (AuditMessage) q.getQuery();
                            switch (auditConfig.getAuditStoreTarget()) {
                                case AUDIT_TARGET_LOGS:
                                    log.info("[audit] {}", auditIntegration.toString(auditMessage));
                                    break;
                                case AUDIT_TARGET_FILES:
                                    break;
                                case AUDIT_TARGET_MONGO:
                                    break;
                                case AUDIT_TARGET_PSQL:
                                    break;
                            }

                            // terminate the action
                            q.setResponse(ActionResult.OK("Audit logged")); // fire & forget, success on every actions
                            q.setResult(null);
                            q.setState(IntegrationQuery.QueryState.STATE_DONE);
                            q.setResponse_ts(Now.NanoTime());

                        }
                    }
            );
        } catch (ITParseException | ITTooManyException x) {
            log.error("[audit] Failed to register audit integration callback: {}", x.getMessage());
        }
    }

}
