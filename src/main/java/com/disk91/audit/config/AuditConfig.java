package com.disk91.audit.config;

import com.disk91.audit.services.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value = {"file:configuration/audit.properties"}, ignoreResourceNotFound = true)
public class AuditConfig {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    // ----------------------------------------------
    // Integration setup
    // ----------------------------------------------

    // How the audit service is connected to the different service instances
    @Value("${audit.integration.medium:memory}")
    protected String auditIntegrationMedium;
    public String getAuditIntegrationMedium() {
        return auditIntegrationMedium;
    }

    @Value("${audit.integration.timeout.ms:10000}")
    protected long auditIntegrationTimeout;
    public long getAuditIntegrationTimeout() {
        return auditIntegrationTimeout;
    }


    // ----------------------------------------------
    // Audit logs configuration
    // ----------------------------------------------
    @Value("${audit.store.medium:logs}")
    protected String auditStoreMedium;
    public String getAuditStoreMedium() {
        return auditStoreMedium;
    }

    public AuditService.AuditTarget getAuditStoreTarget() {
        switch (auditStoreMedium) {
            default:
            case "logs":
                return AuditService.AuditTarget.AUDIT_TARGET_LOGS;
            case "files":
                return AuditService.AuditTarget.AUDIT_TARGET_FILES;
            case "mongodb":
                return AuditService.AuditTarget.AUDIT_TARGET_MONGO;
            case "psql":
                return AuditService.AuditTarget.AUDIT_TARGET_PSQL;
        }
    }


    @Value("${audit.logs.decryption.enabled:false}")
    protected boolean auditLogsDecryptionEnabled;
    public boolean isAuditLogsDecryptionEnabled() {
        return auditLogsDecryptionEnabled;
    }

}
