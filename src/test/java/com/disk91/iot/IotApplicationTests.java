package com.disk91.iot;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ActiveProfiles;


@SpringBootTest(classes = IotApplication.class)
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude={MongoAutoConfiguration.class, MongoDataAutoConfiguration.class})
@ExtendWith(MockitoExtension.class)
@PropertySource(value = {"file:configuration/common-test.properties"}, ignoreResourceNotFound = true)
@AutoConfigureEmbeddedDatabase
public class IotApplicationTests {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Test
    public void testIotApplicationDisplay() {
        log.info("[Common][test] Running testEmptyTest");

    }

}


