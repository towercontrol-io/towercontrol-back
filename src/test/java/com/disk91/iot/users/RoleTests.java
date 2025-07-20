package com.disk91.iot.users;

import com.disk91.common.tools.exceptions.ITParseException;
import com.disk91.common.tools.exceptions.ITTooManyException;
import com.disk91.users.config.UsersConfig;
import com.disk91.users.mdb.entities.Role;
import com.disk91.users.mdb.repositories.RolesRepository;
import com.disk91.users.services.UsersRolesCache;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;

//@SpringBootTest(classes = IotApplication.class)
@Import(UsersConfig.class)
@EnableAutoConfiguration(exclude={MongoAutoConfiguration.class, MongoDataAutoConfiguration.class})
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@PropertySource(value = {"file:configuration/common-test.properties"}, ignoreResourceNotFound = true)
@AutoConfigureEmbeddedDatabase
public class RoleTests {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Mock
    private RolesRepository rolesRepository;

    @InjectMocks
    private UsersRolesCache usersRolesCache;

    //@Autowired
    @InjectMocks
    private UsersConfig usersConfig;

    /**
     * When the userRoleCache Service is getting started, the roles are automatically loaded in the cache
     */
    @Test
    public void testInitialization() {
        log.info("[users][test] Running testInitialization");

        // setup the mock to return null when the findAll method is called
        given(rolesRepository.findAll()).willReturn(new ArrayList<Role>());

        // Because @PostConstruct is not called by the test, we need to call it manually
        usersRolesCache.initRolesCache();

        // verify we have loaded the platform roles
        assertEquals(usersRolesCache.__countPfRoles(),usersRolesCache.__countRolesInCache());

        // query role ROLE_GOD_ADMIN, verify it is existing and corresponding to what is expected
        assertDoesNotThrow(() -> {
            Role godAdmin = usersRolesCache.getRole("ROLE_GOD_ADMIN");
            assertEquals("system",godAdmin.getCreationBy());
        });

    }

    @Test
    protected void testRoleCreation() {
        log.info("[users][test] Running testRoleCreation");

        // setup the mock to return null when the findAll method is called
        given(rolesRepository.findAll()).willReturn(new ArrayList<Role>());

        // Because @PostConstruct is not called by the test, we need to call it manually
        usersRolesCache.initRolesCache();

        // Make sur the configuration is good for test
        //assertEquals("db", usersConfig.getUsersIntracomMedium());

        // Make sure role creation fails when the description is not respecting the
        // lower-case format.
        assertThrows(ITParseException.class, () -> {
            usersRolesCache.addRole("ROLE_TEST", "Test Role", "Test Role Description", "test");
        });

        // Make sure role creation fails when the role already exists
        assertThrows(ITTooManyException.class, () -> {
            usersRolesCache.addRole("ROLE_GOD_ADMIN", "role-duplication-test", "Role duplication test", "test");
        });


        // add one role into the cache, this will create a new entry in the database and also in the cache


        //Role role = new Role();
        //Mockito.when(rolesRepository.save(any(Role.class))).thenReturn(role);

    }

}
