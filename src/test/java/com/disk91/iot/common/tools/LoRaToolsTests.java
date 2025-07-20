package com.disk91.iot.common.tools;

import com.disk91.common.tools.LoRaTools;
import com.disk91.users.config.UsersConfig;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ActiveProfiles;

@EnableAutoConfiguration(exclude={MongoAutoConfiguration.class, MongoDataAutoConfiguration.class})
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@PropertySource(value = {"file:configuration/common-test.properties"}, ignoreResourceNotFound = true)
@AutoConfigureEmbeddedDatabase
public class LoRaToolsTests {

    private final Logger log = LoggerFactory.getLogger(this.getClass());


    @Test
    public void testInitialization() {
        log.info("[users][test] Running testInitialization");

        String eui = LoRaTools.getRandomDevEui("1234");
        log.info("Random DevEui : {}",eui);

    }
}
