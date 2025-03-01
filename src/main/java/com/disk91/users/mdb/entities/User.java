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
import com.disk91.users.mdb.entities.sub.UserAlertPreference;
import com.disk91.users.mdb.entities.sub.UserBillingProfile;
import com.disk91.users.mdb.entities.sub.UserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;

@Document(collection = "users_users")
@CompoundIndexes({
        @CompoundIndex(name = "login", def = "{'login': 'hashed'}"),
})
public class User implements CloneableObject<User> {

   @Transient
   private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Id
    private String id;

    // Document version, used for later structure evolution
    private int version;

    // hash of user login, usually the password of the user
    private String login;

    // hash of the user password
    private String password;

    // encrypted user email
    private String email;

    // encryption salt
    private byte[] salt;

    // session signature salt for the JWT allowing to invalidate the session on logout or repudiation
    private String secret;

    // long term secret used for user profile encryption, derivative from password, so we can remove this key
    // after a certain period of time to make encrypted data unreadable and later user login can reactivate an account and
    // restore decryption mechanisms
    private String userSecret;

    // Last time the user has login
    private long lastLogin;

    // Number of user login over time
    private long countLogin;

    // Registration Date (Ms since epoch)
    private long registrationDate;

    // Registration IP (encrypted)
    private String registrationIP;

    // Last profile modification date in Ms since epoch
    private long modificationDate;

    // Validation String used during registration process
    private String validationId;

    // Password Reset String used to validate the password reset process
    private String passwordResetId;

    // Password Reset String expiration date (Ms since epoch)
    private long passwordResetExp;

    // Accout Status, active can login.
    private boolean active;

    // Account locked status, user can't login but has been validated
    private boolean locked;

    // Password has expired and user need to change it on the next login
    private boolean expiredPassword;

    // This Account is an API account and not a human account, it can't login but we can have existing JWT
    private boolean apiAccount;

    // User who own the apiAccount, here we have the Id of the user entry corresponding
    private String apiAccountOwner;

    // language to be used for the user, 2x2 letters country code (fr-fr)
    private String language;

    // User Conditions has been validated
    private boolean conditionValidation;

    // User Condition validation date ( in Ms since epoch )
    private long conditionValidationDate;

    // Condition validation version (for revalidation when going to change)
    private String conditionValidationVer;

    // Lats communication message displayed
    private long lastComMessageSeen;

    // List the Roles associated to the user
    private ArrayList<String> roles;

    // List the ACL associated to the user
    private ArrayList<String> acls;

    // User Alerts preferences
    private UserAlertPreference alertPreference;

    // User Profile information
    private UserProfile profile;

    // User Billing Profile information
    private UserBillingProfile billingProfile;


    // =========== Encryption Management ===========





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
        ArrayList<String> _acls = new ArrayList<String>(this.acls);
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

    public ArrayList<String> getAcls() {
        return acls;
    }

    public void setAcls(ArrayList<String> acls) {
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
