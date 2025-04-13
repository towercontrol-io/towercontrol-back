package com.disk91.iot.audit;

import com.disk91.audit.config.AuditConfig;
import com.disk91.audit.integration.AuditIntegration;
import com.disk91.audit.integration.AuditMessage;
import com.disk91.common.config.CommonConfig;
import com.disk91.common.config.ModuleCatalog;
import com.disk91.common.tools.EncryptionHelper;
import com.disk91.common.tools.Now;
import com.disk91.integration.services.IntegrationService;
import com.disk91.users.config.ActionCatalog;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.PropertySource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;


@ExtendWith(MockitoExtension.class)
@PropertySource(value = {"file:configuration/common-test.properties"}, ignoreResourceNotFound = true)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuditMessageTests {

    private final Logger log = LoggerFactory.getLogger(this.getClass());


    // All the @Autowired are required here...

    @Mock
    private CommonConfig commonConfig;

    @Mock
    private AuditConfig auditConfig;

    @Mock
    private EncryptionHelper encryptionHelper;

    @Mock
    private IntegrationService integrationService;

    @InjectMocks
    private AuditIntegration auditIntegration;


    @Test
    @Order(1)
    public void testAuditLogWithEncryption() {


        given(commonConfig.getEncryptionKey()).willReturn("12345678901234567890123456789012");
        given(encryptionHelper.encrypt(anyString(), anyString(), anyString())).willCallRealMethod();
        AuditMessage auditMessage = auditIntegration.creatNewAuditMessage(
                ModuleCatalog.Modules.USERS,
                ActionCatalog.getActionName(ActionCatalog.Actions.REGISTRATION),
                "user1 {0} registered from IP {1}",
                new String[]{"john.doe@goo.bar", "1.1.1.1"}
        );

        log.info("[audit][Encryption][test] verify encryption");
        given(auditConfig.isAuditLogsDecryptionEnabled()).willReturn(false);
        assertEquals(Now.formatToYYYYMMDDHHMMSSUtc(auditMessage.getActionMs())+" [users] [registration] user1 Cr81tEYU94nW6EUwAg8fRw== registered from IP vP52cpib5C6uiv22dZu3UA==",auditIntegration.toString(auditMessage));

        log.info("[audit][Encryption][test] verify decryption");
        given(auditConfig.isAuditLogsDecryptionEnabled()).willReturn(true);
        given(encryptionHelper.decrypt(anyString(), anyString(), anyString())).willCallRealMethod();
        assertEquals(Now.formatToYYYYMMDDHHMMSSUtc(auditMessage.getActionMs())+" [users] [registration] user1 john.doe@goo.bar registered from IP 1.1.1.1",auditIntegration.toString(auditMessage));

        log.info("[audit][Encryption][test] test null params");
        auditMessage = auditIntegration.creatNewAuditMessage(
                ModuleCatalog.Modules.USERS,
                ActionCatalog.getActionName(ActionCatalog.Actions.REGISTRATION),
                "user1 registered from IP",
                null
        );

        given(auditConfig.isAuditLogsDecryptionEnabled()).willReturn(false);
        assertEquals(Now.formatToYYYYMMDDHHMMSSUtc(auditMessage.getActionMs())+" [users] [registration] user1 registered from IP",auditIntegration.toString(auditMessage));

        given(auditConfig.isAuditLogsDecryptionEnabled()).willReturn(true);
        assertEquals(Now.formatToYYYYMMDDHHMMSSUtc(auditMessage.getActionMs())+" [users] [registration] user1 registered from IP",auditIntegration.toString(auditMessage));

        log.info("[audit][Encryption][test] test no params");
        auditMessage = auditIntegration.creatNewAuditMessage(
                ModuleCatalog.Modules.USERS,
                ActionCatalog.getActionName(ActionCatalog.Actions.REGISTRATION),
                "user1 registered from IP",
                new String[0]
        );

        given(auditConfig.isAuditLogsDecryptionEnabled()).willReturn(false);
        assertEquals(Now.formatToYYYYMMDDHHMMSSUtc(auditMessage.getActionMs())+" [users] [registration] user1 registered from IP",auditIntegration.toString(auditMessage));

        given(auditConfig.isAuditLogsDecryptionEnabled()).willReturn(true);
        assertEquals(Now.formatToYYYYMMDDHHMMSSUtc(auditMessage.getActionMs())+" [users] [registration] user1 registered from IP",auditIntegration.toString(auditMessage));

        log.info("[audit][Encryption][test] unused param");
        auditMessage = auditIntegration.creatNewAuditMessage(
                ModuleCatalog.Modules.USERS,
                ActionCatalog.getActionName(ActionCatalog.Actions.REGISTRATION),
                "user1 {0} registered from IP {1}",
                new String[]{"john.doe@goo.bar", "1.1.1.1", "unused"}
        );

        given(auditConfig.isAuditLogsDecryptionEnabled()).willReturn(false);
        assertEquals(Now.formatToYYYYMMDDHHMMSSUtc(auditMessage.getActionMs())+" [users] [registration] user1 Cr81tEYU94nW6EUwAg8fRw== registered from IP vP52cpib5C6uiv22dZu3UA==",auditIntegration.toString(auditMessage));

        given(auditConfig.isAuditLogsDecryptionEnabled()).willReturn(true);
        assertEquals(Now.formatToYYYYMMDDHHMMSSUtc(auditMessage.getActionMs())+" [users] [registration] user1 john.doe@goo.bar registered from IP 1.1.1.1",auditIntegration.toString(auditMessage));

        log.info("[audit][Encryption][test] missing param");
        auditMessage = auditIntegration.creatNewAuditMessage(
                ModuleCatalog.Modules.USERS,
                ActionCatalog.getActionName(ActionCatalog.Actions.REGISTRATION),
                "user1 {0} registered from IP {3}",
                new String[]{"john.doe@goo.bar", "1.1.1.1", "unused"}
        );

        given(auditConfig.isAuditLogsDecryptionEnabled()).willReturn(false);
        assertEquals(Now.formatToYYYYMMDDHHMMSSUtc(auditMessage.getActionMs())+" [users] [registration] user1 Cr81tEYU94nW6EUwAg8fRw== registered from IP {3}",auditIntegration.toString(auditMessage));

        given(auditConfig.isAuditLogsDecryptionEnabled()).willReturn(true);
        assertEquals(Now.formatToYYYYMMDDHHMMSSUtc(auditMessage.getActionMs())+" [users] [registration] user1 john.doe@goo.bar registered from IP {3}",auditIntegration.toString(auditMessage));

    }

}
