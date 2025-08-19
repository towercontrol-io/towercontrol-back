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
package com.disk91.users.services;

import com.disk91.audit.integration.AuditIntegration;
import com.disk91.common.config.CommonConfig;
import com.disk91.common.config.ModuleCatalog;
import com.disk91.common.pdb.entities.Param;
import com.disk91.common.pdb.repositories.ParamRepository;
import com.disk91.common.tools.*;
import com.disk91.common.tools.exceptions.ITNotFoundException;
import com.disk91.common.tools.exceptions.ITParseException;
import com.disk91.common.tools.exceptions.ITRightException;
import com.disk91.users.api.interfaces.*;
import com.disk91.users.config.ActionCatalog;
import com.disk91.users.config.UserMessages;
import com.disk91.users.config.UsersConfig;
import com.disk91.users.mdb.entities.User;
import com.disk91.users.mdb.entities.sub.TwoFATypes;
import com.disk91.users.mdb.repositories.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.bouncycastle.util.encoders.Base32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Locale;

@Service
public class UserProfileService {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * In this service, we will find all the functions that allow the consultation, the search of users
     * The update of profile data, the deletion of users.
     */

    @Autowired
    protected UserCache userCache;

    @Autowired
    protected CommonConfig commonConfig;

    @Autowired
    protected UsersConfig usersConfig;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected AuditIntegration auditIntegration;

    @Autowired
    protected EmailTools emailTools;

    @Autowired
    protected UserMessages userMessages;

    @Autowired
    protected ParamRepository paramRepository;

    /**
     * Verify a requestor can access a user profile for read or write. Currently, the detailed ACL are not managed
     * so R/W access is not supported and only global admin can access foreign accounts
     * @param _requestor
     * @param user
     * @return true when requestor can R/W access the user profile
     */
    protected boolean isLegitAccessRead(User _requestor, String user, boolean writeAccess) {
        if ( ! _requestor.isActive() || _requestor.isLocked() ) return false;
        if ( ! _requestor.isInRole(UsersRolesCache.StandardRoles.ROLE_REGISTERED_USER.getRoleName())) return false;

        if (    _requestor.isInRole(UsersRolesCache.StandardRoles.ROLE_GOD_ADMIN.getRoleName())
            ||  _requestor.isInRole(UsersRolesCache.StandardRoles.ROLE_USER_ADMIN.getRoleName())
            ||  _requestor.getLogin().compareTo(user) == 0
        ) {
            // the requestion is the user searched himself or it have admin / user admin global right
            // so we can hase the information
            return true;
        } else {
            // The user is not a global user admin. We may verify if the user can be a local user admin
            // for a group where this user is...
            // @TODO - manage the group ACL
            return false;
        }
    }


    /**
     * Return user basic profile for a given user. Only Admin can acces user information or you can access for yourself
     * @param requestor
     * @return
     */
    public UserBasicProfileResponse getMyUserBasicProfile(String requestor, String user)
    throws ITRightException {

        try {
            User _requestor = userCache.getUser(requestor);

            if ( !this.isLegitAccessRead(_requestor,user,false) ) {
                log.warn("[users] Requestor {} does not have access right to user {} profile", requestor, user);
                throw new ITRightException("user-profile-no-access");
            }

            User _user = _requestor;
            if ( requestor.compareTo(user) != 0 ) {
                try {
                    _user = userCache.getUser(user);
                } catch (ITNotFoundException x){
                    log.warn("[users] Searched user does not exists", x);
                    throw new ITRightException("user-profile-user-not-found");
                }
            }

            _user.setKeys(commonConfig.getEncryptionKey(), commonConfig.getApplicationKey());
            UserBasicProfileResponse r = new UserBasicProfileResponse();
            r.buildFromUser(_user);
            _user.cleanKeys();
            return r;

        } catch (ITNotFoundException x) {
            log.error("[users] Requestor does not exists", x);
            throw new ITRightException("user-profile-user-not-found");
        }
    }

    /**
     * Upsert / Delete a customField for a user in the user profile section.
     * When the value is empty, the custom field is deleted.
     * @param requestor - who is requesting the upsert
     * @param body - list of expected modifications in the profile custom fields
     * @throws ITRightException - when the modifications are not allowed
     */
    public void upsertUserProfileCustomFields(
            String requestor,
            UserProfileCustomFieldBody body
    ) throws ITRightException, ITParseException {
        if ( body.getLogin() == null || body.getLogin().isEmpty() ) {
            throw new ITParseException("user-profile-login-invalid");
        }

        try {
            String user = body.getLogin();

            User _requestor = userCache.getUser(requestor);

            if ( !this.isLegitAccessRead(_requestor,user,false) ) {
                log.warn("[users] Requestor {} does not have access right to user {} profile", requestor, user);
                throw new ITRightException("user-profile-no-access");
            }

            User _user = _requestor;
            if ( requestor.compareTo(user) != 0 ) {
                try {
                    _user = userCache.getUser(user);
                } catch (ITNotFoundException x){
                    log.warn("[users] Searched user does not exists", x);
                    throw new ITRightException("user-profile-user-not-found");
                }
            }
            _user.setKeys(commonConfig.getEncryptionKey(), commonConfig.getApplicationKey());
            boolean userObjectUpdated = false;
            for (CustomField cf : body.getCustomFields()) {
                try {
                   if ( _user.upsertEncCustomField(cf) ) {
                        userObjectUpdated = true; // at least one field has been updated
                    }
                } catch (ITParseException x) {
                    // we don't know what to do, better skip this field
                    log.warn("[users] Custom field {} generate a parsing error for user {}", cf.getName(), _user.getLogin());
                }
            }
            _user.cleanKeys();

            // commit the user modification
            if ( userObjectUpdated ) {
                userCache.saveUser(_user);                      // save & flush caches
            }

        } catch (ITNotFoundException x) {
            log.error("[users] Requestor does not exists", x);
            throw new ITRightException("user-profile-user-not-found");
        }
    }

    /**
     * Update the user basic profile information. This can be executed by the user itself or by an admin
     * This only concerns the basic profile information like first name, last name, language and profile custom fields.
     * @param requestor - who is requesting the update
     * @param body - the body of the request containing the user login and the profile information to update
     * @param req - for tracing IP and audit log
     * @throws ITRightException
     * @throws ITParseException
     * @throws ITNotFoundException
     */
    public void userBasicProfileUpdate(
            String requestor,
            UserBasicProfileBody body,
            HttpServletRequest req
    ) throws ITRightException, ITParseException, ITNotFoundException {

        if ( body.getLogin() == null || body.getLogin().isEmpty() ) {
            throw new ITParseException("user-profile-login-invalid");
        }
        if ( !body.getLanguage().isEmpty() && body.getLanguage().length() != 2) {
            throw new ITParseException("user-profile-language-invalid");
        }

        try {
            User _requestor = userCache.getUser(requestor);
            String user = body.getLogin();

            if ( !this.isLegitAccessRead(_requestor,user,true) ) {
                log.warn("[users] Requestor {} does not have access right to user {} profile", requestor, user);
                throw new ITRightException("user-profile-no-access");
            }

            User _user = _requestor;
            if ( requestor.compareTo(user) != 0 ) {
                try {
                    _user = userCache.getUser(user);
                } catch (ITNotFoundException x){
                    log.warn("[users] Searched user does not exists", x);
                    throw new ITNotFoundException("user-profile-user-not-found");
                }
            }
            _user.setKeys(commonConfig.getEncryptionKey(), commonConfig.getApplicationKey());

            boolean userObjectUpdated = false;

            if ( body.getLanguage() != null && !body.getLanguage().equals(_user.getLanguage())) {
                _user.setLanguage(body.getLanguage());
                userObjectUpdated = true;
            }

            if ( body.getFirstName() != null && !body.getFirstName().equals(_user.getEncProfileFirstName())) {
                _user.setEncProfileFirstName(body.getFirstName());
                userObjectUpdated = true;
            }
            if ( body.getLastName() != null && !body.getLastName().equals(_user.getEncProfileLastName())) {
                _user.setEncProfileLastName(body.getLastName());
                userObjectUpdated = true;
            }

            for (CustomField cf : body.getCustomFields()) {
                try {
                    if ( _user.upsertEncCustomField(cf) ) {
                        userObjectUpdated = true; // at least one field has been updated
                    }
                } catch (ITParseException x) {
                    // we don't know what to do, better skip this field
                    log.warn("[users] Custom field {} generate a parsing error for user {}", cf.getName(), _user.getLogin());
                }
            }
            _user.cleanKeys();
            // commit the user modification
            if ( userObjectUpdated ) {
                _user.setModificationDate(Now.NowUtcMs());
                userCache.saveUser(_user);                      // save & flush caches
                auditIntegration.auditLog(
                        ModuleCatalog.Modules.USERS,
                        ActionCatalog.getActionName(ActionCatalog.Actions.PROFILE_UPDATE),
                        _user.getLogin(),
                        "User profile modified by {0} from {1}",
                        new String[]{
                                _requestor.getLogin(),
                                (req != null && req.getHeader("x-real-ip") != null) ? req.getHeader("x-real-ip") : "Unknown"
                        }
                );
            }

        } catch (ITNotFoundException x) {
            log.error("[users] Requestor does not exists", x);
            throw new ITRightException("user-profile-user-not-found");
        } catch (ITParseException x) {
            throw new ITRightException("user-profile-encryption-error");
        }
    }



    /**
     * Accept the User Conditions for a given user. The user can accept its own conditions or an admin can accept the conditions
     * (even if this not really making sense)
     * @param requestor - who's requesting the user condition acceptation
     * @param user - who's user's condition applies
     * @param req - for tracing IP
     * @throws ITRightException
     * @throws ITParseException
     */
    public void userConditionAcceptation(
            String requestor,
            String user,
            HttpServletRequest req
    ) throws ITRightException, ITParseException {
        try {
            User _requestor = userCache.getUser(requestor);

            if ( !this.isLegitAccessRead(_requestor,user,true) ) {
                log.warn("[users] Requestor {} does not have write access right to user {} profile", requestor, user);
                throw new ITRightException("user-profile-no-access");
            }

            User _user = _requestor;
            if ( requestor.compareTo(user) != 0 ) {
                try {
                    _user = userCache.getUser(user);
                } catch (ITNotFoundException x){
                    log.warn("[users] Searched user does not exists", x);
                    throw new ITRightException("user-profile-user-not-found");
                }
            }

            _user.setKeys(commonConfig.getEncryptionKey(), commonConfig.getApplicationKey());
            Param p = paramRepository.findByParamKey("users.condition.version");
            if ( p != null && !p.getStringValue().isEmpty() ) {
                _user.setConditionValidation(true);
                _user.setConditionValidationDate(Now.NowUtcMs());
                _user.setConditionValidationVer(p.getStringValue());
            } else throw new ITParseException("user-profile-condition-not-found");
            _user.cleanKeys();
            userCache.saveUser(_user);                      // save & flush caches
            auditIntegration.auditLog(
                    ModuleCatalog.Modules.USERS,
                    ActionCatalog.getActionName(ActionCatalog.Actions.EULA_VALIDATION),
                    _user.getLogin(),
                    "User Condition validated by {0} from {1} with version {2}",
                    new String[]{
                            _requestor.getLogin(),
                            (req != null && req.getHeader("x-real-ip") != null) ? req.getHeader("x-real-ip") : "Unknown",
                            p.getStringValue()
                    }
            );
        } catch (ITNotFoundException x) {
            log.error("[users] Requestor does not exists", x);
            throw new ITRightException("user-profile-user-not-found");
        }
    }



    // -------------------------------------------------------
    // User Password

    @Autowired
    protected UserCreationService userCreationService;

    /**
     * Change user password, as an admin or for yourself
     * @param requestor
     * @return
     */
    public void userPasswordChange(String requestor, String user, String password)
    throws ITRightException, ITParseException {

        try {
            User _requestor = userCache.getUser(requestor);

            if ( !this.isLegitAccessRead(_requestor,user,true) ) {
                log.warn("[users] Requestor {} does not have write access right to user {} profile", requestor, user);
                throw new ITRightException("user-profile-no-access");
            }

            User _user = _requestor;
            if ( requestor.compareTo(user) != 0 ) {
                try {
                    _user = userCache.getUser(user);
                    auditIntegration.auditLog(
                            ModuleCatalog.Modules.USERS,
                            ActionCatalog.getActionName(ActionCatalog.Actions.PASSWORD_CHANGE),
                            _user.getLogin(),
                            "User {0} made a password change",
                            new String[]{
                                    _requestor.getLogin()
                            }
                    );

                } catch (ITNotFoundException x){
                    log.warn("[users] Searched user does not exists", x);
                    throw new ITRightException("user-profile-user-not-found");
                }
            }

            if ( !userCreationService.verifyPassword(password) ) {
                throw new ITParseException("user-profile-password-not-valid");
            }

            _user.setKeys(commonConfig.getEncryptionKey(), commonConfig.getApplicationKey());
            try {
                _user.changePassword(password, false);
                if ( usersConfig.getUsersPasswordExpirationDays() > 0 ) {
                    _user.setExpiredPassword(Now.NowUtcMs() + (usersConfig.getUsersPasswordExpirationDays() * Now.ONE_FULL_DAY));
                } else {
                    _user.setExpiredPassword(0);
                }
                userCache.saveUser(_user);
            } catch (ITParseException x) {
                throw new ITParseException("user-profile-change-failed");
            }
            _user.cleanKeys();

        } catch (ITNotFoundException x) {
            log.error("[users] Requestor does not exists", x);
            throw new ITRightException("user-profile-user-not-found");
        }

    }

    /**
     * Reset a user password after an email authentication received after a password lost request.
     * This is a public endpoint and the user is not authenticated. The code is available a single time
     * and for a limited period of time.
     * @param req - request, used for IP tracking
     * @param body - contains the passwor and the authentication link
     * @throws ITRightException
     * @throws ITParseException
     */
    public void userPublicPasswordChange(HttpServletRequest req, UserPasswordChangeBody body)
    throws ITRightException, ITParseException {

        if ( body.getPassword() == null || !userCreationService.verifyPassword(body.getPassword()) ) {
            throw new ITParseException("user-profile-password-not-valid");
        }

        if ( body.getChangeKey() == null || body.getChangeKey().length() != 128 ) {
            throw new ITParseException("user-profile-key-not-valid");
        }

        // Search if the key exists and is not expired
        User _user = userRepository.findOneUserByPasswordResetIdAndPasswordResetExpGreaterThan(
                body.getChangeKey(),
                Now.NowUtcMs()
        );
        if ( _user == null ) {
            throw new ITRightException("user-profile-key-not-found");
        }

        // We have a valid and not expired key, we can change password and expire it
        _user.setKeys(commonConfig.getEncryptionKey(), commonConfig.getApplicationKey());
        try {
            _user.changePassword(body.getPassword(), false);
            if ( usersConfig.getUsersPasswordExpirationDays() > 0 ) {
                _user.setExpiredPassword(Now.NowUtcMs() + (usersConfig.getUsersPasswordExpirationDays() * Now.ONE_FULL_DAY));
            } else {
                _user.setExpiredPassword(0);
            }
            _user.setPasswordResetExp(0);
            userCache.saveUser(_user);

            // Add audit trace
            auditIntegration.auditLog(
                    ModuleCatalog.Modules.USERS,
                    ActionCatalog.getActionName(ActionCatalog.Actions.PASSWORD_RESET),
                    _user.getLogin(),
                    "Password reset from {0}",
                    new String[]{(req.getHeader("x-real-ip") != null) ? req.getHeader("x-real-ip") : "Unknown"}
            );

        } catch (ITParseException x) {
            throw new ITParseException("user-profile-change-failed");
        }
        _user.cleanKeys();

    }


    /**
     * Generate a link for a password reset procedure and email the user with the link
     * The email will contain a front end link to the password reset page with a key as a parameter
     * so the user can reset its password
     *
     * @param req
     * @param body
     * @throws ITParseException
     */
    public void userPublicPasswordLost(HttpServletRequest req, UserPasswordLostBody body)
    throws ITParseException {

        // Check the email format
        if (body.getEmail() == null || body.getEmail().isEmpty() || body.getEmail().length() > usersConfig.getUsersRegistrationEmailMaxLength()) {
            Now.randomSleep(50, 350);
            throw new ITParseException("Email is missing or too long");
        }
        body.setEmail(body.getEmail().toLowerCase());
        if (!Tools.isValidEmailSyntax(body.getEmail())) {
            Now.randomSleep(50, 350);
            throw new ITParseException("Email format is not valid");
        }
        if (!Tools.isAcceptedEmailSyntax(body.getEmail(), usersConfig.getUsersRegistrationEmailFilters())) {
            Now.randomSleep(50, 350);
            throw new ITParseException("Email pattern rejected");
        }

        // search for existing
        User u = userRepository.findOneUserByLogin(User.encodeLogin(body.getEmail()));
        if (u == null) {
            Now.randomSleep(50, 300);
            throw new ITParseException("User does not exists");
        }

        // Check if the user is not locked
        if (u.isLocked() || !u.isActive()) {
            Now.randomSleep(50, 300);
            throw new ITParseException("User is locked/not active");
        }

        // Generate a new key and expiration
        u.setPasswordResetId(HexCodingTools.getRandomHexString(128));
        u.setPasswordResetExp(Now.NowUtcMs() + (usersConfig.getUsersLostPasswordLinkExpiration() * 1000));
        userCache.saveUser(u);

        // Send email
        String _path = usersConfig.getUsersLostPasswordPath().replace("!0!", u.getPasswordResetId());
        String _link = commonConfig.getCommonServiceUrl(_path, true);

        Locale locale = emailTools.extractLocale(req, Locale.forLanguageTag(commonConfig.getCommonLangDefault()));
        Object[] args = { commonConfig.getCommonServiceName(), _link };
        String _subject = userMessages.messageSource().getMessage("users.messages.lostpassword.subject", args, locale);
        String _body = userMessages.messageSource().getMessage("users.messages.lostpassword.body", args, locale);
        u.setKeys(commonConfig.getEncryptionKey(), commonConfig.getApplicationKey());
        emailTools.send(u.getEncEmail(), _body, _subject, commonConfig.getCommonMailSender());
        u.cleanKeys();
        Now.randomSleep(40, 290);
    }



    /**
     * User sign out, this will invalidate the JWT token and remove the user session key. So all the JWT tokens
     * will be invalidated immediately.Requestor can Admin can force sign out a user
     * @param requestor - who's asking for sign out
     * @param user - who's signing out login
     * @param req
     */
    public void userSignOut(
            String requestor,
            String user,
            HttpServletRequest req
    ) throws ITRightException, ITParseException {

        try {
            User _requestor = userCache.getUser(requestor);
            if ( !this.isLegitAccessRead(_requestor,user,true) ) {
                log.warn("[users] Requestor {} does not have access right to user {} profile", requestor, user);
                throw new ITRightException("user-profile-no-access");
            }

            User _user = _requestor;
            if ( requestor.compareTo(user) != 0 ) {
                try {
                    _user = userCache.getUser(user);
                } catch (ITNotFoundException x){
                    log.warn("[users] Searched user does not exists", x);
                    throw new ITRightException("user-profile-user-not-found");
                }
            }

            // Remove the session key
            _user.renewSessionSecret();
            _user.setModificationDate(Now.NowUtcMs());
            userCache.saveUser(_user);
            // Add audit trace
            auditIntegration.auditLog(
                    ModuleCatalog.Modules.USERS,
                    ActionCatalog.getActionName(ActionCatalog.Actions.LOGOUT),
                    _user.getLogin(),
                    "Logout from {0}",
                    new String[]{(req.getHeader("x-real-ip") != null) ? req.getHeader("x-real-ip") : "Unknown"}
            );

        } catch (ITNotFoundException x) {
            log.error("[users] Requestor {} not found", requestor);
            throw new ITRightException("user-logout-refused");
        }

    }

    // ----------------------------------------------------------
    // Delete account

    /**
     * Delete a User Account. Admin can delete a user account but standard user can only delete its account.
     * Account deletion is virtual in a first step. The account has its userKey cleared to make sure the
     * user personal data won't be accessible anymore. The account will be completely deleted after a given
     * time with an external schedule. This allows an admin to restore an account on user request. User login
     * will be mandatory for restoring the personal data. The user won't be able to recreate an account until
     * a full destroy. User login won't be possible after this action until admin restore.
     * When the deletion purgatory period is set to 0, the account is immediately destroyed.
     *
     * @param requestor
     * @param user
     * @param req
     * @throws ITRightException
     * @throws ITNotFoundException
     * @throws ITParseException
     */
    public void deleteUser(String requestor, String user, HttpServletRequest req )
    throws ITRightException, ITNotFoundException, ITParseException {

        try {
            User _requestor = userCache.getUser(requestor);

            if ( !this.isLegitAccessRead(_requestor,user,true) ) {
                log.warn("[users] Requestor {} does not have write access right to user {} profile", requestor, user);
                throw new ITRightException("user-profile-no-access");
            }

            User _user = _requestor;
            if ( requestor.compareTo(user) != 0 ) {
                try {
                    _user = userCache.getUser(user);
                } catch (ITNotFoundException x){
                    log.warn("[users] Searched user does not exists", x);
                    throw new ITRightException("user-profile-user-not-found");
                }
            }

            _user.setKeys(commonConfig.getEncryptionKey(), commonConfig.getApplicationKey());
            if ( usersConfig.getUserDeletionPurgatoryDuration() > 0 ) {
                if (_user.getDeletionDate() == 0) {
                    _user.setDeletionDate(Now.NowUtcMs()+(usersConfig.getUserDeletionPurgatoryDuration()*Now.ONE_HOUR));
                                                                // prepare for future physical deletion
                    _user.setActive(false);                     // make sure it is not reconnecting later
                    _user.setLocked(true);                      // ...
                    _user.clearUserSecret();                    // make sure personal data won't be accessible
                    _user.renewSessionSecret();                 // disconnect the existing sessions
                }
                _user.cleanKeys();
                userCache.saveUser(_user);                      // save & flush caches
                auditIntegration.auditLog(
                        ModuleCatalog.Modules.USERS,
                        ActionCatalog.getActionName(ActionCatalog.Actions.DELETION),
                        _user.getLogin(),
                        "User deletion by {0} from {1}",
                        new String[]{_requestor.getLogin(), (req.getHeader("x-real-ip") != null) ? req.getHeader("x-real-ip") : "Unknown"}
                );
            } else {
                // delete immediately
                userRepository.delete(_user);
            }
        } catch (ITNotFoundException x) {
            log.error("[users] Requestor does not exists", x);
            throw new ITRightException("user-profile-user-not-found");
        }
    }


    /*
     * The function will check the expiration date. When the Registration is expired, the function will delete the entry
     * in the database. Scanned on every 60 seconds
     */
    @Scheduled(fixedRate = 300000)
    void processUserPhysicalDeletion() {
        // remove all the expired registrations
        long now = Now.NowUtcMs();
        userRepository.deleteUserByDeletionDate(Now.NowUtcMs());
    }


    // ----------------------------------------------------------
    // 2FA related

    /**
     * Enable 2FA for a user. The user will be asked to provide a TOTP code to enable the 2FA
     * @param requestor - who's requesting the application
     * @param user - Who's going to have 2FA set
     * @param req - Http Request to get the IP
     * @throws ITRightException
     * @throws ITParseException
     * @return The method used and the secret to be used for the authenticator
     */
    public UserTwoFaResponse setupSecondFactor(
            String requestor,
            String user,
            UserTwoFaBody body,
            HttpServletRequest req
    ) throws ITRightException, ITParseException {
        try {
            User _requestor = userCache.getUser(requestor);

            if ( !this.isLegitAccessRead(_requestor,user,true) ) {
                log.warn("[users] Requestor {} does not have write access right to user {} profile", requestor, user);
                throw new ITRightException("user-profile-no-access");
            }

            User _user = _requestor;
            if ( requestor.compareTo(user) != 0 ) {
                try {
                    _user = userCache.getUser(user);
                } catch (ITNotFoundException x){
                    log.warn("[users] Searched user does not exists", x);
                    throw new ITRightException("user-profile-user-not-found");
                }
            }

            _user.setKeys(commonConfig.getEncryptionKey(), commonConfig.getApplicationKey());
            UserTwoFaResponse response = new UserTwoFaResponse();
            response.setTwoFaMethod(body.getTwoFaMethod());
            response.setSecret("");
            if ( body.getTwoFaMethod() == -1 ) {
                switch (body.getTwoFaType()) {
                    case NONE:
                        body.setTwoFaMethod(0);
                        break;
                    case EMAIL:
                        body.setTwoFaMethod(1);
                        break;
                    case SMS:
                        body.setTwoFaMethod(2);
                        break;
                    case AUTHENTICATOR:
                        body.setTwoFaMethod(3);
                        break;
                    default:
                        throw new ITParseException("user-profile-2fa-method-not-valid");
                }
            }
            // We setup the 2FA with the given method
            switch (body.getTwoFaMethod()) {
                case 0:
                    // None
                    _user.setTwoFAType(TwoFATypes.NONE);
                    _user.setTwoFASecret("");
                    break;
                case 1:
                    // Email
                    _user.setTwoFAType(TwoFATypes.EMAIL);
                    _user.setTwoFASecret("");
                    break;
                case 2:
                    throw new ITParseException("user-profile-2fa-sms-not-supported");
                case 3:
                    // Authenticator
                    SecureRandom random = new SecureRandom();
                    byte [] bytes = new byte[10];
                    random.nextBytes(bytes);
                    _user.setEncTwoFASecret(Base32.toBase32String(bytes));
                    _user.setTwoFAType(TwoFATypes.AUTHENTICATOR);
                    response.setSecret(
                            "otpauth://totp/" + commonConfig.getCommonServiceName() + ":" + _user.getLogin() +
                                    "?secret=" + Base32.toBase32String(bytes) +
                                    "&issuer=" + commonConfig.getCommonServiceName()
                    );
                    break;
                default:
                    throw new ITParseException("user-profile-2fa-method-not-valid");
            }
            response.setTwoFaType(_user.getTwoFAType());
            _user.cleanKeys();
            userCache.saveUser(_user);                      // save & flush caches
            auditIntegration.auditLog(
                    ModuleCatalog.Modules.USERS,
                    ActionCatalog.getActionName(ActionCatalog.Actions.TWOFACTOR_CHANGE),
                    _user.getLogin(),
                    "User 2FA changed by {0} from {1} for method {2}",
                    new String[]{
                            _requestor.getLogin(),
                            (req.getHeader("x-real-ip") != null) ? req.getHeader("x-real-ip") : "Unknown",
                            String.valueOf(_user.getTwoFAType())
                    }
            );
            return response;
        } catch (ITNotFoundException x) {
            log.error("[users] Requestor does not exists", x);
            throw new ITRightException("user-profile-user-not-found");
        }

    }


}
