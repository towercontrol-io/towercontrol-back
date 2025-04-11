package com.disk91.iot.users;

import com.disk91.common.config.CommonConfig;
import com.disk91.common.tools.EncryptionHelper;
import com.disk91.common.tools.exceptions.ITParseException;
import com.disk91.common.tools.exceptions.ITTooManyException;
import com.disk91.users.api.interfaces.UserAccountCreationBody;
import com.disk91.users.config.UsersConfig;
import com.disk91.users.mdb.entities.User;
import com.disk91.users.mdb.entities.UserRegistration;
import com.disk91.users.mdb.repositories.UserRegistrationRepository;
import com.disk91.users.mdb.repositories.UserRepository;
import com.disk91.users.services.UserRegistrationService;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;


@PropertySource(value = {"file:configuration/common-test.properties"}, ignoreResourceNotFound = true)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserRegistrationServiceTests {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Mock
    private CommonConfig commonConfig;

    @Mock
    private UsersConfig usersConfig;

    @Mock
    private EncryptionHelper encryptionHelper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserRegistrationRepository userRegistrationRepository;

    @InjectMocks
    private UserRegistrationService userRegistrationService;

    @Test
    @Order(1)
    @ExtendWith(MockitoExtension.class)
    public void testUserRegistration() {
        log.info("[users][Registration][test] email structure validation");

        UserAccountCreationBody body = new UserAccountCreationBody();
        body.setEmail("john.doe@foo.bar");
        body.setRegistrationCode("1234567890");

        // The registration is closed
        log.info("[users][Registration][test] Closed registration test");
        given(usersConfig.isUsersRegistrationSelf()).willReturn(false);
        assertThrows(ITParseException.class, () -> {
            userRegistrationService.requestAccountCreation(body, null);
        });

        // The registration is open
        log.info("[users][Registration][test] Empty email test");
        given(usersConfig.isUsersRegistrationSelf()).willReturn(true);
        given(usersConfig.getUsersRegistrationEmailMaxLength()).willReturn(100);
        given(usersConfig.getUsersRegistrationEmailFilters()).willReturn("");
        body.setEmail(null);
        assertThrows(ITParseException.class, () -> {
            userRegistrationService.requestAccountCreation(body, null);
        });
        body.setEmail("");
        assertThrows(ITParseException.class, () -> {
            userRegistrationService.requestAccountCreation(body, null);
        });

        log.info("[users][Registration][test] too long email test");
        given(usersConfig.getUsersRegistrationEmailMaxLength()).willReturn(10);
        body.setEmail("john.doe@foor.bar");
        assertThrows(ITParseException.class, () -> {
            userRegistrationService.requestAccountCreation(body, null);
        });

        given(usersConfig.getUsersRegistrationEmailMaxLength()).willReturn(100);
        log.info("[users][Registration][test] Invalid email test");
        body.setEmail("john.doe");
        assertThrows(ITParseException.class, () -> {
            userRegistrationService.requestAccountCreation(body, null);
        });
        body.setEmail("john.doe@domain");
        assertThrows(ITParseException.class, () -> {
            userRegistrationService.requestAccountCreation(body, null);
        });

        log.info("[users][Registration][test] Already existing email");
        given(userRepository.findOneUserByLogin(anyString())).willReturn(new User());
        body.setEmail("john.doe@foo.bar");
        assertThrows(ITTooManyException.class, () -> {
            userRegistrationService.requestAccountCreation(body, null);
        });


        given(userRepository.findOneUserByLogin(anyString())).willReturn(null);
        given(usersConfig.getUsersRegistrationLinkExpiration()).willReturn(5L); // 5 second expiration link
        given(userRegistrationRepository.findOneUserRegistrationByEmail(any())).willReturn(new UserRegistration());

        //UserRegistration mockRegistration = Mockito.spy(UserRegistration.class);
        try {
            // Intercept class creation
            try  ( MockedConstruction<UserRegistration> mockedConstruction = Mockito.mockConstruction(UserRegistration.class,
                    (mock, context) -> {
                        // We don't care about the init method
                        doNothing().when(mock).init(anyString(),anyString(),anyLong());

                        // Bypass the protected status for the fields to inject
                        //Field encryptionHelperField = mock.getClass().getDeclaredField("encryptionHelper");
                        //Field commonConfigField = mock.getClass().getDeclaredField("commonConfig");
                        //Field logField = mock.getClass().getDeclaredField("log");
                        //encryptionHelperField.setAccessible(true);
                        //commonConfigField.setAccessible(true);
                        //logField.setAccessible(true);
                        // Inject the mock objects
                        //encryptionHelperField.set(mock, encryptionHelper);
                        //commonConfigField.set(mock, commonConfig);
                        //logField.set(mock,log);
                        // Ensure the init method calls the real implementation
                        // doCallRealMethod().when(mock).init(anyString(), anyString(), anyLong());
                        // doCallRealMethod().when(encryptionHelper).encrypt(anyString(), anyString(), anyString());
                    })
            ) {

                log.info("[users][Registration][test] Pending registration");
                assertThrows(ITTooManyException.class, () -> {
                    userRegistrationService.requestAccountCreation(body, null);
                });

                log.info("[users][Registration][test] Registration is possible");
                given(userRegistrationRepository.findOneUserRegistrationByEmail(any())).willReturn(null);
                assertDoesNotThrow(() -> {
                    userRegistrationService.requestAccountCreation(body, null);
                });

            }
        } catch ( Exception e) {
            fail("Failed to init the UserRegistration: " + e.getMessage());
        }



    }



}
