package com.disk91.iot.common.tools;

import com.disk91.common.config.CommonConfig;
import com.disk91.common.services.EncryptionService;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.PropertySource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@PropertySource(value = {"file:configuration/common-test.properties"}, ignoreResourceNotFound = true)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EncryptionTests {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Mock
    private CommonConfig commonConfig;

    @InjectMocks
    private EncryptionService encryptionService;

    @Test
    public void testDecrypt() {
        given(commonConfig.getEncryptionKey()).willReturn("");
        assertDoesNotThrow(() -> {
            String r = encryptionService.decryptStringWithServerKey("");
            log.info("Decrypted: {}", r);
        });
    }

}
