package com.disk91.iot.users;

import com.disk91.common.config.CommonConfig;
import com.disk91.common.tools.CustomField;
import com.disk91.common.tools.HexCodingTools;
import com.disk91.common.tools.Now;
import com.disk91.common.tools.exceptions.ITNotFoundException;
import com.disk91.common.tools.exceptions.ITParseException;
import com.disk91.users.mdb.entities.User;
import com.disk91.users.mdb.entities.UserRegistration;
import com.disk91.users.mdb.entities.sub.UserAcl;
import com.disk91.users.mdb.entities.sub.UserAlertPreference;
import com.disk91.users.mdb.entities.sub.UserBillingProfile;
import com.disk91.users.mdb.entities.sub.UserProfile;
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

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@PropertySource(value = {"file:configuration/common-test.properties"}, ignoreResourceNotFound = true)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserTests {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Mock
    private CommonConfig commonConfig;

    @InjectMocks
    private User user2;

    @InjectMocks
    private User user;

    @Test
    @Order(1)
    public void testUserCreationBasicFunction() {
        log.info("[users][test] Verify the underlaying function");
        User user = new User();

        given(commonConfig.getEncryptionKey()).willReturn("d5b504d560363cfe33890c4f5343f387");
        given(commonConfig.getApplicationKey()).willReturn("a84c2d1f7b9e063d5f1a2e9c3b7d408e");
        user.setKeys(commonConfig.getEncryptionKey(), commonConfig.getApplicationKey());

        assertDoesNotThrow(() -> {
            user.changePassword("test", true);
            user.setEncLogin("john.doe@foo.bar");
            log.info("[users][test] login: " + user.getLogin());
            log.info("[users][test] salt: " + HexCodingTools.bytesToHex(user.getSalt()));
            log.info("[users][test] password (SHA-256): " + user.getPassword());
            log.info("[users][test] secret (PBKDF2): " + user.getUserSecret());
            assertNotNull(user.getSalt());
            assertNotNull(user.getPassword());
            assertNotNull(user.getUserSecret());
            assertEquals(user.getSalt().length,16);
            assertEquals(user.getPassword().length(),64); // 32 Bytes
            assertEquals(user.getUserSecret().length(),32); // 16 Bytes
        });

        // verify the key generation stability over time
        user2.setKeys(commonConfig.getEncryptionKey(), commonConfig.getApplicationKey());
        user2.setSalt(HexCodingTools.getByteArrayFromHexString("21AFD07BF82F5F9139942BCB3910619E"));
        user2.setSessionSecret("ED8075DF985E20F80F3248DCCC11FEFD");
        assertDoesNotThrow(() -> {
            user2.changePassword("test", false);
            log.info("[users][test] salt: " + HexCodingTools.bytesToHex(user2.getSalt()));
            log.info("[users][test] password (SHA-256): " + user2.getPassword());
            log.info("[users][test] secret (PBKDF2): " + user2.getUserSecret());
            assertNotNull(user2.getSalt());
            assertNotNull(user2.getPassword());
            assertNotNull(user2.getUserSecret());
            assertEquals("21AFD07BF82F5F9139942BCB3910619E", HexCodingTools.bytesToHex(user2.getSalt()));
            assertEquals("8C9830810DD36F82E1AD24762ADD3477BF64378ECA36CD5D61772BC76B865930", user2.getPassword()); // 32 Bytes
            assertEquals("ED8075DF985E20F80F3248DCCC11FEFD", user2.getUserSecret()); // 16 Bytes
        });

        // make sure salt is working
        assertNotEquals(user.getPassword(), user2.getPassword());
        assertNotEquals(user.getUserSecret(), user2.getUserSecret());
        // make sure we don't generate the same hash with two different methods
        assertNotEquals(user.getPassword(), user.getUserSecret());

        // encrypt the email
        assertDoesNotThrow(() -> {
            user2.setEncEmail("test@foo.bar");
            log.info("[users][test] email: {}", user2.getEmail());
            assertEquals("7dZ4AZCzdis6/5jUr+7LeA==",user2.getEmail());

            user2.setEncRegistrationIP("1.1.1.1");
            log.info("[users][test] registration IP: {}", user2.getRegistrationIP());
            assertEquals("C0e5mJe2s9hQg2QVQI9VVg==",user2.getRegistrationIP());
        });

        // decrypt the email
        assertDoesNotThrow(() -> {
            log.info("[users][test] email: " + user2.getEncEmail());
            assertEquals("test@foo.bar", user2.getEncEmail());

            log.info("[users][test] registration IP: " + user2.getEncRegistrationIP());
            assertEquals("1.1.1.1", user2.getEncRegistrationIP());
        });

        // Ensure a user w/o a password can't be configured
        assertThrows(ITParseException.class, () -> {
            User user3 = new User();
            user3.setKeys(commonConfig.getEncryptionKey(), commonConfig.getApplicationKey());
            user3.setEncEmail("john.doe@foo.bar");
        });

    }


    @Test
    @Order(2)
    public void testNewUserCreation() {
        log.info("[users][test] Create new User");
        String presetId = HexCodingTools.getRandomHexString(128);
        long regDate = Now.NowUtcMs();

        given(commonConfig.getEncryptionKey()).willReturn("d5b504d560363cfe33890c4f5343f387");
        given(commonConfig.getApplicationKey()).willReturn("a84c2d1f7b9e063d5f1a2e9c3b7d408e");
        user.setKeys(commonConfig.getEncryptionKey(), commonConfig.getApplicationKey());


        log.info("[users][test] create a full user");
        assertDoesNotThrow(() -> {
            user.changePassword("test", true);
            assertNotNull(user.getSalt());
            assertNotNull(user.getSessionSecret());
            assertNotEquals("", user.getUserSecret());
            assertNotEquals(0, user.getSalt().length);

            user.setVersion(1);
            user.setEncLogin("john.doe@foo.bar");
            assertNotEquals("john.doe@foo.bar", user.getLogin());
            user.setEncEmail("john.doe@foo.bar");
            assertNotEquals("john.doe@foo.bar", user.getEmail());
            assertNotNull(user.getEmail());
            user.setLastLogin(regDate);
            user.setCountLogin(0);
            user.setRegistrationDate(regDate+10);
            user.setDeletionDate(0);
            user.setEncRegistrationIP("1.1.1.1");
            assertNotEquals("1.1.1.1", user.getLogin());
            user.setModificationDate(regDate+20);
            user.setPasswordResetId(presetId);
            user.setPasswordResetExp(regDate+1000);
            user.setActive(true);
            user.setLocked(false);
            user.setExpiredPassword(Now.NowUtcMs()+100_000);
            user.setApiAccount(false);
            user.setApiAccountOwner("");
            user.setLanguage("fr-fr");
            user.setConditionValidation(true);
            user.setConditionValidationDate(regDate+30);
            user.setConditionValidationVer("20250404");
            user.setLastComMessageSeen(regDate+40);

            user.setProfile(new UserProfile());
            user.setBillingProfile(new UserBillingProfile());
            user.setAlertPreference(new UserAlertPreference());
            ArrayList<CustomField> customFields = new ArrayList<>();
            CustomField cf = new CustomField();
            cf.setName("mobile");
            cf.setValue("0203040507");
            customFields.add(cf);
            user.setEncCustomFields(customFields);

            user.setEncProfileGender("Mr");
            user.setEncProfileFirstName("John");
            user.setEncProfileLastName("Doe");
            user.setEncProfileAddress("1 rue de la paix");
            user.setEncProfileZipCode("75001");
            user.setEncProfileCity("Paris");
            user.setEncProfileCountry("France");
            user.setEncProfilePhone("0123456789");
            customFields = new ArrayList<>();
            cf = new CustomField();
            cf.setName("mobile");
            cf.setValue("0203040506");
            customFields.add(cf);
            user.setEncProfileCustomFields(customFields);

            user.setEncBillingGender("Mr");
            user.setEncBillingFirstName("John");
            user.setEncBillingLastName("Doe");
            user.setEncBillingAddress("1 rue de la paix");
            user.setEncBillingZipCode("75001");
            user.setEncBillingCity("Paris");
            user.setEncBillingCountry("France");
            user.setEncBillingPhone("0123456789");
            user.setEncBillingCompanyName("Acme Corp");
            user.setEncBillingCountryCode("FR");
            user.setEncBillingVatNumber("FR123456789");
            ArrayList<CustomField> customBFields = new ArrayList<>();
            CustomField bcf = new CustomField();
            bcf.setName("mobile");
            bcf.setValue("0203040506");
            customBFields.add(bcf);
            user.setEncBillingCustomFields(customBFields);

            user.getAlertPreference().setEmailAlert(true);
            user.getAlertPreference().setPushAlert(false);
            user.getAlertPreference().setSmsAlert(false);

            ArrayList<String> roles = new ArrayList<>();
            user.setRoles(roles);

            ArrayList<UserAcl> acls = new ArrayList<>();
            user.setAcls(acls);
        });

        // Verify the user is well created
        log.info("[users][test] Verify the user is well created and encryption made");
        assertEquals(1, user.getVersion());
        assertNotEquals("john.doe@foo.bar", user.getLogin());
        assertNotEquals("john.doe@foo.bar", user.getEmail());
        assertNotEquals("1.1.1.1", user.getRegistrationIP());
        assertEquals(1, user.getCustomFields().size());
        assertEquals("mobile", user.getCustomFields().getFirst().getName());
        assertNotEquals("0203040507", user.getCustomFields().getFirst().getValue());

        assertNotEquals("Mr", user.getProfile().getGender());
        assertNotEquals("John", user.getProfile().getFirstName());
        assertNotEquals("Doe", user.getProfile().getLastName());
        assertNotEquals("1 rue de la paix", user.getProfile().getAddress());
        assertNotEquals("75001", user.getProfile().getZipCode());
        assertNotEquals("Paris", user.getProfile().getCity());
        assertNotEquals("France", user.getProfile().getCountry());
        assertNotEquals("0123456789", user.getProfile().getPhoneNumber());
        assertEquals(1, user.getProfile().getCustomFields().size());
        assertEquals("mobile", user.getProfile().getCustomFields().get(0).getName());
        assertNotEquals("0203040506", user.getProfile().getCustomFields().get(0).getValue());
        assertNotEquals("Mr", user.getBillingProfile().getGender());
        assertNotEquals("John", user.getBillingProfile().getFirstName());
        assertNotEquals("Doe", user.getBillingProfile().getLastName());
        assertNotEquals("1 rue de la paix", user.getBillingProfile().getAddress());
        assertNotEquals("75001", user.getBillingProfile().getZipCode());
        assertNotEquals("Paris", user.getBillingProfile().getCity());
        assertNotEquals("France", user.getBillingProfile().getCountry());
        assertNotEquals("0123456789", user.getBillingProfile().getPhoneNumber());
        assertNotEquals("Acme Corp", user.getBillingProfile().getCompanyName());
        assertNotEquals("FR", user.getBillingProfile().getCountryCode());
        assertNotEquals("FR123456789", user.getBillingProfile().getVatNumber());
        assertEquals(1, user.getBillingProfile().getCustomFields().size());
        assertEquals("mobile", user.getBillingProfile().getCustomFields().get(0).getName());
        assertNotEquals("0203040506", user.getBillingProfile().getCustomFields().get(0).getValue());

        String encEmail = user.getEmail();
        String encFirstName = user.getProfile().getFirstName();
        String encLastName = user.getBillingProfile().getLastName();
        String encVat = user.getBillingProfile().getVatNumber();
        String encLogin = user.getLogin();

        log.info("[users][test] Rekeying the user");
        assertDoesNotThrow(() -> {
            user.changePassword("newTest", false);
            assertEquals(encLogin, user.getLogin());
            assertNotEquals(user.getBillingProfile().getVatNumber(), encVat);
            assertNotEquals(user.getProfile().getFirstName(), encFirstName);
            assertNotEquals(user.getEmail(), encEmail);
            assertNotEquals(user.getProfile().getLastName(), encLastName);
            assertEquals(1, user.getCustomFields().size());
            assertEquals("mobile", user.getCustomFields().getFirst().getName());
            assertEquals("0203040507", user.getEncCustomFields().getFirst().getValue());

            log.info("[users][test]  Verify rekeying");
            assertEquals("john.doe@foo.bar", user.getEncEmail());
            assertEquals("1.1.1.1", user.getEncRegistrationIP());
            assertEquals(1, user.getVersion());
            assertEquals("Mr", user.getEncProfileGender());
            assertEquals("John", user.getEncProfileFirstName());
            assertEquals("Doe", user.getEncProfileLastName());
            assertEquals("1 rue de la paix", user.getEncProfileAddress());
            assertEquals("75001", user.getEncProfileZipCode());
            assertEquals("Paris", user.getEncProfileCity());
            assertEquals("France", user.getEncProfileCountry());
            assertEquals("0123456789", user.getEncProfilePhone());
            assertEquals(1,user.getProfile().getCustomFields().size());
            assertEquals("mobile", user.getEncProfileCustomFields().get(0).getName());
            assertEquals("0203040506", user.getEncProfileCustomFields().get(0).getValue());
            assertEquals("Mr", user.getEncBillingGender());
            assertEquals("John", user.getEncBillingFirstName());
            assertEquals("Doe", user.getEncBillingLastName());
            assertEquals("1 rue de la paix", user.getEncBillingAddress());
            assertEquals("75001", user.getEncBillingZipCode());
            assertEquals("Paris", user.getEncBillingCity());
            assertEquals("France", user.getEncBillingCountry());
            assertEquals("0123456789", user.getEncBillingPhone());
            assertEquals("Acme Corp", user.getEncBillingCompanyName());
            assertEquals("FR", user.getEncBillingCountryCode());
            assertEquals("FR123456789", user.getEncBillingVatNumber());
            assertEquals(1,user.getBillingProfile().getCustomFields().size());
            assertEquals("mobile", user.getEncBillingCustomFields().get(0).getName());
            assertEquals("0203040506", user.getEncBillingCustomFields().get(0).getValue());
        });
    }


    @InjectMocks
    private UserRegistration userRegistration;

    @InjectMocks
    private UserRegistration userRegistration2;

    @Test
    @Order(3)
    public void testUserPending() {
        log.info("[users][test] Manage User Pending Creation");
        String presetId = HexCodingTools.getRandomHexString(128);
        long regDate = Now.NowUtcMs();

        given(commonConfig.getEncryptionKey()).willReturn("d5b504d560363cfe33890c4f5343f387");

        assertDoesNotThrow(() -> {
            userRegistration.init("john.doe@foo.bar", null,"1.1.1.1",1000, commonConfig.getEncryptionKey());
            userRegistration2.init("john.doe@foo.bar", "","1.1.1.1", 2000, commonConfig.getEncryptionKey());
            assertNotNull(userRegistration.getEmail());
            assertNotEquals("john.doe@foo.bar", userRegistration.getEmail());
            assertNotNull(userRegistration.getValidationId());
            assertEquals(128, userRegistration.getValidationId().length());
            assertNotNull(userRegistration2.getEmail());
            assertNotEquals("john.doe@foo.bar", userRegistration2.getEmail());
            assertNotNull(userRegistration2.getValidationId());
            assertEquals(128, userRegistration2.getValidationId().length());
            assertEquals(userRegistration.getEmail(), userRegistration2.getEmail());
            assertNotEquals(userRegistration.getValidationId(), userRegistration2.getValidationId());

            assertTrue(userRegistration.getCreationDate()-Now.NowUtcMs() < 1000);
            assertEquals(1000, userRegistration.getExpirationDate() - userRegistration.getCreationDate());
        });

        log.info("[users][test] Manage User Pending Verify");
        assertThrows(ITNotFoundException.class, () -> {
            Thread.sleep(1000); // this may expire the userPending
            userRegistration.verify(userRegistration.getValidationId(), commonConfig.getEncryptionKey());
        });
        assertDoesNotThrow(() -> {
            userRegistration2.verify(userRegistration2.getValidationId(), commonConfig.getEncryptionKey());
        });
        assertThrows(ITNotFoundException.class, () -> {
            userRegistration.verify("1234567890123456789012345678901234567890123456789012345678901234", commonConfig.getEncryptionKey());
        });

    }

}
