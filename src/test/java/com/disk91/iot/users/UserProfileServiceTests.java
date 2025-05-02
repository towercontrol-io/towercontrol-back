package com.disk91.iot.users;

import com.disk91.audit.integration.AuditIntegration;
import com.disk91.common.config.CommonConfig;
import com.disk91.common.pdb.entities.Param;
import com.disk91.common.pdb.repositories.ParamRepository;
import com.disk91.common.tools.EmailTools;
import com.disk91.common.tools.Now;
import com.disk91.common.tools.exceptions.ITNotFoundException;
import com.disk91.common.tools.exceptions.ITParseException;
import com.disk91.common.tools.exceptions.ITRightException;
import com.disk91.common.tools.exceptions.ITTooManyException;
import com.disk91.users.api.interfaces.UserAccountCreationBody;
import com.disk91.users.api.interfaces.UserBasicProfileResponse;
import com.disk91.users.config.UserMessages;
import com.disk91.users.config.UsersConfig;
import com.disk91.users.mdb.entities.User;
import com.disk91.users.mdb.entities.UserRegistration;
import com.disk91.users.mdb.repositories.UserRegistrationRepository;
import com.disk91.users.mdb.repositories.UserRepository;
import com.disk91.users.services.UserCache;
import com.disk91.users.services.UserCreationService;
import com.disk91.users.services.UserProfileService;
import com.disk91.users.services.UsersRolesCache;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.PropertySource;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
@PropertySource(value = {"file:configuration/common-test.properties"}, ignoreResourceNotFound = true)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserProfileServiceTests {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Mock
    private UserCache userCache;

    // All the @Autowired are required here...
    @Mock
    private CommonConfig commonConfig;

    @Mock
    private UsersConfig usersConfig;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditIntegration auditIntegration;

    @Mock
    private EmailTools emailTools;

    @Mock
    private UserMessages userMessages;

    @Mock
    private ParamRepository paramRepository;

    @Mock
    private UserCreationService userCreationService;

    @InjectMocks
    private UserProfileService userProfileService;

    // Common variables
    private static User johnDoe;
    private static User admin;
    private static User aliceBob;
    private static String testKey = "12345678901234561234567890123456";

    @BeforeAll
    public static void init () {
        assertDoesNotThrow(() -> {
            johnDoe = new User();
            johnDoe.setKeys(testKey, testKey);
            johnDoe.setEncLogin("john.doe@foo.bar");
            johnDoe.changePassword("abcd1234", true);
            johnDoe.setEncEmail("john.doe@foo.bar");
            johnDoe.setLocked(false);
            johnDoe.setActive(true);
            johnDoe.setApiAccount(false);
            johnDoe.setConditionValidation(true);
            johnDoe.setEncProfileFirstName("john");
            johnDoe.setEncProfileLastName("doe");
            johnDoe.setLanguage("en");
            johnDoe.getRoles().add(UsersRolesCache.StandardRoles.ROLE_REGISTERED_USER.getRoleName());

            admin = new User();
            admin.setKeys(testKey, testKey);
            admin.setEncLogin("master@foo.bar");
            admin.changePassword("abcd1234", true);
            admin.setEncEmail("master@foo.bar");
            admin.setLocked(false);
            admin.setActive(true);
            admin.setApiAccount(false);
            admin.setConditionValidation(true);
            admin.getRoles().add(UsersRolesCache.StandardRoles.ROLE_REGISTERED_USER.getRoleName());
            admin.getRoles().add(UsersRolesCache.StandardRoles.ROLE_GOD_ADMIN.getRoleName());

            aliceBob = new User();
            aliceBob.setKeys(testKey, testKey);
            aliceBob.setEncLogin("alice.bob@foo.bar");
            aliceBob.changePassword("abcd1234", true);
            aliceBob.setEncEmail("alice.bob@foo.bar");
            aliceBob.setLocked(false);
            aliceBob.setActive(true);
            aliceBob.setApiAccount(false);
            aliceBob.setConditionValidation(true);
            aliceBob.getRoles().add(UsersRolesCache.StandardRoles.ROLE_REGISTERED_USER.getRoleName());

        });
    }

    @Test
    @Order(1)
    public void testGetMyUserBasicProfile() throws ITNotFoundException {
        log.info("[users][Profile][test] getMyUserBasicProfile");
        doNothing().when(auditIntegration).auditLog(any(),any(),any(), any(), any());

        given(userCache.getUser(johnDoe.getLogin())).willReturn(johnDoe);
        given(userCache.getUser(admin.getLogin())).willReturn(admin);
        given(userCache.getUser(aliceBob.getLogin())).willReturn(aliceBob);
        given(commonConfig.getEncryptionKey()).willReturn(testKey);
        given(commonConfig.getApplicationKey()).willReturn(testKey);

        // Request ourself, no problem
        log.info("[users][Profile][test] self access");
        assertDoesNotThrow(() -> {
            UserBasicProfileResponse r = userProfileService.getMyUserBasicProfile(johnDoe.getLogin(), johnDoe.getLogin());
            assertEquals("john.doe@foo.bar", r.getEmail());
            assertEquals("john",r.getFirstName());
            assertEquals("doe",r.getLastName());
            assertEquals(johnDoe.getLogin(),r.getLogin());
            assertEquals(1,r.getRoles().size());
            assertEquals(0, r.getAcls().size());
        });

        // Request alice.bob must fail
        log.info("[users][Profile][test] Normal user may not access another user");
        assertThrows(ITRightException.class, () -> {
            UserBasicProfileResponse r = userProfileService.getMyUserBasicProfile(johnDoe.getLogin(), aliceBob.getLogin());
        });

        // Admin may access normal user
        log.info("[users][Profile][test] Normal user may not access another user");
        assertDoesNotThrow(() -> {
            UserBasicProfileResponse r = userProfileService.getMyUserBasicProfile(admin.getLogin(), johnDoe.getLogin());
            assertEquals("john.doe@foo.bar", r.getEmail());
            assertEquals("john",r.getFirstName());
            assertEquals("doe",r.getLastName());
            assertEquals(johnDoe.getLogin(),r.getLogin());
            assertEquals(1,r.getRoles().size());
            assertEquals(0, r.getAcls().size());
        });


    }


}
