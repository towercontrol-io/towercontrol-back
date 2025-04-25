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

import com.disk91.common.tools.CloneableObject;
import com.disk91.common.tools.CustomField;
import com.disk91.common.tools.EncryptionHelper;
import com.disk91.common.tools.HexCodingTools;
import com.disk91.common.tools.exceptions.ITParseException;
import com.disk91.users.mdb.entities.sub.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

   @Transient
   private static final int USER_VERSION = 1;

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
    protected String sessionSecret;

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

    // Password Reset String used to validate the password reset process
    protected String passwordResetId;

    // Password Reset String expiration date (Ms since epoch)
    protected long passwordResetExp;

    // Accout Status, active can login.
    protected boolean active;

    // Account locked status, user can't login but has been validated
    protected boolean locked;

    // Password expiration date in Ms from epoch
    protected long expiredPassword;

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

    // TWO FA information
    protected String twoFASecret;
    protected TwoFATypes twoFAType;


    // =========== Encryption Management ===========
    @Transient
    private static final String IV = "0123456789abcdef0123456789abcdef"; // 16 bytes IV for AES encryption

    @Transient
    private String serverKey = null;

    @Transient
    private String applicationKey = null;

    public void setKeys(String serverKey, String applicationKey) {
        this.serverKey = serverKey;
        this.applicationKey = applicationKey;
    }

    public void cleanKeys() {
        this.serverKey = null;
        this.applicationKey = null;
    }

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
        byte[] serverKey = HexCodingTools.getByteArrayFromHexString(this.serverKey);
        byte[] applicationKey = HexCodingTools.getByteArrayFromHexString(this.applicationKey);
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

    /**
     * Verify a given password for this user
     * @param _password to be tested
     * @return true when the password matches
     */
    public boolean isRightPassword(String _password) {
        try {
            // Hash of the password with SHA256 ; this will be used to authenticate the user
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(this.salt);
            byte[] passwordHash = digest.digest(_password.getBytes(StandardCharsets.UTF_8));
            return (HexCodingTools.bytesToHex(passwordHash).compareTo(this.password) == 0);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Set the userSecret based on userPassword
     * @param password
     */
    protected byte[] generateUserSecret(String password) throws ITParseException {
        try {
            // Hash the password for the encryption key (purpose is to be able to lock personal data without having to
            // delete the user account and let the user to later reactivate its account by logging into the system
            char[] passwordChars = password.toCharArray();
            PBEKeySpec spec = new PBEKeySpec(passwordChars, this.salt, 65536, 128);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return skf.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException e) {
            log.error("[users] Error while hashing password", e);
            throw new ITParseException("Unsupported hashing algorithm");
        } catch (InvalidKeySpecException e) {
            log.error("[users] Error while hashing password", e);
            throw new ITParseException("Invalid key spec");
        }
    }

    public void restoreUserSecret(String password) throws ITParseException {
        this.userSecret = HexCodingTools.bytesToHex(this.generateUserSecret(password));
    }

    /**
     * Clear the user secret, so the personal data can't be decrypted anymore (until next login)
     */
    public void clearUserSecret() {
        this.userSecret = "";
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
        // Make sure structure is complete
        if ( this.roles == null ) this.roles = new ArrayList<>();
        if ( this.acls == null ) this.acls = new ArrayList<>();
        if ( this.profile == null ) this.profile = new UserProfile();
        if ( this.billingProfile == null ) this.billingProfile = new UserBillingProfile();
        if ( this.alertPreference == null ) this.alertPreference = new UserAlertPreference();

        // If creation we need to generate a new salt
        if ( create ) {
            this.salt = new byte[16];
            SecureRandom random = new SecureRandom();
            random.nextBytes(this.salt);
            this.sessionSecret = HexCodingTools.getRandomHexString(64);
            this.version = USER_VERSION;
        }

        try {

            // Hash of the password with SHA256 ; this will be used to authenticate the user
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(this.salt);
            byte[] passwordHash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            byte[] passwordHashForEncryption = this.generateUserSecret(password);

            if ( ! create ) {
                // We need to rekey all the encrypted data
                // Decode all Data ...
                String _email = ( this.email != null ) ? this.getEncEmail() : null;
                String _getRegistrationIP = ( this.registrationIP != null ) ? this.getEncRegistrationIP() : null;
                String _getProfileGender = (this.profile.getGender() != null )? this.getEncProfileGender() : null;
                String _getProfileFirstName = (this.profile.getFirstName() != null )? this.getEncProfileFirstName() :null;
                String _getProfileLastName = (this.profile.getLastName() != null )?this.getEncProfileLastName() :null;
                String _getProfilePhone = (this.profile.getPhoneNumber() != null )?this.getEncProfilePhone():null;
                String _getProfileAddress = (this.profile.getAddress() != null )?this.getEncProfileAddress() : null;
                String _getProfileCity = (this.profile.getCity() != null )?this.getEncProfileCity():null;
                String _getProfileZipCode = (this.profile.getZipCode() != null )?this.getEncProfileZipCode():null;
                String _getProfileCountry = (this.profile.getCountry() != null )?this.getEncProfileCountry():null;
                ArrayList<CustomField> _getProfileCustomFields = (this.profile.getCustomFields() != null )?this.getEncProfileCustomFields():null;
                String _getBillingGender = (this.billingProfile.getGender() != null )?this.getEncBillingGender():null;
                String _getBillingFirstName = (this.billingProfile.getFirstName() != null )?this.getEncBillingFirstName():null;
                String _getBillingLastName = (this.billingProfile.getLastName() != null )?this.getEncBillingLastName():null;
                String _getBillingPhone = (this.billingProfile.getPhoneNumber() != null )?this.getEncBillingPhone():null;
                String _getBillingAddress = (this.billingProfile.getAddress() != null )?this.getEncBillingAddress():null;
                String _getBillingCity = (this.billingProfile.getCity() != null )?this.getEncBillingCity():null;
                String _getBillingZipCode = (this.billingProfile.getZipCode() != null )?this.getEncBillingZipCode():null;
                String _getBillingCountry = (this.billingProfile.getCountry() != null )?this.getEncBillingCountry():null;
                ArrayList<CustomField> _getBillingCustomFields = (this.billingProfile.getCustomFields() != null )?this.getEncBillingCustomFields():null;
                String _getBillingCompanyName = (this.billingProfile.getCompanyName() != null )?this.getEncBillingCompanyName():null;
                String _getBillingCountryCode = (this.billingProfile.getCountryCode()!= null )?this.getEncBillingCountryCode():null;
                String _getBillingVatNumber = (this.billingProfile.getVatNumber() != null )?this.getEncBillingVatNumber():null;
                String _getTwoFASecret = (this.twoFASecret != null )?this.getEncTwoFASecret():null;

                // Re-encrypt all data
                this.password = HexCodingTools.bytesToHex(passwordHash);
                this.userSecret = HexCodingTools.bytesToHex(passwordHashForEncryption);
                if ( _email != null) this.setEncEmail(_email);
                if ( _getRegistrationIP != null) this.setEncRegistrationIP(_getRegistrationIP);
                if ( _getProfileGender != null) this.setEncProfileGender(_getProfileGender);
                if ( _getProfileFirstName != null) this.setEncProfileFirstName(_getProfileFirstName);
                if ( _getProfileLastName != null) this.setEncProfileLastName(_getProfileLastName);
                if ( _getProfilePhone != null) this.setEncProfilePhone(_getProfilePhone);
                if ( _getProfileAddress != null) this.setEncProfileAddress(_getProfileAddress);
                if ( _getProfileCity != null) this.setEncProfileCity(_getProfileCity);
                if ( _getProfileZipCode != null) this.setEncProfileZipCode(_getProfileZipCode);
                if ( _getProfileCountry != null) this.setEncProfileCountry(_getProfileCountry);
                if ( _getProfileCustomFields != null) this.setEncProfileCustomFields(_getProfileCustomFields);
                if ( _getBillingGender != null) this.setEncBillingGender(_getBillingGender);
                if ( _getBillingFirstName != null) this.setEncBillingFirstName(_getBillingFirstName);
                if ( _getBillingLastName != null) this.setEncBillingLastName(_getBillingLastName);
                if ( _getBillingPhone != null) this.setEncBillingPhone(_getBillingPhone);
                if ( _getBillingAddress != null) this.setEncBillingAddress(_getBillingAddress);
                if ( _getBillingCity != null) this.setEncBillingCity(_getBillingCity);
                if ( _getBillingZipCode != null) this.setEncBillingZipCode(_getBillingZipCode);
                if ( _getBillingCountry != null) this.setEncBillingCountry(_getBillingCountry);
                if ( _getBillingCustomFields != null) this.setEncBillingCustomFields(_getBillingCustomFields);
                if ( _getBillingCompanyName != null) this.setEncBillingCompanyName(_getBillingCompanyName);
                if ( _getBillingCountryCode != null) this.setEncBillingCountryCode(_getBillingCountryCode);
                if ( _getBillingVatNumber != null) this.setEncBillingVatNumber(_getBillingVatNumber);
                if ( _getTwoFASecret != null) this.setEncTwoFASecret(_getTwoFASecret);

            } else {
                // Save the change
                this.password = HexCodingTools.bytesToHex(passwordHash);
                this.userSecret = HexCodingTools.bytesToHex(passwordHashForEncryption);
            }
        } catch (NoSuchAlgorithmException e) {
            log.error("[users] Error while hashing password", e);
            throw new ITParseException("Unsupported hashing algorithm");
        } catch (ITParseException e) {
            log.error("[users] Error while hashing password", e);
            throw new ITParseException("Invalid key spec");
        }

    }

    // ===================================================
    // Encrypted Setters / Getters

    @Transient
    private static String LOGINSALT_STR = "8c7b5e6d3f2a1b4c9d0e7f3a6c5b2d1e";
    /**
     * Set the login of the user, this is the login used to authenticate the user
     * We use a Hash of the login to avoid storing the login in clear text.
     * Login use a global slat and is global function
     * @return
     */
    public static String encodeLogin(String login) throws ITParseException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(HexCodingTools.getByteArrayFromHexString(LOGINSALT_STR));
            byte[] loginHash = digest.digest(login.getBytes(StandardCharsets.UTF_8));
            return HexCodingTools.bytesToHex(loginHash);
        } catch (NoSuchAlgorithmException e) {
            throw new ITParseException("Unsupported hashing algorithm");
        }
    }

    public void setEncLogin(String login) throws ITParseException {
        try {
            this.login = encodeLogin(login);
        } catch (ITParseException e) {
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
        String _email = EncryptionHelper.decrypt(this.email, IV, HexCodingTools.bytesToHex(this.getEncryptionKey()));
        return _email;
    }

    /**
     * Encrypt the email from the clear text value
     * @param _email - clear text email
     */
    public void setEncEmail(String _email)  throws ITParseException {
        this.email = EncryptionHelper.encrypt(_email, IV, HexCodingTools.bytesToHex(this.getEncryptionKey()));
    }

    // --- Registration IP

    /**
     * Encrypt the registration IP from the clear text value
     */
    public String getEncRegistrationIP() throws ITParseException {
        return EncryptionHelper.decrypt(this.registrationIP, IV, HexCodingTools.bytesToHex(this.getEncryptionKey()));
    }

    /**
     * Encrypt the registration IP from the clear text value
     * @param _registrationIP - clear text registration IP
     */
    public void setEncRegistrationIP(String _registrationIP)  throws ITParseException {
        this.registrationIP = EncryptionHelper.encrypt(_registrationIP, IV, HexCodingTools.bytesToHex(this.getEncryptionKey()));
    }

    /**
     * Decrypt the 2FA secret information
     */
    public String getEncTwoFASecret() throws ITParseException {
        return EncryptionHelper.decrypt(this.twoFASecret, IV, HexCodingTools.bytesToHex(this.getEncryptionKey()));
    }

    /**
     * Encrypt the 2FA secret information
     * @param _twoFASecret - clear text 2FA secret
     */
    public void setEncTwoFASecret(String _twoFASecret)  throws ITParseException {
        this.twoFASecret = EncryptionHelper.encrypt(_twoFASecret, IV, HexCodingTools.bytesToHex(this.getEncryptionKey()));
    }

    // --------------------
    // Profile
    public String getEncProfileGender() throws ITParseException {
        return EncryptionHelper.decrypt(this.profile.getGender(), IV, HexCodingTools.bytesToHex(this.getEncryptionKey()));
    }

    public String getEncProfileFirstName() throws ITParseException {
        if ( this.profile.getFirstName() == null || !this.profile.getFirstName().isEmpty() ) return "";
        return EncryptionHelper.decrypt(this.profile.getFirstName(), IV, HexCodingTools.bytesToHex(this.getEncryptionKey()));
    }

    public String getEncProfileLastName() throws ITParseException {
        if ( this.profile.getLastName() == null || !this.profile.getLastName().isEmpty() ) return "";
        return EncryptionHelper.decrypt(this.profile.getLastName(), IV, HexCodingTools.bytesToHex(this.getEncryptionKey()));
    }

    public String getEncProfilePhone() throws ITParseException {
        return EncryptionHelper.decrypt(this.profile.getPhoneNumber(), IV, HexCodingTools.bytesToHex(this.getEncryptionKey()));
    }

    public String getEncProfileAddress() throws ITParseException {
        return EncryptionHelper.decrypt(this.profile.getAddress(), IV, HexCodingTools.bytesToHex(this.getEncryptionKey()));
    }

    public String getEncProfileCity() throws ITParseException {
        return EncryptionHelper.decrypt(this.profile.getCity(), IV, HexCodingTools.bytesToHex(this.getEncryptionKey()));
    }

    public String getEncProfileZipCode() throws ITParseException {
        return EncryptionHelper.decrypt(this.profile.getZipCode(), IV, HexCodingTools.bytesToHex(this.getEncryptionKey()));
    }

    public String getEncProfileCountry() throws ITParseException {
        return EncryptionHelper.decrypt(this.profile.getCountry(), IV, HexCodingTools.bytesToHex(this.getEncryptionKey()));
    }

    public ArrayList<CustomField> getEncProfileCustomFields() throws ITParseException {
        ArrayList<CustomField> customFields = new ArrayList<>();
        for ( CustomField cf : this.profile.getCustomFields() ) {
            CustomField _cf = new CustomField();
            _cf.setName(cf.getName());
            _cf.setValue(EncryptionHelper.decrypt(cf.getValue(), IV, HexCodingTools.bytesToHex(this.getEncryptionKey())));
            customFields.add(_cf);
        }
        return customFields;
    }

    public void setEncProfileGender(String _value)  throws ITParseException {
        if (this.profile == null) throw new ITParseException("Profile not set");
        this.profile.setGender(EncryptionHelper.encrypt(_value, IV, HexCodingTools.bytesToHex(this.getEncryptionKey())));
    }

    public void setEncProfileFirstName(String _value)  throws ITParseException {
        if (this.profile == null) throw new ITParseException("Profile not set");
        this.profile.setFirstName(EncryptionHelper.encrypt(_value, IV, HexCodingTools.bytesToHex(this.getEncryptionKey())));
    }

    public void setEncProfileLastName(String _value)  throws ITParseException {
        if (this.profile == null) throw new ITParseException("Profile not set");
        this.profile.setLastName(EncryptionHelper.encrypt(_value, IV, HexCodingTools.bytesToHex(this.getEncryptionKey())));
    }

    public void setEncProfilePhone(String _value)  throws ITParseException {
        if (this.profile == null) throw new ITParseException("Profile not set");
        this.profile.setPhoneNumber(EncryptionHelper.encrypt(_value, IV, HexCodingTools.bytesToHex(this.getEncryptionKey())));
    }

    public void setEncProfileAddress(String _value)  throws ITParseException {
        if (this.profile == null) throw new ITParseException("Profile not set");
        this.profile.setAddress(EncryptionHelper.encrypt(_value, IV, HexCodingTools.bytesToHex(this.getEncryptionKey())));
    }

    public void setEncProfileCity(String _value)  throws ITParseException {
        if (this.profile == null) throw new ITParseException("Profile not set");
        this.profile.setCity(EncryptionHelper.encrypt(_value, IV, HexCodingTools.bytesToHex(this.getEncryptionKey())));
    }

    public void setEncProfileZipCode(String _value)  throws ITParseException {
        if (this.profile == null) throw new ITParseException("Profile not set");
        this.profile.setZipCode(EncryptionHelper.encrypt(_value, IV, HexCodingTools.bytesToHex(this.getEncryptionKey())));
    }

    public void setEncProfileCountry(String _value)  throws ITParseException {
        if (this.profile == null) throw new ITParseException("Profile not set");
        this.profile.setCountry(EncryptionHelper.encrypt(_value, IV, HexCodingTools.bytesToHex(this.getEncryptionKey())));
    }

    public void setEncProfileCustomFields(ArrayList<CustomField> _value)  throws ITParseException {
        if (this.profile == null) throw new ITParseException("Profile not set");
        ArrayList<CustomField> customFields = new ArrayList<>();
        for ( CustomField cf : _value ) {
            CustomField _cf = new CustomField();
            _cf.setName(cf.getName());
            _cf.setValue(EncryptionHelper.encrypt(cf.getValue(), IV, HexCodingTools.bytesToHex(this.getEncryptionKey())));
            customFields.add(_cf);
        }
        this.profile.setCustomFields(customFields);
    }


    // --------------------
    // Billing Profile

    public String getEncBillingGender() throws ITParseException {
        return EncryptionHelper.decrypt(this.billingProfile.getGender(), IV, HexCodingTools.bytesToHex(this.getEncryptionKey()));
    }

    public String getEncBillingFirstName() throws ITParseException {
        return EncryptionHelper.decrypt(this.billingProfile.getFirstName(), IV, HexCodingTools.bytesToHex(this.getEncryptionKey()));
    }

    public String getEncBillingLastName() throws ITParseException {
        return EncryptionHelper.decrypt(this.billingProfile.getLastName(), IV, HexCodingTools.bytesToHex(this.getEncryptionKey()));
    }

    public String getEncBillingPhone() throws ITParseException {
        return EncryptionHelper.decrypt(this.billingProfile.getPhoneNumber(), IV, HexCodingTools.bytesToHex(this.getEncryptionKey()));
    }

    public String getEncBillingAddress() throws ITParseException {
        return EncryptionHelper.decrypt(this.billingProfile.getAddress(), IV, HexCodingTools.bytesToHex(this.getEncryptionKey()));
    }

    public String getEncBillingCity() throws ITParseException {
        return EncryptionHelper.decrypt(this.billingProfile.getCity(), IV, HexCodingTools.bytesToHex(this.getEncryptionKey()));
    }

    public String getEncBillingZipCode() throws ITParseException {
        return EncryptionHelper.decrypt(this.billingProfile.getZipCode(), IV, HexCodingTools.bytesToHex(this.getEncryptionKey()));
    }

    public String getEncBillingCountry() throws ITParseException {
        return EncryptionHelper.decrypt(this.billingProfile.getCountry(), IV, HexCodingTools.bytesToHex(this.getEncryptionKey()));
    }

    public ArrayList<CustomField> getEncBillingCustomFields() throws ITParseException {
        ArrayList<CustomField> customFields = new ArrayList<>();
        for ( CustomField cf : this.billingProfile.getCustomFields() ) {
            CustomField _cf = new CustomField();
            _cf.setName(cf.getName());
            _cf.setValue(EncryptionHelper.decrypt(cf.getValue(), IV, HexCodingTools.bytesToHex(this.getEncryptionKey())));
            customFields.add(_cf);
        }
        return customFields;
    }

    public String getEncBillingCompanyName() throws ITParseException {
        return EncryptionHelper.decrypt(this.billingProfile.getCompanyName(), IV, HexCodingTools.bytesToHex(this.getEncryptionKey()));
    }

    public String getEncBillingCountryCode() throws ITParseException {
        return EncryptionHelper.decrypt(this.billingProfile.getCountryCode(), IV, HexCodingTools.bytesToHex(this.getEncryptionKey()));
    }

    public String getEncBillingVatNumber() throws ITParseException {
        return EncryptionHelper.decrypt(this.billingProfile.getVatNumber(), IV, HexCodingTools.bytesToHex(this.getEncryptionKey()));
    }

    // ----

    public void setEncBillingGender(String _value)  throws ITParseException {
        if (this.billingProfile == null) throw new ITParseException("Profile not set");
        this.billingProfile.setGender(EncryptionHelper.encrypt(_value, IV, HexCodingTools.bytesToHex(this.getEncryptionKey())));
    }

    public void setEncBillingFirstName(String _value)  throws ITParseException {
        if (this.billingProfile == null) throw new ITParseException("Profile not set");
        this.billingProfile.setFirstName(EncryptionHelper.encrypt(_value, IV, HexCodingTools.bytesToHex(this.getEncryptionKey())));
    }

    public void setEncBillingLastName(String _value)  throws ITParseException {
        if (this.billingProfile == null) throw new ITParseException("Profile not set");
        this.billingProfile.setLastName(EncryptionHelper.encrypt(_value, IV, HexCodingTools.bytesToHex(this.getEncryptionKey())));
    }

    public void setEncBillingPhone(String _value)  throws ITParseException {
        if (this.billingProfile == null) throw new ITParseException("Profile not set");
        this.billingProfile.setPhoneNumber(EncryptionHelper.encrypt(_value, IV, HexCodingTools.bytesToHex(this.getEncryptionKey())));
    }

    public void setEncBillingAddress(String _value)  throws ITParseException {
        if (this.billingProfile == null) throw new ITParseException("Profile not set");
        this.billingProfile.setAddress(EncryptionHelper.encrypt(_value, IV, HexCodingTools.bytesToHex(this.getEncryptionKey())));
    }

    public void setEncBillingCity(String _value)  throws ITParseException {
        if (this.billingProfile == null) throw new ITParseException("Profile not set");
        this.billingProfile.setCity(EncryptionHelper.encrypt(_value, IV, HexCodingTools.bytesToHex(this.getEncryptionKey())));
    }

    public void setEncBillingZipCode(String _value)  throws ITParseException {
        if (this.billingProfile == null) throw new ITParseException("Profile not set");
        this.billingProfile.setZipCode(EncryptionHelper.encrypt(_value, IV, HexCodingTools.bytesToHex(this.getEncryptionKey())));
    }

    public void setEncBillingCountry(String _value)  throws ITParseException {
        if (this.billingProfile == null) throw new ITParseException("Profile not set");
        this.billingProfile.setCountry(EncryptionHelper.encrypt(_value, IV, HexCodingTools.bytesToHex(this.getEncryptionKey())));
    }

    public void setEncBillingCustomFields(ArrayList<CustomField> _value)  throws ITParseException {
        if (this.billingProfile == null) throw new ITParseException("Profile not set");
        ArrayList<CustomField> customFields = new ArrayList<>();
        for ( CustomField cf : _value ) {
            CustomField _cf = new CustomField();
            _cf.setName(cf.getName());
            _cf.setValue(EncryptionHelper.encrypt(cf.getValue(), IV, HexCodingTools.bytesToHex(this.getEncryptionKey())));
            customFields.add(_cf);
        }
        this.billingProfile.setCustomFields(customFields);
    }

    public void setEncBillingCompanyName(String _value)  throws ITParseException {
        if (this.billingProfile == null) throw new ITParseException("Profile not set");
        this.billingProfile.setCompanyName(EncryptionHelper.encrypt(_value, IV, HexCodingTools.bytesToHex(this.getEncryptionKey())));
    }

    public void setEncBillingCountryCode(String _value)  throws ITParseException {
        if (this.billingProfile == null) throw new ITParseException("Profile not set");
        this.billingProfile.setCountryCode(EncryptionHelper.encrypt(_value, IV, HexCodingTools.bytesToHex(this.getEncryptionKey())));
    }

    public void setEncBillingVatNumber(String _value)  throws ITParseException {
        if (this.billingProfile == null) throw new ITParseException("Profile not set");
        this.billingProfile.setVatNumber(EncryptionHelper.encrypt(_value, IV, HexCodingTools.bytesToHex(this.getEncryptionKey())));
    }

    // ===================================================
    // Roles Management

    /**
     * Check if a given role has been attributed to the user
     * @param role
     */
    public boolean isInRole(String role) {
        if ( this.roles == null ) return false;
        for ( String r : this.roles ) {
            if ( r.compareTo(role) == 0 ) return true;
        }
        return false;
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
        u.setSessionSecret(this.sessionSecret);
        u.setUserSecret(this.userSecret);
        u.setLastLogin(this.lastLogin);
        u.setCountLogin(this.countLogin);
        u.setRegistrationDate(this.registrationDate);
        u.setRegistrationIP(this.registrationIP);
        u.setModificationDate(this.modificationDate);
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
        u.setTwoFAType(this.twoFAType);
        u.setTwoFASecret(this.twoFASecret);

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

    public String getSessionSecret() {
        return sessionSecret;
    }

    public void setSessionSecret(String sessionSecret) {
        this.sessionSecret = sessionSecret;
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

    public long getExpiredPassword() {
        return expiredPassword;
    }

    public void setExpiredPassword(long expiredPassword) {
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

    public String getTwoFASecret() {
        return twoFASecret;
    }

    public void setTwoFASecret(String twoFASecret) {
        this.twoFASecret = twoFASecret;
    }

    public TwoFATypes getTwoFAType() {
        return twoFAType;
    }

    public void setTwoFAType(TwoFATypes twoFAType) {
        this.twoFAType = twoFAType;
    }
}
