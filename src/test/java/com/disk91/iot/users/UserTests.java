package com.disk91.iot.users;

import com.disk91.common.config.CommonConfig;
import com.disk91.common.tools.CustomField;
import com.disk91.common.tools.EncryptionHelper;
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
import org.mockito.Mockito;
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

    @Mock
    private EncryptionHelper encryptionHelper;

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

        given(encryptionHelper.encrypt(Mockito.anyString(),Mockito.anyString(),Mockito.anyString())).willCallRealMethod();
        given(encryptionHelper.decrypt(Mockito.anyString(),Mockito.anyString(),Mockito.anyString())).willCallRealMethod();


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
        user2.setSalt(HexCodingTools.getByteArrayFromHexString("21AFD07BF82F5F9139942BCB3910619E"));
        user2.setSecret("ED8075DF985E20F80F3248DCCC11FEFD");
        assertDoesNotThrow(() -> {
            user2.changePassword("test", false);
            log.info("[users][test] salt: " + HexCodingTools.bytesToHex(user2.getSalt()));
            log.info("[users][test] password (SHA-256): " + user2.getPassword());
            log.info("[users][test] secret (PBKDF2): " + user2.getUserSecret());
            assertNotNull(user2.getSalt());
            assertNotNull(user2.getPassword());
            assertNotNull(user2.getUserSecret());
            assertEquals(HexCodingTools.bytesToHex(user2.getSalt()),"21AFD07BF82F5F9139942BCB3910619E");
            assertEquals(user2.getPassword(), "8C9830810DD36F82E1AD24762ADD3477BF64378ECA36CD5D61772BC76B865930"); // 32 Bytes
            assertEquals(user2.getUserSecret(),"ED8075DF985E20F80F3248DCCC11FEFD"); // 16 Bytes
        });

        // make sure salt is working
        assertNotEquals(user.getPassword(), user2.getPassword());
        assertNotEquals(user.getUserSecret(), user2.getUserSecret());
        // make sure we don't generate the same hash with two different methods
        assertNotEquals(user.getPassword(), user.getUserSecret());

        // encrypt the email
        assertDoesNotThrow(() -> {
            user2.setEncEmail("test@foo.bar");
            log.info("[users][test] email: " + user2.getEmail());
            assertEquals("7dZ4AZCzdis6/5jUr+7LeA==",user2.getEmail());

            user2.setEncRegistrationIP("1.1.1.1");
            log.info("[users][test] registration IP: " + user2.getRegistrationIP());
            assertEquals("C0e5mJe2s9hQg2QVQI9VVg==",user2.getRegistrationIP());
        });

        // decrypt the email
        assertDoesNotThrow(() -> {
            log.info("[users][test] email: " + user2.getEncEmail());
            assertEquals(user2.getEncEmail(), "test@foo.bar");

            log.info("[users][test] registration IP: " + user2.getEncRegistrationIP());
            assertEquals(user2.getEncRegistrationIP(),"1.1.1.1");
        });

        // Ensure a user w/o a password can't be configured
        assertThrows(ITParseException.class, () -> {
            User user3 = new User();
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

        given(encryptionHelper.encrypt(Mockito.anyString(),Mockito.anyString(),Mockito.anyString())).willCallRealMethod();
        given(encryptionHelper.decrypt(Mockito.anyString(),Mockito.anyString(),Mockito.anyString())).willCallRealMethod();


        log.info("[users][test] create a full user");
        assertDoesNotThrow(() -> {
            user.changePassword("test", true);
            assertNotNull(user.getSalt());
            assertNotNull(user.getSecret());
            assertNotEquals(user.getUserSecret(),"");
            assertNotEquals(user.getSalt().length,0);

            user.setVersion(1);
            user.setEncLogin("john.doe@foo.bar");
            assertNotEquals(user.getLogin(),"john.doe@foo.bar");
            user.setEncEmail("john.doe@foo.bar");
            assertNotEquals(user.getEmail(),"john.doe@foo.bar");
            assertNotNull(user.getEmail());
            user.setLastLogin(regDate);
            user.setCountLogin(0);
            user.setRegistrationDate(regDate+10);
            user.setEncRegistrationIP("1.1.1.1");
            assertNotEquals(user.getLogin(),"1.1.1.1");
            user.setModificationDate(regDate+20);
            user.setPasswordResetId(presetId);
            user.setPasswordResetExp(regDate+1000);
            user.setActive(true);
            user.setLocked(false);
            user.setExpiredPassword(false);
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

            user.setEncProfileGender("Mr");
            user.setEncProfileFirstName("John");
            user.setEncProfileLastName("Doe");
            user.setEncProfileAddress("1 rue de la paix");
            user.setEncProfileZipCode("75001");
            user.setEncProfileCity("Paris");
            user.setEncProfileCountry("France");
            user.setEncProfilePhone("0123456789");
            ArrayList<CustomField> customFields = new ArrayList<>();
            CustomField cf = new CustomField();
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
        assertEquals(user.getVersion(), 1);
        assertNotEquals(user.getLogin(),"john.doe@foo.bar");
        assertNotEquals(user.getEmail(), "john.doe@foo.bar");
        assertNotEquals(user.getRegistrationIP(), "1.1.1.1");
        assertNotEquals(user.getProfile().getGender(), "Mr");
        assertNotEquals(user.getProfile().getFirstName(), "John");
        assertNotEquals(user.getProfile().getLastName(), "Doe");
        assertNotEquals(user.getProfile().getAddress(), "1 rue de la paix");
        assertNotEquals(user.getProfile().getZipCode(), "75001");
        assertNotEquals(user.getProfile().getCity(), "Paris");
        assertNotEquals(user.getProfile().getCountry(), "France");
        assertNotEquals(user.getProfile().getPhoneNumber(), "0123456789");
        assertEquals(user.getProfile().getCustomFields().size(), 1);
        assertEquals(user.getProfile().getCustomFields().get(0).getName(), "mobile");
        assertNotEquals(user.getProfile().getCustomFields().get(0).getValue(), "0203040506");
        assertNotEquals(user.getBillingProfile().getGender(), "Mr");
        assertNotEquals(user.getBillingProfile().getFirstName(), "John");
        assertNotEquals(user.getBillingProfile().getLastName(), "Doe");
        assertNotEquals(user.getBillingProfile().getAddress(), "1 rue de la paix");
        assertNotEquals(user.getBillingProfile().getZipCode(), "75001");
        assertNotEquals(user.getBillingProfile().getCity(), "Paris");
        assertNotEquals(user.getBillingProfile().getCountry(), "France");
        assertNotEquals(user.getBillingProfile().getPhoneNumber(), "0123456789");
        assertNotEquals(user.getBillingProfile().getCompanyName(), "Acme Corp");
        assertNotEquals(user.getBillingProfile().getCountryCode(), "FR");
        assertNotEquals(user.getBillingProfile().getVatNumber(), "FR123456789");
        assertEquals(user.getBillingProfile().getCustomFields().size(), 1);
        assertEquals(user.getBillingProfile().getCustomFields().get(0).getName(), "mobile");
        assertNotEquals(user.getBillingProfile().getCustomFields().get(0).getValue(), "0203040506");

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

            log.info("[users][test]  Verify rekeying");
            assertEquals("john.doe@foo.bar", user.getEncEmail());
            assertEquals("1.1.1.1", user.getEncRegistrationIP());
            assertEquals(user.getVersion(), 1);
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

        given(encryptionHelper.encrypt(Mockito.anyString(),Mockito.anyString(),Mockito.anyString())).willCallRealMethod();
        given(encryptionHelper.decrypt(Mockito.anyString(),Mockito.anyString(),Mockito.anyString())).willCallRealMethod();

        assertDoesNotThrow(() -> {
            userRegistration.init("john.doe@foo.bar", "1.1.1.1",1000);
            userRegistration2.init("john.doe@foo.bar", "1.1.1.1", 2000);
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
            assertTrue(userRegistration.getExpirationDate()- userRegistration.getCreationDate() == 1000);
        });

        log.info("[users][test] Manage User Pending Verify");
        assertThrows(ITNotFoundException.class, () -> {
            Thread.sleep(1000); // this may expire the userPending
            userRegistration.verify(userRegistration.getValidationId());
        });
        assertDoesNotThrow(() -> {
            userRegistration2.verify(userRegistration2.getValidationId());
        });
        assertThrows(ITNotFoundException.class, () -> {
            userRegistration.verify("1234567890123456789012345678901234567890123456789012345678901234");
        });

    }

}
