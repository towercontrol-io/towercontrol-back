package com.disk91.iot.users;

import com.disk91.audit.integration.AuditIntegration;
import com.disk91.common.config.CommonConfig;
import com.disk91.common.pdb.entities.Param;
import com.disk91.common.pdb.repositories.ParamRepository;
import com.disk91.common.tools.EmailTools;
import com.disk91.common.tools.EncryptionHelper;
import com.disk91.common.tools.Now;
import com.disk91.common.tools.exceptions.ITNotFoundException;
import com.disk91.common.tools.exceptions.ITParseException;
import com.disk91.common.tools.exceptions.ITRightException;
import com.disk91.common.tools.exceptions.ITTooManyException;
import com.disk91.users.api.interfaces.UserAccountCreationBody;
import com.disk91.users.api.interfaces.UserAccountRegistrationBody;
import com.disk91.users.config.UserMessages;
import com.disk91.users.config.UsersConfig;
import com.disk91.users.mdb.entities.User;
import com.disk91.users.mdb.entities.UserRegistration;
import com.disk91.users.mdb.repositories.UserRegistrationRepository;
import com.disk91.users.mdb.repositories.UserRepository;
import com.disk91.users.services.UserCreationService;
import com.disk91.users.services.UserRegistrationService;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
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
public class UserCreationServiceTests {

    private final Logger log = LoggerFactory.getLogger(this.getClass());


    // All the @Autowired are required here...
    @Mock
    private CommonConfig commonConfig;

    @Mock
    private UsersConfig usersConfig;

    @Mock
    private UserRegistrationRepository userRegistrationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ParamRepository paramRepository;

    @Mock
    private AuditIntegration auditIntegration;

    @InjectMocks
    private UserCreationService userCreationService;



    @Test
    @Order(1)
    public void testUserCreation_unsecured() {
        log.info("[users][Creation][test] User Creation test");

        UserAccountCreationBody body = new UserAccountCreationBody();
        body.setPassword("123456");
        body.setEmail("john.doe@foo.bar");
        body.setConditionValidation(true);
        body.setValidationID("1234567890");

        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        given(req.getHeader(anyString())).willReturn("1.1.1.1");
        doNothing().when(auditIntegration).auditLog(any(),any(),any(), any(), any());

        // Creation comes from a self-service registration with a code
        try {
            Method createUserMethod = UserCreationService.class.getDeclaredMethod(
                    "createUser_unsecured",
                    UserAccountCreationBody.class,
                    HttpServletRequest.class,
                    boolean.class
            );
            createUserMethod.setAccessible(true);

            // ---------------------------------------
            log.info("[users][Creation][test] Full Self Creation verification");

            UserRegistration r = Mockito.mock(UserRegistration.class);
            given(r.getExpirationDate()).willReturn(Now.NowUtcMs()+100_000);
            given(r.getEncEmail(anyString())).willReturn("john.doe@foo.bar");

            given(userRegistrationRepository.findOneUserRegistrationByRegistrationCode(any())).willReturn(r);
            doNothing().when(r).setExpirationDate(anyLong());
            given(userRegistrationRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));
            given(userRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));
            given(userRepository.findOneUserByLogin(anyString())).willReturn(null);
            given(commonConfig.getEncryptionKey()).willReturn("d5b504d560363cfe33890c4f5343f387");
            given(commonConfig.getApplicationKey()).willReturn("a84c2d1f7b9e063d5f1a2e9c3b7d408e");
            Param p = new Param();
            p.setParamKey("users.condition.version");
            p.setStringValue("ABCD");
            given(paramRepository.findByParamKey(anyString())).willReturn(p);
            given(usersConfig.isUsersPendingAutoValidation()).willReturn(true);
            given(usersConfig.getUsersPasswordExpirationDays()).willReturn(0);
            given(usersConfig.getUsersPasswordHeader()).willReturn("1234");
            given(usersConfig.getUsersPasswordFooter()).willReturn("5678");
            assertDoesNotThrow(() -> {
                User us = (User) createUserMethod.invoke(userCreationService, body, req, false);
                assertNotNull(us);
                assertEquals(2,us.getRoles().size());
                assertTrue(us.getRoles().contains("ROLE_PENDING_USER"));
                assertTrue(us.getRoles().contains("ROLE_REGISTERED_USER"));
                assertNotEquals("john.doe@foo.bar", us.getEmail());
                us.setKeys("d5b504d560363cfe33890c4f5343f387","a84c2d1f7b9e063d5f1a2e9c3b7d408e");
                assertEquals("john.doe@foo.bar", us.getEncEmail());
                assertTrue(us.isActive());
                assertFalse(us.isLocked());
            });

            // ---------------------------------------
            log.info("[users][Creation][test] Managed Self Creation verification");
            given(usersConfig.isUsersPendingAutoValidation()).willReturn(false);
            assertDoesNotThrow(() -> {
                User us = (User) createUserMethod.invoke(userCreationService, body, req, false);
                assertNotNull(us);
                assertEquals(1,us.getRoles().size());
                assertTrue(us.getRoles().contains("ROLE_PENDING_USER"));
                assertNotEquals("john.doe@foo.bar", us.getEmail());
                us.setKeys("d5b504d560363cfe33890c4f5343f387","a84c2d1f7b9e063d5f1a2e9c3b7d408e");
                assertEquals("john.doe@foo.bar", us.getEncEmail());
                assertFalse(us.isActive());
                assertFalse(us.isLocked());
            });

            // ---------------------------------------
            log.info("[users][Creation][test] Managed Self Creation verification");
            given(usersConfig.isUsersPendingAutoValidation()).willReturn(false);
            assertDoesNotThrow(() -> {
                User us = (User) createUserMethod.invoke(userCreationService, body, req, false);
                assertNotNull(us);
                assertEquals(1,us.getRoles().size());
                assertTrue(us.getRoles().contains("ROLE_PENDING_USER"));
                assertNotEquals("john.doe@foo.bar", us.getEmail());
                us.setKeys("d5b504d560363cfe33890c4f5343f387","a84c2d1f7b9e063d5f1a2e9c3b7d408e");
                assertEquals("john.doe@foo.bar", us.getEncEmail());
                assertFalse(us.isActive());
                assertFalse(us.isLocked());
            });

            // ---------------------------------------
            log.info("[users][Creation][test] Managed Self Creation verification with password expiration");
            given(usersConfig.isUsersPendingAutoValidation()).willReturn(false);
            assertDoesNotThrow(() -> {
                User us = (User) createUserMethod.invoke(userCreationService, body, req, true);
                assertNotNull(us);
                assertTrue(us.getExpiredPassword() < Now.NowUtcMs());
            });

            // ---------------------------------------
            log.info("[users][Creation][test] Invalid Registration ID");
            given(userRegistrationRepository.findOneUserRegistrationByRegistrationCode(any())).willReturn(null);
            try {
                User us = (User) createUserMethod.invoke(userCreationService, body, req, true);
            } catch (InvocationTargetException | IllegalAccessException x) {
                assertInstanceOf(ITRightException.class, x.getCause());
            }
            given(userRegistrationRepository.findOneUserRegistrationByRegistrationCode(any())).willReturn(r);

            // ---------------------------------------
            log.info("[users][Creation][test] Existing User");
            given(userRepository.findOneUserByLogin(anyString())).willReturn(new User());
            try {
                User us = (User) createUserMethod.invoke(userCreationService, body, req, true);
            } catch (InvocationTargetException | IllegalAccessException x) {
                assertInstanceOf(ITTooManyException.class, x.getCause());
            }
            given(userRepository.findOneUserByLogin(anyString())).willReturn(null);

            // ---------------------------------------
            log.info("[users][Creation][test] Change password header and footer");
            given(usersConfig.getUsersPasswordHeader()).willReturn("abcd");
            given(usersConfig.getUsersPasswordFooter()).willReturn("efgh");
            try {
                User us = (User) createUserMethod.invoke(userCreationService, body, req, true);
                //given(usersConfig.getUsersPasswordHeader()).willReturn("1234");
                //given(usersConfig.getUsersPasswordFooter()).willReturn("5678");
                assertFalse(us.isRightPassword("1234"+body.getPassword()+"5678"));
                assertTrue(us.isRightPassword("abcd"+body.getPassword()+"efgh"));
            } catch (InvocationTargetException | IllegalAccessException x) {
                assertInstanceOf(ITTooManyException.class, x.getCause());
            }

        } catch (NoSuchMethodException x){
            fail("Failed to find the createUser_unsecured method: " + x.getMessage());
        } catch (ITNotFoundException | ITParseException x) {
            fail("Failed to find the user registration: " + x.getMessage());
        }


    }


    @Test
    @Order(2)
    public void testUserCreateUserSelf() {
        log.info("[users][Creation][test] Self Creation test");

        UserAccountCreationBody body = new UserAccountCreationBody();
        body.setPassword("123456");
        body.setEmail("john.doe@foo.bar");
        body.setConditionValidation(true);
        body.setValidationID("1234567890");

        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        given(req.getHeader(anyString())).willReturn("1.1.1.1");
        doNothing().when(auditIntegration).auditLog(any(), any(), any(), any(), any());

        try {
            log.info("[users][Creation][test] Creation success");

            UserRegistration r = Mockito.mock(UserRegistration.class);
            given(r.getExpirationDate()).willReturn(Now.NowUtcMs() + 100_000);
            given(r.getEncEmail(anyString())).willReturn("john.doe@foo.bar");
            given(userRegistrationRepository.findOneUserRegistrationByRegistrationCode(any())).willReturn(r);
            doNothing().when(r).setExpirationDate(anyLong());
            given(userRegistrationRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));
            given(userRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));
            given(userRepository.findOneUserByLogin(anyString())).willReturn(null);
            given(commonConfig.getEncryptionKey()).willReturn("d5b504d560363cfe33890c4f5343f387");
            given(commonConfig.getApplicationKey()).willReturn("a84c2d1f7b9e063d5f1a2e9c3b7d408e");
            given(usersConfig.isUsersRegistrationSelf()).willReturn(true);
            Param p = new Param();
            p.setParamKey("users.condition.version");
            p.setStringValue("ABCD");
            given(paramRepository.findByParamKey(anyString())).willReturn(p);
            given(usersConfig.isUsersPendingAutoValidation()).willReturn(true);
            given(usersConfig.getUsersPasswordExpirationDays()).willReturn(0);
            given(usersConfig.getUsersPasswordMinSize()).willReturn(5);
            given(usersConfig.getUsersPasswordMinUppercase()).willReturn(0);
            given(usersConfig.getUsersPasswordMinLowercase()).willReturn(0);
            given(usersConfig.getUsersPasswordMinNumbers()).willReturn(0);
            given(usersConfig.getUsersPasswordMinSymbols()).willReturn(0);
            assertDoesNotThrow(() -> {
                userCreationService.createUserSelf(body, req);
            });

            log.info("[users][Creation][test] Creation error - registration closed");
            given(usersConfig.isUsersRegistrationSelf()).willReturn(false);
            assertThrows(ITParseException.class, () -> {
                userCreationService.createUserSelf(body, req);
            });
            given(usersConfig.isUsersRegistrationSelf()).willReturn(true);

            log.info("[users][Creation][test] Creation error - empty password");
            body.setPassword("");
            assertThrows(ITParseException.class, () -> {
                userCreationService.createUserSelf(body, req);
            });
            body.setPassword(null);
            assertThrows(ITParseException.class, () -> {
                userCreationService.createUserSelf(body, req);
            });

            log.info("[users][Creation][test] Creation error - password rules");
            given(usersConfig.getUsersPasswordMinSize()).willReturn(10);
            given(usersConfig.getUsersPasswordMinUppercase()).willReturn(0);
            given(usersConfig.getUsersPasswordMinLowercase()).willReturn(0);
            given(usersConfig.getUsersPasswordMinNumbers()).willReturn(0);
            given(usersConfig.getUsersPasswordMinSymbols()).willReturn(0);
            body.setPassword("01abDE$%h");
            assertThrows(ITParseException.class, () -> {
                userCreationService.createUserSelf(body, req);
            });
            given(usersConfig.getUsersPasswordMinSize()).willReturn(9);
            assertDoesNotThrow(()-> {
                userCreationService.createUserSelf(body, req);
            });
            given(usersConfig.getUsersPasswordMinUppercase()).willReturn(3);
            assertThrows(ITParseException.class, () -> {
                userCreationService.createUserSelf(body, req);
            });
            given(usersConfig.getUsersPasswordMinUppercase()).willReturn(2);
            given(usersConfig.getUsersPasswordMinLowercase()).willReturn(4);
            assertThrows(ITParseException.class, () -> {
                userCreationService.createUserSelf(body, req);
            });
            given(usersConfig.getUsersPasswordMinLowercase()).willReturn(3);
            given(usersConfig.getUsersPasswordMinNumbers()).willReturn(3);
            assertThrows(ITParseException.class, () -> {
                userCreationService.createUserSelf(body, req);
            });
            given(usersConfig.getUsersPasswordMinNumbers()).willReturn(2);
            given(usersConfig.getUsersPasswordMinSymbols()).willReturn(3);
            assertThrows(ITParseException.class, () -> {
                userCreationService.createUserSelf(body, req);
            });
            given(usersConfig.getUsersPasswordMinSymbols()).willReturn(2);
            assertDoesNotThrow(() -> {
                userCreationService.createUserSelf(body, req);
            });

        } catch (Exception e) {
            log.error("[users][Creation][test] Failed with {}", e.getMessage());
        }
    }

}
