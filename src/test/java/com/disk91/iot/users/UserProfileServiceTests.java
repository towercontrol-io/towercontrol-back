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
            johnDoe.addRole(UsersRolesCache.StandardRoles.ROLE_REGISTERED_USER);

            admin = new User();
            admin.setKeys(testKey, testKey);
            admin.setEncLogin("master@foo.bar");
            admin.changePassword("abcd1234", true);
            admin.setEncEmail("master@foo.bar");
            admin.setLocked(false);
            admin.setActive(true);
            admin.setApiAccount(false);
            admin.setConditionValidation(true);
            admin.addRole(UsersRolesCache.StandardRoles.ROLE_REGISTERED_USER);
            admin.addRole(UsersRolesCache.StandardRoles.ROLE_GOD_ADMIN);

            aliceBob = new User();
            aliceBob.setKeys(testKey, testKey);
            aliceBob.setEncLogin("alice.bob@foo.bar");
            aliceBob.changePassword("abcd1234", true);
            aliceBob.setEncEmail("alice.bob@foo.bar");
            aliceBob.setLocked(false);
            aliceBob.setActive(true);
            aliceBob.setApiAccount(false);
            aliceBob.setConditionValidation(true);
            aliceBob.addRole(UsersRolesCache.StandardRoles.ROLE_REGISTERED_USER);

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
        log.info("[users][Profile][test] Admin user may access another user");
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

    @Test
    @Order(2)
    public void testuserConditionAcceptation() throws ITNotFoundException {
        log.info("[users][Profile][test] userConditionAcceptation");
        doNothing().when(auditIntegration).auditLog(any(),any(),any(), any(), any());
        doNothing().when(userCache).saveUser(any());

        given(userCache.getUser(johnDoe.getLogin())).willReturn(johnDoe);
        given(commonConfig.getEncryptionKey()).willReturn(testKey);
        given(commonConfig.getApplicationKey()).willReturn(testKey);
        Param p = new Param();
        p.setParamKey("users.condition.version");
        p.setStringValue("1234");
        given(paramRepository.findByParamKey("users.condition.version")).willReturn(p);

        // Validate for ourself, make sure structure has been updated with good date & version
        log.info("[users][Profile][test] self accept user condition");
        assertDoesNotThrow(() -> {
            userProfileService.userConditionAcceptation(johnDoe.getLogin(), johnDoe.getLogin(), null);
            assertTrue(johnDoe.isConditionValidation());
            assertEquals("1234",johnDoe.getConditionValidationVer());
            assertTrue((Now.NowUtcMs() - johnDoe.getConditionValidationDate()) < 1000);
        });

    }


    @Test
    @Order(3)
    public void testuserPasswordChange() throws ITNotFoundException {
        log.info("[users][Profile][test] userPasswordChange");
        doNothing().when(auditIntegration).auditLog(any(),any(),any(), any(), any());

        given(userCache.getUser(johnDoe.getLogin())).willReturn(johnDoe);
        given(userCache.getUser(admin.getLogin())).willReturn(admin);
        lenient().when(userCache.getUser(aliceBob.getLogin())).thenReturn(aliceBob);
        given(commonConfig.getEncryptionKey()).willReturn(testKey);
        given(commonConfig.getApplicationKey()).willReturn(testKey);

        given(userCreationService.verifyPassword(any())).willReturn(true);
        given(usersConfig.getUsersPasswordExpirationDays()).willReturn(2);

        // Request ourself, no problem
        log.info("[users][Profile][test] self password change");
        assertDoesNotThrow(() -> {
            userProfileService.userPasswordChange(johnDoe.getLogin(), johnDoe.getLogin(), "1234abcd");
            johnDoe.setKeys(testKey, testKey);
            assertEquals("john.doe@foo.bar", johnDoe.getEncEmail());
            assertTrue((johnDoe.getExpiredPassword()-Now.NowUtcMs()) > (2*Now.ONE_FULL_DAY-1000) );
            assertTrue((johnDoe.getExpiredPassword()-Now.NowUtcMs()) < (2*Now.ONE_FULL_DAY+1000) );
        });

        // Request alice.bob must fail
        log.info("[users][Profile][test] Normal user may not access another user");
        assertThrows(ITRightException.class, () -> {
            userProfileService.userPasswordChange(johnDoe.getLogin(), aliceBob.getLogin(), "1234abcd");
        });

        // Admin may access normal user
        log.info("[users][Profile][test] Admin user may access another user");
        given(usersConfig.getUsersPasswordExpirationDays()).willReturn(5);
        assertDoesNotThrow(() -> {
            userProfileService.userPasswordChange(admin.getLogin(), johnDoe.getLogin(), "12abcd34");
            johnDoe.setKeys(testKey, testKey);
            assertEquals("john.doe@foo.bar", johnDoe.getEncEmail());
            assertTrue((johnDoe.getExpiredPassword()-Now.NowUtcMs()) > (5*Now.ONE_FULL_DAY-1000) );
            assertTrue((johnDoe.getExpiredPassword()-Now.NowUtcMs()) < (5*Now.ONE_FULL_DAY+1000) );
        });
    }


}
