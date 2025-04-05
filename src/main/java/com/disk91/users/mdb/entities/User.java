/*
 * Copyright (c) - Paul Pinault (aka disk91) - 2024.
 *
 *    Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 *    and associated documentation files (the "Software"), to deal in the Software without restriction,
 *    including without limitation the rights to use, copy, modify, merge, publish, distribute,
 *    sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 *    furnished to do so, subject to the following conditions:
 *
 *    The above copyright notice and this permission notice shall be included in all copies or
 *    substantial portions of the Software.
 *
 *    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 *    FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
 *    OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 *    WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 *    IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.disk91.users.mdb.entities;

import com.disk91.common.config.CommonConfig;
import com.disk91.common.tools.CloneableObject;
import com.disk91.common.tools.CustomField;
import com.disk91.common.tools.EncryptionHelper;
import com.disk91.common.tools.HexCodingTools;
import com.disk91.common.tools.exceptions.ITParseException;
import com.disk91.users.mdb.entities.sub.UserAcl;
import com.disk91.users.mdb.entities.sub.UserAlertPreference;
import com.disk91.users.mdb.entities.sub.UserBillingProfile;
import com.disk91.users.mdb.entities.sub.UserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;

@Document(collection = "users_users")
@CompoundIndexes({
        @CompoundIndex(name = "login", def = "{'login': 'hashed'}"),
})
public class User implements CloneableObject<User> {

   @Transient
   private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Id
    protected String id;

    // Document version, used for later structure evolution
    protected int version;

    // hash of user login, usually the password of the user
    protected String login;

    // hash of the user password
    protected String password;

    // base64 + encrypted user email
    protected String email;

    // encryption salt
    protected byte[] salt;

    // session signature salt for the JWT allowing to invalidate the session on logout or repudiation
    protected String secret;

    // long term secret used for user profile encryption, derivative from password, so we can remove this key
    // after a certain period of time to make encrypted data unreadable and later user login can reactivate an account and
    // restore decryption mechanisms
    protected String userSecret;

    // Last time the user has login
    protected long lastLogin;

    // Number of user login over time
    protected long countLogin;

    // Registration Date (Ms since epoch)
    protected long registrationDate;

    // Registration IP (encrypted)
    protected String registrationIP;

    // Last profile modification date in Ms since epoch
    protected long modificationDate;

    // Validation String used during registration process
    protected String validationId;

    // Password Reset String used to validate the password reset process
    protected String passwordResetId;

    // Password Reset String expiration date (Ms since epoch)
    protected long passwordResetExp;

    // Accout Status, active can login.
    protected boolean active;

    // Account locked status, user can't login but has been validated
    protected boolean locked;

    // Password has expired and user need to change it on the next login
    protected boolean expiredPassword;

    // This Account is an API account and not a human account, it can't login but we can have existing JWT
    protected boolean apiAccount;

    // User who own the apiAccount, here we have the Id of the user entry corresponding
    protected String apiAccountOwner;

    // language to be used for the user, 2x2 letters country code (fr-fr)
    protected String language;

    // User Conditions has been validated
    protected boolean conditionValidation;

    // User Condition validation date ( in Ms since epoch )
    protected long conditionValidationDate;

    // Condition validation version (for revalidation when going to change)
    protected String conditionValidationVer;

    // Lats communication message displayed
    protected long lastComMessageSeen;

    // List the Roles associated to the user
    protected ArrayList<String> roles;

    // List the ACL associated to the user
    protected ArrayList<UserAcl> acls;

    // User Alerts preferences
    protected UserAlertPreference alertPreference;

    // User Profile information
    protected UserProfile profile;

    // User Billing Profile information
    protected UserBillingProfile billingProfile;


    // =========== Encryption Management ===========
    @Transient
    private static final String IV = "0123456789abcdef0123456789abcdef"; // 16 bytes IV for AES encryption

    @Transient
    @Autowired
    protected CommonConfig commonConfig;

    /**
     * Generate the encryption key used for the personal or confidential data encryption
     * This key is based on different element to maximize the security. It required the
     * server key located in configuration file (file system), application key (jar file)
     * and the userSecret (database) derivative from the user clear text password.
     * All are 16 bytes long. the 3 keys are xored to compose the encryption key.
     * IV is static from that class
     * @return
     */
    protected byte[] getEncryptionKey() throws ITParseException {
        if ( userSecret == null || userSecret.length() < 32 ) throw new ITParseException("User secret not set");
        byte[] serverKey = HexCodingTools.getByteArrayFromHexString(commonConfig.getEncryptionKey());
        byte[] applicationKey = HexCodingTools.getByteArrayFromHexString(commonConfig.getApplicationKey());
        byte[] userKey = HexCodingTools.getByteArrayFromHexString(this.userSecret);
        if ( serverKey.length != 16 || applicationKey.length != 16 || userKey.length != 16 ) {
            log.error("[users] Encryption key is not 16 bytes long");
            throw new ITParseException("Encryption key are not 16 bytes long");
        }
        byte[] encryptionKey = new byte[16];
        for ( int i=0; i< encryptionKey.length; i++) {
            encryptionKey[i] = (byte) (serverKey[i] ^ applicationKey[i] ^ userKey[i]);
        }
        return encryptionKey;
    }


    public String encrypt(String data) {
        return null;
    }


    /**
     * When the password is changed, it is necessary to rekey all information, as the password is used as a key
     * in the signature of user data. The password is also hashed with salt
     * @param password - clear text password
     * @param create - true if the password is created for the first time (no rekeying required)
     */
    public void changePassword(String password, boolean create)
    throws ITParseException
    {
        // If creation we need to generate a new salt
        if ( create ) {
            this.salt = new byte[16];
            SecureRandom random = new SecureRandom();
            random.nextBytes(this.salt);
            this.secret = HexCodingTools.getRandomHexString(32);
        }

        try {

            // Hash of the password with SHA256 ; this will be used to authenticate the user
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(this.salt);
            byte[] passwordHash = digest.digest(password.getBytes(StandardCharsets.UTF_8));

            // Hash the password for the encryption key (purpose is to be able to lock personal data without having to
            // delete the user account and let the user to later reactivate its account by logging into the system
            char[] passwordChars = password.toCharArray();
            PBEKeySpec spec = new PBEKeySpec(passwordChars, this.salt, 65536, 128);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] passwordHashForEncryption = skf.generateSecret(spec).getEncoded();

            if ( ! create ) {
                // We need to rekey all the encrypted data
                // Decode all Data ...
                String _email = this.getEncEmail();
                String _getRegistrationIP = this.getEncRegistrationIP();
                String _getProfileGender = this.getEncProfileGender();
                String _getProfileFirstName = this.getEncProfileFirstName();
                String _getProfileLastName = this.getEncProfileLastName();
                String _getProfilePhone = this.getEncProfilePhone();
                String _getProfileAddress = this.getEncProfileAddress();
                String _getProfileCity = this.getEncProfileCity();
                String _getProfileZipCode = this.getEncProfileZipCode();
                String _getProfileCountry = this.getEncProfileCountry();
                ArrayList<CustomField> _getProfileCustomFields = this.getEncProfileCustomFields();
                String _getBillingGender = this.getEncBillingGender();
                String _getBillingFirstName = this.getEncBillingFirstName();
                String _getBillingLastName = this.getEncBillingLastName();
                String _getBillingPhone = this.getEncBillingPhone();
                String _getBillingAddress = this.getEncBillingAddress();
                String _getBillingCity = this.getEncBillingCity();
                String _getBillingZipCode = this.getEncBillingZipCode();
                String _getBillingCountry = this.getEncBillingCountry();
                ArrayList<CustomField> _getBillingCustomFields = this.getEncBillingCustomFields();
                String _getBillingCompanyName = this.getEncBillingCompanyName();
                String _getBillingCountryCode = this.getEncBillingCountryCode();
                String _getBillingVatNumber = this.getEncBillingVatNumber();

                // Re-encrypt all data
                this.password = HexCodingTools.bytesToHex(passwordHash);
                this.userSecret = HexCodingTools.bytesToHex(passwordHashForEncryption);
                this.setEncEmail(_email);
                this.setEncRegistrationIP(_getRegistrationIP);
                this.setEncProfileGender(_getProfileGender);
                this.setEncProfileFirstName(_getProfileFirstName);
                this.setEncProfileLastName(_getProfileLastName);
                this.setEncProfilePhone(_getProfilePhone);
                this.setEncProfileAddress(_getProfileAddress);
                this.setEncProfileCity(_getProfileCity);
                this.setEncProfileZipCode(_getProfileZipCode);
                this.setEncProfileCountry(_getProfileCountry);
                this.setEncProfileCustomFields(_getProfileCustomFields);
                this.setEncBillingGender(_getBillingGender);
                this.setEncBillingFirstName(_getBillingFirstName);
                this.setEncBillingLastName(_getBillingLastName);
                this.setEncBillingPhone(_getBillingPhone);
                this.setEncBillingAddress(_getBillingAddress);
                this.setEncBillingCity(_getBillingCity);
                this.setEncBillingZipCode(_getBillingZipCode);
                this.setEncBillingCountry(_getBillingCountry);
                this.setEncBillingCustomFields(_getBillingCustomFields);
                this.setEncBillingCompanyName(_getBillingCompanyName);
                this.setEncBillingCountryCode(_getBillingCountryCode);
                this.setEncBillingVatNumber(_getBillingVatNumber);

            } else {
                // Save the change
                this.password = HexCodingTools.bytesToHex(passwordHash);
                this.userSecret = HexCodingTools.bytesToHex(passwordHashForEncryption);
            }
        } catch (NoSuchAlgorithmException e) {
            log.error("[users] Error while hashing password", e);
            throw new ITParseException("Unsupported hashing algorithm");
        } catch (InvalidKeySpecException e) {
            log.error("[users] Error while hashing password", e);
            throw new ITParseException("Invalid key spec");
        }

    }

    // ===================================================
    // Encrypted Setters / Getters

    @Transient
    @Autowired
    protected EncryptionHelper encryptionHelper;


    /**
     * Set the login of the user, this is the login used to authenticate the user
     * We use a Hash of the login to avoid storing the login in clear text
     * @return
     */
    public void setEncLogin(String login) throws ITParseException {
        if ( this.salt == null || this.salt.length != 16 ) {
            log.error("[users] Salt is not set or not 16 bytes long");
            throw new ITParseException("Invalid Salt");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(this.salt);
            byte[] loginHash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            this.login = HexCodingTools.bytesToHex(loginHash);
        } catch (NoSuchAlgorithmException e) {
            log.error("[users] Error while hashing login", e);
            throw new ITParseException("Unsupported hashing algorithm");
        }
    }

    // --- Email

    /**
     * Returns the clear text email
     * @return
     */
    public String getEncEmail() throws ITParseException {
        String _email = encryptionHelper.decrypt(this.email, IV, HexCodingTools.bytesToHex(this.getEncryptionKey()));
        return _email;
    }

    /**
     * Encrypt the email from the clear text value
     * @param _email - clear text email
     */
    public void setEncEmail(String _email)  throws ITParseException {
        this.email = encryptionHelper.encrypt(_email, IV, HexCodingTools.bytesToHex(this.getEncryptionKey()));
    }

    // --- Registration IP

    /**
     * Encrypt the registration IP from the clear text value
     */
    public String getEncRegistrationIP() throws ITParseException {
        return encryptionHelper.decrypt(this.registrationIP, IV, HexCodingTools.bytesToHex(this.getEncryptionKey()));
    }

    /**
     * Encrypt the registration IP from the clear text value
     * @param _registrationIP - clear text registration IP
     */
    public void setEncRegistrationIP(String _registrationIP)  throws ITParseException {
        this.registrationIP = encryptionHelper.encrypt(_registrationIP, IV, HexCodingTools.bytesToHex(this.getEncryptionKey()));
    }

    // --------------------
    // Profile
    public String getEncProfileGender() throws ITParseException {
        return encryptionHelper.decrypt(this.profile.getGender(), IV, HexCodingTools.bytesToHex(this.getEncryptionKey()));
    }

    public String getEncProfileFirstName() throws ITParseException {
        return encryptionHelper.decrypt(this.profile.getFirstName(), IV, HexCodingTools.bytesToHex(this.getEncryptionKey()));
    }

    public String getEncProfileLastName() throws ITParseException {
        return encryptionHelper.decrypt(this.profile.getLastName(), IV, HexCodingTools.bytesToHex(this.getEncryptionKey()));
    }

    public String getEncProfilePhone() throws ITParseException {
        return encryptionHelper.decrypt(this.profile.getPhoneNumber(), IV, HexCodingTools.bytesToHex(this.getEncryptionKey()));
    }

    public String getEncProfileAddress() throws ITParseException {
        return encryptionHelper.decrypt(this.profile.getAddress(), IV, HexCodingTools.bytesToHex(this.getEncryptionKey()));
    }

    public String getEncProfileCity() throws ITParseException {
        return encryptionHelper.decrypt(this.profile.getCity(), IV, HexCodingTools.bytesToHex(this.getEncryptionKey()));
    }

    public String getEncProfileZipCode() throws ITParseException {
        return encryptionHelper.decrypt(this.profile.getZipCode(), IV, HexCodingTools.bytesToHex(this.getEncryptionKey()));
    }

    public String getEncProfileCountry() throws ITParseException {
        return encryptionHelper.decrypt(this.profile.getCountry(), IV, HexCodingTools.bytesToHex(this.getEncryptionKey()));
    }

    public ArrayList<CustomField> getEncProfileCustomFields() throws ITParseException {
        ArrayList<CustomField> customFields = new ArrayList<>();
        for ( CustomField cf : this.profile.getCustomFields() ) {
            CustomField _cf = new CustomField();
            _cf.setName(cf.getName());
            _cf.setValue(encryptionHelper.decrypt(cf.getValue(), IV, HexCodingTools.bytesToHex(this.getEncryptionKey())));
            customFields.add(_cf);
        }
        return customFields;
    }

    public void setEncProfileGender(String _value)  throws ITParseException {
        if (this.profile == null) throw new ITParseException("Profile not set");
        this.profile.setGender(encryptionHelper.encrypt(_value, IV, HexCodingTools.bytesToHex(this.getEncryptionKey())));
    }

    public void setEncProfileFirstName(String _value)  throws ITParseException {
        if (this.profile == null) throw new ITParseException("Profile not set");
        this.profile.setFirstName(encryptionHelper.encrypt(_value, IV, HexCodingTools.bytesToHex(this.getEncryptionKey())));
    }

    public void setEncProfileLastName(String _value)  throws ITParseException {
        if (this.profile == null) throw new ITParseException("Profile not set");
        this.profile.setLastName(encryptionHelper.encrypt(_value, IV, HexCodingTools.bytesToHex(this.getEncryptionKey())));
    }

    public void setEncProfilePhone(String _value)  throws ITParseException {
        if (this.profile == null) throw new ITParseException("Profile not set");
        this.profile.setPhoneNumber(encryptionHelper.encrypt(_value, IV, HexCodingTools.bytesToHex(this.getEncryptionKey())));
    }

    public void setEncProfileAddress(String _value)  throws ITParseException {
        if (this.profile == null) throw new ITParseException("Profile not set");
        this.profile.setAddress(encryptionHelper.encrypt(_value, IV, HexCodingTools.bytesToHex(this.getEncryptionKey())));
    }

    public void setEncProfileCity(String _value)  throws ITParseException {
        if (this.profile == null) throw new ITParseException("Profile not set");
        this.profile.setCity(encryptionHelper.encrypt(_value, IV, HexCodingTools.bytesToHex(this.getEncryptionKey())));
    }

    public void setEncProfileZipCode(String _value)  throws ITParseException {
        if (this.profile == null) throw new ITParseException("Profile not set");
        this.profile.setZipCode(encryptionHelper.encrypt(_value, IV, HexCodingTools.bytesToHex(this.getEncryptionKey())));
    }

    public void setEncProfileCountry(String _value)  throws ITParseException {
        if (this.profile == null) throw new ITParseException("Profile not set");
        this.profile.setCountry(encryptionHelper.encrypt(_value, IV, HexCodingTools.bytesToHex(this.getEncryptionKey())));
    }

    public void setEncProfileCustomFields(ArrayList<CustomField> _value)  throws ITParseException {
        if (this.profile == null) throw new ITParseException("Profile not set");
        ArrayList<CustomField> customFields = new ArrayList<>();
        for ( CustomField cf : _value ) {
            CustomField _cf = new CustomField();
            _cf.setName(cf.getName());
            _cf.setValue(encryptionHelper.encrypt(cf.getValue(), IV, HexCodingTools.bytesToHex(this.getEncryptionKey())));
            customFields.add(_cf);
        }
        this.profile.setCustomFields(customFields);
    }


    // --------------------
    // Billing Profile

    public String getEncBillingGender() throws ITParseException {
        return encryptionHelper.decrypt(this.billingProfile.getGender(), IV, HexCodingTools.bytesToHex(this.getEncryptionKey()));
    }

    public String getEncBillingFirstName() throws ITParseException {
        return encryptionHelper.decrypt(this.billingProfile.getFirstName(), IV, HexCodingTools.bytesToHex(this.getEncryptionKey()));
    }

    public String getEncBillingLastName() throws ITParseException {
        return encryptionHelper.decrypt(this.billingProfile.getLastName(), IV, HexCodingTools.bytesToHex(this.getEncryptionKey()));
    }

    public String getEncBillingPhone() throws ITParseException {
        return encryptionHelper.decrypt(this.billingProfile.getPhoneNumber(), IV, HexCodingTools.bytesToHex(this.getEncryptionKey()));
    }

    public String getEncBillingAddress() throws ITParseException {
        return encryptionHelper.decrypt(this.billingProfile.getAddress(), IV, HexCodingTools.bytesToHex(this.getEncryptionKey()));
    }

    public String getEncBillingCity() throws ITParseException {
        return encryptionHelper.decrypt(this.billingProfile.getCity(), IV, HexCodingTools.bytesToHex(this.getEncryptionKey()));
    }

    public String getEncBillingZipCode() throws ITParseException {
        return encryptionHelper.decrypt(this.billingProfile.getZipCode(), IV, HexCodingTools.bytesToHex(this.getEncryptionKey()));
    }

    public String getEncBillingCountry() throws ITParseException {
        return encryptionHelper.decrypt(this.billingProfile.getCountry(), IV, HexCodingTools.bytesToHex(this.getEncryptionKey()));
    }

    public ArrayList<CustomField> getEncBillingCustomFields() throws ITParseException {
        ArrayList<CustomField> customFields = new ArrayList<>();
        for ( CustomField cf : this.billingProfile.getCustomFields() ) {
            CustomField _cf = new CustomField();
            _cf.setName(cf.getName());
            _cf.setValue(encryptionHelper.decrypt(cf.getValue(), IV, HexCodingTools.bytesToHex(this.getEncryptionKey())));
            customFields.add(_cf);
        }
        return customFields;
    }

    public String getEncBillingCompanyName() throws ITParseException {
        return encryptionHelper.decrypt(this.billingProfile.getCompanyName(), IV, HexCodingTools.bytesToHex(this.getEncryptionKey()));
    }

    public String getEncBillingCountryCode() throws ITParseException {
        return encryptionHelper.decrypt(this.billingProfile.getCountryCode(), IV, HexCodingTools.bytesToHex(this.getEncryptionKey()));
    }

    public String getEncBillingVatNumber() throws ITParseException {
        return encryptionHelper.decrypt(this.billingProfile.getVatNumber(), IV, HexCodingTools.bytesToHex(this.getEncryptionKey()));
    }

    // ----

    public void setEncBillingGender(String _value)  throws ITParseException {
        if (this.billingProfile == null) throw new ITParseException("Profile not set");
        this.billingProfile.setGender(encryptionHelper.encrypt(_value, IV, HexCodingTools.bytesToHex(this.getEncryptionKey())));
    }

    public void setEncBillingFirstName(String _value)  throws ITParseException {
        if (this.billingProfile == null) throw new ITParseException("Profile not set");
        this.billingProfile.setFirstName(encryptionHelper.encrypt(_value, IV, HexCodingTools.bytesToHex(this.getEncryptionKey())));
    }

    public void setEncBillingLastName(String _value)  throws ITParseException {
        if (this.billingProfile == null) throw new ITParseException("Profile not set");
        this.billingProfile.setLastName(encryptionHelper.encrypt(_value, IV, HexCodingTools.bytesToHex(this.getEncryptionKey())));
    }

    public void setEncBillingPhone(String _value)  throws ITParseException {
        if (this.billingProfile == null) throw new ITParseException("Profile not set");
        this.billingProfile.setPhoneNumber(encryptionHelper.encrypt(_value, IV, HexCodingTools.bytesToHex(this.getEncryptionKey())));
    }

    public void setEncBillingAddress(String _value)  throws ITParseException {
        if (this.billingProfile == null) throw new ITParseException("Profile not set");
        this.billingProfile.setAddress(encryptionHelper.encrypt(_value, IV, HexCodingTools.bytesToHex(this.getEncryptionKey())));
    }

    public void setEncBillingCity(String _value)  throws ITParseException {
        if (this.billingProfile == null) throw new ITParseException("Profile not set");
        this.billingProfile.setCity(encryptionHelper.encrypt(_value, IV, HexCodingTools.bytesToHex(this.getEncryptionKey())));
    }

    public void setEncBillingZipCode(String _value)  throws ITParseException {
        if (this.billingProfile == null) throw new ITParseException("Profile not set");
        this.billingProfile.setZipCode(encryptionHelper.encrypt(_value, IV, HexCodingTools.bytesToHex(this.getEncryptionKey())));
    }

    public void setEncBillingCountry(String _value)  throws ITParseException {
        if (this.billingProfile == null) throw new ITParseException("Profile not set");
        this.billingProfile.setCountry(encryptionHelper.encrypt(_value, IV, HexCodingTools.bytesToHex(this.getEncryptionKey())));
    }

    public void setEncBillingCustomFields(ArrayList<CustomField> _value)  throws ITParseException {
        if (this.billingProfile == null) throw new ITParseException("Profile not set");
        ArrayList<CustomField> customFields = new ArrayList<>();
        for ( CustomField cf : _value ) {
            CustomField _cf = new CustomField();
            _cf.setName(cf.getName());
            _cf.setValue(encryptionHelper.encrypt(cf.getValue(), IV, HexCodingTools.bytesToHex(this.getEncryptionKey())));
            customFields.add(_cf);
        }
        this.billingProfile.setCustomFields(customFields);
    }

    public void setEncBillingCompanyName(String _value)  throws ITParseException {
        if (this.billingProfile == null) throw new ITParseException("Profile not set");
        this.billingProfile.setCompanyName(encryptionHelper.encrypt(_value, IV, HexCodingTools.bytesToHex(this.getEncryptionKey())));
    }

    public void setEncBillingCountryCode(String _value)  throws ITParseException {
        if (this.billingProfile == null) throw new ITParseException("Profile not set");
        this.billingProfile.setCountryCode(encryptionHelper.encrypt(_value, IV, HexCodingTools.bytesToHex(this.getEncryptionKey())));
    }

    public void setEncBillingVatNumber(String _value)  throws ITParseException {
        if (this.billingProfile == null) throw new ITParseException("Profile not set");
        this.billingProfile.setVatNumber(encryptionHelper.encrypt(_value, IV, HexCodingTools.bytesToHex(this.getEncryptionKey())));
    }


    // ========== CLONEABLE INTERFACE ==========

    public User clone() {
        User u = new User();
        u.setId(this.id);
        u.setVersion(this.version);
        u.setLogin(this.login);
        u.setPassword(this.password);
        u.setEmail(this.email);

        // Clone the salt element by element of the Array
        byte[] _salt = new byte[this.salt.length];
        for (int i=0; i<this.salt.length; i++) {
            _salt[i] = this.salt[i];
        }
        u.setSalt(_salt);
        u.setSecret(this.secret);
        u.setUserSecret(this.userSecret);
        u.setLastLogin(this.lastLogin);
        u.setCountLogin(this.countLogin);
        u.setRegistrationDate(this.registrationDate);
        u.setRegistrationIP(this.registrationIP);
        u.setModificationDate(this.modificationDate);
        u.setValidationId(this.validationId);
        u.setPasswordResetId(this.passwordResetId);
        u.setPasswordResetExp(this.passwordResetExp);
        u.setActive(this.active);
        u.setLocked(this.locked);
        u.setExpiredPassword(this.expiredPassword);
        u.setApiAccount(this.apiAccount);
        u.setApiAccountOwner(this.apiAccountOwner);
        u.setLanguage(this.language);
        u.setConditionValidation(this.conditionValidation);
        u.setConditionValidationDate(this.conditionValidationDate);
        u.setConditionValidationVer(this.conditionValidationVer);
        u.setLastComMessageSeen(this.lastComMessageSeen);

        // Create a copy of the roles list
        ArrayList<String> _roles = new ArrayList<String>(this.roles);
        u.setRoles(_roles);

        // Create a copy of the acls list
        ArrayList<UserAcl> _acls = new ArrayList<UserAcl>();
        for (UserAcl acl : this.acls) {
            _acls.add(acl.clone());
        }
        u.setAcls(_acls);

        u.setAlertPreference(this.alertPreference.clone());
        u.setProfile(this.profile.clone());
        u.setBillingProfile(this.billingProfile.clone());
        return u;
    }


    // =============  GETTER / SETTER ==========


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public byte[] getSalt() {
        return salt;
    }

    public void setSalt(byte[] salt) {
        this.salt = salt;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getUserSecret() {
        return userSecret;
    }

    public void setUserSecret(String userSecret) {
        this.userSecret = userSecret;
    }

    public long getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(long lastLogin) {
        this.lastLogin = lastLogin;
    }

    public long getCountLogin() {
        return countLogin;
    }

    public void setCountLogin(long countLogin) {
        this.countLogin = countLogin;
    }

    public long getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(long registrationDate) {
        this.registrationDate = registrationDate;
    }

    public String getRegistrationIP() {
        return registrationIP;
    }

    public void setRegistrationIP(String registrationIP) {
        this.registrationIP = registrationIP;
    }

    public long getModificationDate() {
        return modificationDate;
    }

    public void setModificationDate(long modificationDate) {
        this.modificationDate = modificationDate;
    }

    public String getValidationId() {
        return validationId;
    }

    public void setValidationId(String validationId) {
        this.validationId = validationId;
    }

    public String getPasswordResetId() {
        return passwordResetId;
    }

    public void setPasswordResetId(String passwordResetId) {
        this.passwordResetId = passwordResetId;
    }

    public long getPasswordResetExp() {
        return passwordResetExp;
    }

    public void setPasswordResetExp(long passwordResetExp) {
        this.passwordResetExp = passwordResetExp;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public boolean isExpiredPassword() {
        return expiredPassword;
    }

    public void setExpiredPassword(boolean expiredPassword) {
        this.expiredPassword = expiredPassword;
    }

    public boolean isApiAccount() {
        return apiAccount;
    }

    public void setApiAccount(boolean apiAccount) {
        this.apiAccount = apiAccount;
    }

    public String getApiAccountOwner() {
        return apiAccountOwner;
    }

    public void setApiAccountOwner(String apiAccountOwner) {
        this.apiAccountOwner = apiAccountOwner;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public boolean isConditionValidation() {
        return conditionValidation;
    }

    public void setConditionValidation(boolean conditionValidation) {
        this.conditionValidation = conditionValidation;
    }

    public long getConditionValidationDate() {
        return conditionValidationDate;
    }

    public void setConditionValidationDate(long conditionValidationDate) {
        this.conditionValidationDate = conditionValidationDate;
    }

    public String getConditionValidationVer() {
        return conditionValidationVer;
    }

    public void setConditionValidationVer(String conditionValidationVer) {
        this.conditionValidationVer = conditionValidationVer;
    }

    public long getLastComMessageSeen() {
        return lastComMessageSeen;
    }

    public void setLastComMessageSeen(long lastComMessageSeen) {
        this.lastComMessageSeen = lastComMessageSeen;
    }

    public ArrayList<String> getRoles() {
        return roles;
    }

    public void setRoles(ArrayList<String> roles) {
        this.roles = roles;
    }

    public ArrayList<UserAcl> getAcls() {
        return acls;
    }

    public void setAcls(ArrayList<UserAcl> acls) {
        this.acls = acls;
    }

    public UserAlertPreference getAlertPreference() {
        return alertPreference;
    }

    public void setAlertPreference(UserAlertPreference alertPreference) {
        this.alertPreference = alertPreference;
    }

    public UserProfile getProfile() {
        return profile;
    }

    public void setProfile(UserProfile profile) {
        this.profile = profile;
    }

    public UserBillingProfile getBillingProfile() {
        return billingProfile;
    }

    public void setBillingProfile(UserBillingProfile billingProfile) {
        this.billingProfile = billingProfile;
    }
}
