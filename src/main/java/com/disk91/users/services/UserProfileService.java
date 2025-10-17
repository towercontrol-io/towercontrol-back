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
import com.disk91.groups.mdb.entities.Group;
import com.disk91.groups.services.GroupsServices;
import com.disk91.groups.tools.GroupsHierarchySimplified;
import com.disk91.users.api.interfaces.*;
import com.disk91.users.api.interfaces.sub.AclItf;
import com.disk91.users.api.interfaces.sub.GroupItf;
import com.disk91.users.api.interfaces.sub.RoleItf;
import com.disk91.users.config.ActionCatalog;
import com.disk91.users.config.UserMessages;
import com.disk91.users.config.UsersConfig;
import com.disk91.users.mdb.entities.Role;
import com.disk91.users.mdb.entities.User;
import com.disk91.users.mdb.entities.sub.TwoFATypes;
import com.disk91.users.mdb.entities.sub.UserAcl;
import com.disk91.users.mdb.repositories.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.bouncycastle.util.encoders.Base32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
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

    @Autowired
    protected UserCommon userCommon;

    /**
     * Return user basic profile for a given user. Only Admin can access user information or you can access for yourself
     * @param requestor
     * @return
     */
    public UserBasicProfileResponse getMyUserBasicProfile(String requestor, String user)
    throws ITRightException {

        try {
            User _requestor = userCache.getUser(requestor);

            if ( !userCommon.isLegitAccessRead(_requestor,user,false) ) {
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

            if ( !userCommon.isLegitAccessRead(_requestor,user,false) ) {
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

            if ( !userCommon.isLegitAccessRead(_requestor,user,true) ) {
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
            if ( body.getMobileNumber() != null && !body.getMobileNumber().equals(_user.getEncProfilePhone())) {
                _user.setEncProfilePhone(body.getMobileNumber());
                userObjectUpdated = true;
            }
            if ( body.getIsoCountryCode() != null && !body.getIsoCountryCode().equals(_user.getEncProfileCountry())) {
                _user.setEncProfileCountry(body.getIsoCountryCode());
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

            if ( !userCommon.isLegitAccessRead(_requestor,user,true) ) {
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

            if ( !userCommon.isLegitAccessRead(_requestor,user,true) ) {
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
            if ( !userCommon.isLegitAccessRead(_requestor,user,true) ) {
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
     * @param requestor - user requesting the deletion
     * @param user - user to delete
     * @param immediate - when true, the user is immediately deleted, no purgatory period
     * @param req - for tracing IP and audit log
     * @throws ITRightException
     * @throws ITNotFoundException
     * @throws ITParseException
     */
    public void deleteUser(String requestor, String user, boolean immediate, HttpServletRequest req )
    throws ITRightException, ITNotFoundException, ITParseException {

        if ( user == null || user.isEmpty() ) {
            throw new ITParseException("user-profile-login-invalid");
        }

        try {
            User _requestor = userCache.getUser(requestor);

            if ( !userCommon.isLegitAccessRead(_requestor,user,true) ) {
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
            if ( usersConfig.getUsersDeletionPurgatoryDuration() > 0 && !immediate ) {
                if (_user.getDeletionDate() == 0) {
                    _user.setDeletionDate(Now.NowUtcMs()+(usersConfig.getUsersDeletionPurgatoryDuration()*Now.ONE_HOUR));
                                                                // prepare for future physical deletion
                    _user.clearUserSecret();                    // make sure personal data won't be accessible
                    _user.renewSessionSecret();                 // disconnect the existing sessions
                    _user.clearApiSessionSecret();              // disconnect the existing API sessions
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
                // @TODO - delete all the user related data in other services (devices, groups, data, etc.)
                userRepository.delete(_user);
                auditIntegration.auditLog(
                        ModuleCatalog.Modules.USERS,
                        ActionCatalog.getActionName(ActionCatalog.Actions.DELETION),
                        _user.getLogin(),
                        "User immediate deletion by {0} from {1}",
                        new String[]{_requestor.getLogin(), (req.getHeader("x-real-ip") != null) ? req.getHeader("x-real-ip") : "Unknown"}
                );
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
        try {
            // remove all the expired registrations
            // @TODO - delete all the user related data in other services (devices, groups, data, etc.)
            // @TODO - add an audit log for each deletions
            userRepository.deleteUserByDeletionDate(Now.NowUtcMs());
        } catch (Exception x) {
            // catch exception to avoid scheduler blocking
            log.error("[users] Unable to proceed to physical user deletion", x);
        }
    }

    /**
     * Resume a user account currently in purgatory. Only Admin can do this action
     */
    public void restoreUser(String requestor, String user, HttpServletRequest req )
            throws ITRightException, ITNotFoundException, ITParseException {
        User _requestor;
        try {
            _requestor = userCache.getUser(requestor);
        } catch (ITNotFoundException x) {
            log.error("[users] Requestor does not exists", x);
            throw new ITRightException("user-profile-user-not-found");
        }

        if ( !userCommon.isLegitAccessRead(_requestor,user,true) ) {
            log.warn("[users] Requestor {} does not have write access right to user {} profile", requestor, user);
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

        // restore the user if in purgatory
        if ( _user.getDeletionDate() > 0  && _user.getDeletionDate() > Now.NowUtcMs()) {
            _user.setDeletionDate(0);
            userCache.saveUser(_user);
            auditIntegration.auditLog(
                    ModuleCatalog.Modules.USERS,
                    ActionCatalog.getActionName(ActionCatalog.Actions.RESTORATION),
                    _user.getLogin(),
                    "User restored form purgatory by {0} from {1}",
                    new String[]{_requestor.getLogin(), (req.getHeader("x-real-ip") != null) ? req.getHeader("x-real-ip") : "Unknown"}
            );

        } else {
            throw new ITParseException("user-profile-not-to-restore");
        }
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

            if ( !userCommon.isLegitAccessRead(_requestor,user,true) ) {
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

    // ==========================================================================
    // Admin functions
    // ==========================================================================

    /**
     * Change the user state to active or not active. Only Admin can do this action
     *
     * @param requestor - user requesting the change
     * @param body - user and state requested
     * @param req - for tracing IP and audit log
     * @throws ITRightException
     * @throws ITNotFoundException
     * @throws ITParseException
     */
    public void activStateChangeUser(String requestor, UserStateSwitchBody body, HttpServletRequest req )
            throws ITRightException, ITNotFoundException, ITParseException {

        if ( body.getLogin() == null || body.getLogin().isEmpty() ) {
            throw new ITParseException("user-profile-login-invalid");
        }

        try {
            User _requestor = userCache.getUser(requestor);

            if ( !userCommon.isLegitAccessRead(_requestor,body.getLogin(),true) ) {
                log.warn("[users] Requestor {} does not have write access right to user {} profile", requestor, body.getLogin());
                throw new ITRightException("user-profile-no-access");
            }

            User _user = null;
            try {
                _user = userCache.getUser(body.getLogin());
            } catch (ITNotFoundException x){
                log.warn("[users] Searched user does not exists", x);
                throw new ITRightException("user-profile-user-not-found");
            }

            // user found, rights verified, we can proceed
            _user.setActive(body.isState());
            userCache.saveUser(_user);                      // save & flush caches
            auditIntegration.auditLog(
                        ModuleCatalog.Modules.USERS,
                        ActionCatalog.getActionName(ActionCatalog.Actions.PROFILE_UPDATE),
                        _user.getLogin(),
                        (body.isState()?"User actived by {0} from {1}":"User de-activated by {0} from {1}"),
                        new String[]{_requestor.getLogin(), (req.getHeader("x-real-ip") != null) ? req.getHeader("x-real-ip") : "Unknown"}
            );
        } catch (ITNotFoundException x) {
            log.error("[users] Requestor does not exists", x);
            throw new ITRightException("user-profile-user-not-found");
        }
    }


    /**
     * Change the user state to lock or unlock. Only Admin can do this action
     *
     * @param requestor - user requesting the change
     * @param body - user and state requested
     * @param req - for tracing IP and audit log
     * @throws ITRightException
     * @throws ITNotFoundException
     * @throws ITParseException
     */
    public void lockStateChangeUser(String requestor, UserStateSwitchBody body, HttpServletRequest req )
            throws ITRightException, ITNotFoundException, ITParseException {

        if ( body.getLogin() == null || body.getLogin().isEmpty() ) {
            throw new ITParseException("user-profile-login-invalid");
        }

        try {
            User _requestor = userCache.getUser(requestor);

            if ( !userCommon.isLegitAccessRead(_requestor,body.getLogin(),true) ) {
                log.warn("[users] Requestor {} does not have write access right to user {} profile", requestor, body.getLogin());
                throw new ITRightException("user-profile-no-access");
            }

            User _user = null;
            try {
                _user = userCache.getUser(body.getLogin());
            } catch (ITNotFoundException x){
                log.warn("[users] Searched user does not exists", x);
                throw new ITRightException("user-profile-user-not-found");
            }

            // user found, rights verified, we can proceed
            _user.setLocked(body.isState());
            userCache.saveUser(_user);                      // save & flush caches
            auditIntegration.auditLog(
                    ModuleCatalog.Modules.USERS,
                    ActionCatalog.getActionName(ActionCatalog.Actions.PROFILE_UPDATE),
                    _user.getLogin(),
                    (body.isState()?"User locked by {0} from {1}":"User un-locked by {0} from {1}"),
                    new String[]{_requestor.getLogin(), (req.getHeader("x-real-ip") != null) ? req.getHeader("x-real-ip") : "Unknown"}
            );
        } catch (ITNotFoundException x) {
            log.error("[users] Requestor does not exists", x);
            throw new ITRightException("user-profile-user-not-found");
        }
    }

    /**
     * Disable the user 2FA when active. Only Admin can do this action
     *
     * @param requestor - user requesting the change
     * @param body - user and state requested
     * @param req - for tracing IP and audit log
     * @throws ITRightException
     * @throws ITNotFoundException
     * @throws ITParseException
     */
    public void twoFaStateChangeUser(String requestor, UserStateSwitchBody body, HttpServletRequest req )
            throws ITRightException, ITNotFoundException, ITParseException {

        if ( body.getLogin() == null || body.getLogin().isEmpty() ) {
            throw new ITParseException("user-profile-login-invalid");
        }

        if (body.isState()) {
            throw new ITParseException("user-profile-2fa-enable-not-allowed");
        }

        try {
            User _requestor = userCache.getUser(requestor);

            if ( !userCommon.isLegitAccessRead(_requestor,body.getLogin(),true) ) {
                log.warn("[users] Requestor {} does not have write access right to user {} profile", requestor, body.getLogin());
                throw new ITRightException("user-profile-no-access");
            }

            User _user = null;
            try {
                _user = userCache.getUser(body.getLogin());
            } catch (ITNotFoundException x){
                log.warn("[users] Searched user does not exists", x);
                throw new ITRightException("user-profile-user-not-found");
            }

            if ( _user.getTwoFAType() == TwoFATypes.NONE ) {
                throw new ITParseException("user-profile-2fa-not-enabled");
            }

            // user found, rights verified, we can proceed
            _user.setTwoFAType(TwoFATypes.NONE);
            userCache.saveUser(_user);                      // save & flush caches
            auditIntegration.auditLog(
                    ModuleCatalog.Modules.USERS,
                    ActionCatalog.getActionName(ActionCatalog.Actions.TWOFACTOR_CHANGE),
                    _user.getLogin(),
                    "User {0} has deactivate second factor from {1}",
                    new String[]{_requestor.getLogin(), (req.getHeader("x-real-ip") != null) ? req.getHeader("x-real-ip") : "Unknown"}
            );
        } catch (ITNotFoundException x) {
            log.error("[users] Requestor does not exists", x);
            throw new ITRightException("user-profile-user-not-found");
        }
    }

    // ==========================================================================
    // Update focussing on rights
    // ==========================================================================

    @Autowired
    protected UsersRolesCache usersRolesCache;

    @Autowired
    protected GroupsServices groupsServices;

    /**
     * Sub Function - Update Role, the _resquestor and _user are already identified
     * @param _requestor - Who is requesting the change
     * @param _user - Who is having its roles updated
     * @param body - The role list expected
     * @param req - The request for IP tracing
     * @throws ITRightException - When there is a request for a role the requestor cannot assign
     * @throws ITParseException - When the body is not correctly formatted or Role does not exist
     */
    protected void userUpdateRoles (
            User _requestor,
            User _user,
            UserUpdateBody body,
            HttpServletRequest req
    ) throws ITRightException, ITParseException {

        // A user may not be able to change his own role, non-sense
        boolean self = false;
        if ( _requestor.getLogin().compareTo(_user.getLogin()) == 0 ) {
            if ( !_requestor.isInRole(UsersRolesCache.StandardRoles.ROLE_GOD_ADMIN)) {
                throw new ITParseException("user-profile-role-self-change-not-allowed");
            }
            self = true;
        }

        // Only Admin can change roles
        if (   _requestor.isInRole(UsersRolesCache.StandardRoles.ROLE_GOD_ADMIN)
            || _requestor.isInRole(UsersRolesCache.StandardRoles.ROLE_USER_ADMIN)
        ) {
            // Admin can only change the role they have

            // First pass - add the missing roles
            ArrayList<String> rolesToAdd = new ArrayList<>();
            for ( String r : body.getRoles() ) {
                // Check the role... exists and is assignable
                try {
                    Role _r = usersRolesCache.getRole(r);
                    if ( ! _r.isAssignable() ) throw new ITParseException("user-profile-role-not-assignable");
                } catch (ITNotFoundException x) {
                    throw new ITParseException("user-profile-role-not-found");
                }

                // problem if the role is not owned by requestor and user (user can have a role the requestor does not have)
                if ( ! _requestor.isInRole(r) && ! _user.isInRole(r) && !_requestor.isInRole(UsersRolesCache.StandardRoles.ROLE_GOD_ADMIN)) {
                    log.warn("[users] Requestor {} does not have right to assign role {}", _requestor.getLogin(), r);
                    throw new ITRightException("user-profile-role-change-not-owned");
                }

                // Role exists and can be assigned, verify the user do not already own it
                if ( ! _user.isInRole(r) ) {
                    rolesToAdd.add(r);
                }
            }

            // Second pass - remove the roles not wanted anymore
            ArrayList<String> rolesToRemove = new ArrayList<>();
            for ( String r : _user.getRoles() ) {
                // Check the role... exists and is assignable
                try {
                    Role _r = usersRolesCache.getRole(r);
                    if ( ! _r.isAssignable() ) continue;
                } catch (ITNotFoundException x) {
                    continue;
                }

                // check the requestor has it, in this case the removal is possible
                if ( _requestor.isInRole(r) ) {
                    boolean found = false;
                    for ( String rr : body.getRoles() ) {
                        if ( rr.compareTo(r) == 0 ) {
                            // role must be kept
                            found = true;
                            break;
                        }
                    }
                    if ( !found ) {
                        // role onwed by user is no more in the role list, must be removed
                        // just avoid god admin self remove god admin role
                        if ( !self || r.compareTo(UsersRolesCache.StandardRoles.ROLE_GOD_ADMIN.getRoleName()) != 0 ) {
                            rolesToRemove.add(r);
                        }
                    }
                }
            }

            // Proceed to change
            for ( String r : rolesToAdd ) {
                _user.getRoles().add(r);
            }
            for ( String r : rolesToRemove ) {
                _user.getRoles().remove(r);
            }
            _user.setModificationDate(Now.NowUtcMs());
            userCache.saveUser(_user);

            // Audit logs
            for ( String r : rolesToAdd ) {
                auditIntegration.auditLog(
                        ModuleCatalog.Modules.USERS,
                        ActionCatalog.getActionName(ActionCatalog.Actions.ROLE_CHANGE),
                        _user.getLogin(),
                        "User {0} added role {1} from {2}",
                        new String[]{_requestor.getLogin(), r, (req.getHeader("x-real-ip") != null) ? req.getHeader("x-real-ip") : "Unknown"}
                );
            }
            for ( String r : rolesToRemove ) {
                auditIntegration.auditLog(
                        ModuleCatalog.Modules.USERS,
                        ActionCatalog.getActionName(ActionCatalog.Actions.ROLE_CHANGE),
                        _user.getLogin(),
                        "User {0} removed role {1} from {2}",
                        new String[]{_requestor.getLogin(), r, (req.getHeader("x-real-ip") != null) ? req.getHeader("x-real-ip") : "Unknown"}
                );
            }

        } else {
            throw new ITRightException("user-profile-role-change-not-authorized");
        }
    }


    /**
     * Sub Function - Update Groups, the _resquestor and _user are already identified
     * @param _requestor - Who is requesting the change
     * @param _user - Who is having its group updated
     * @param body - The group list expected
     * @param req - The request for IP tracing
     * @throws ITRightException - When there is a request for a group the requestor cannot assign
     * @throws ITParseException - When the body is not correctly formatted or group does not exist
     */
    protected void userUpdateGroups (
            User _requestor,
            User _user,
            UserUpdateBody body,
            HttpServletRequest req
    ) throws ITRightException, ITParseException {

        // A user can remove a group, in this case, we can have a deletion cascade
        // But no addition
        boolean selfRequest = false;
        if ( _requestor.getLogin().compareTo(_user.getLogin()) == 0 ) {
            selfRequest = true;
        }

        // First pass add groups
        ArrayList<String> groupsToAdd = new ArrayList<>();
        ArrayList<String> aclToRemove = new ArrayList<>();
        for ( String g : body.getGroups() ) {

            try {
                Group _g = groupsServices.getGroupByShortId(g);
            } catch (ITNotFoundException x) {
                throw new ITParseException("user-profile-group-not-found");
            }

            // Make sure we can affect that group as ownership
            if (   ! groupsServices.isUserInGroup(_requestor,g,false,false,true)   // not owned by requestor
                && !_requestor.isInRole(UsersRolesCache.StandardRoles.ROLE_GOD_ADMIN)                            // not GOD_ADMIN
            ) {
                if ( ! selfRequest || ! _requestor.isInGroup(g,false,false) ) {
                    // we can give ownership for a group we do not own but GOD_ADMIN can do anything
                    throw new ITRightException("user-profile-group-change-not-owned");
                }
            }

            // We can add this group if the user does not already own it.
            if ( ! groupsServices.isUserInGroup(_user,g,false, true, false)  ) {
                boolean inAcl = false;
                if ( groupsServices.isUserInGroup(_user,g,true, true, false) ) {
                    // In this case, the group is not owned but there is an ACL on the group... we need to remove the ACL
                    // and transform into ownership
                    inAcl = true;
                }
                // when exists
                try {
                    groupsServices.getGroupByShortId(g);
                    groupsToAdd.add(g);
                    if ( inAcl ) aclToRemove.add(g);
                } catch (ITNotFoundException x) {
                    throw new ITParseException("user-profile-group-not-found");
                }
            }
        }

        // Second pass - remove groups
        ArrayList<String> groupsToRemove = new ArrayList<>();
        for ( String g : _user.getGroups() ) {
            boolean found = false;
            for ( String gg : body.getGroups() ) {
                if  (groupsServices.isInGroup(g,gg) ) { // We need to make sure it is not here because we have it in the hierarchy
                    found = true;
                    break;
                }
            }
            // verify this group can be removed by the requestor or skip it
            if ( !found ) {
                if (groupsServices.isUserInGroup(_requestor, g, false, false, true)) {
                    // the user can, so it's a removal
                    groupsToRemove.add(g);
                }
            }
        }

        // Proceed to change
        for ( String g : groupsToAdd ) {
            _user.getGroups().add(g);
            // resume a purgatory group in case
            // only GOD_ADMIN can reactivate a group when it has no ownership, before deletion
            if ( _requestor.isInRole(UsersRolesCache.StandardRoles.ROLE_GOD_ADMIN)) {
                try {
                    Group _g = groupsServices.getGroupByShortId(g);
                    if ( ! _g.isActive() ) {
                        groupsServices.groupCascadeResume(g);
                    }
                } catch (ITNotFoundException x) {}
            }
        }
        for ( String g : groupsToRemove ) {
            _user.getGroups().remove(g);
        }
        // An ACL can be replaced by a hierarchy
        for ( String acl : aclToRemove ) {
            String g = groupsServices.findGroupUserForGroup(_user,acl,true,false,false);
            if ( g != null ) {
                UserAcl found = null;
                for (UserAcl _acl : _user.getAcls()) {
                    if (_acl.getGroup().compareTo(g) == 0) {
                        found = _acl;
                        break;
                    }
                }
                _user.getAcls().remove(found);
            }
        }
        _user.setModificationDate(Now.NowUtcMs());
        userCache.saveUser(_user);

        // Audit logs
        for ( String r : groupsToAdd ) {
            auditIntegration.auditLog(
                    ModuleCatalog.Modules.USERS,
                    ActionCatalog.getActionName(ActionCatalog.Actions.GROUP_CHANGE),
                    _user.getLogin(),
                    "User {0} added group {1} from {2}",
                    new String[]{_requestor.getLogin(), r, (req.getHeader("x-real-ip") != null) ? req.getHeader("x-real-ip") : "Unknown"}
            );
        }
        for ( String r : groupsToRemove ) {
            auditIntegration.auditLog(
                    ModuleCatalog.Modules.USERS,
                    ActionCatalog.getActionName(ActionCatalog.Actions.GROUP_CHANGE),
                    _user.getLogin(),
                    "User {0} removed group {1} from {2}",
                    new String[]{_requestor.getLogin(), r, (req.getHeader("x-real-ip") != null) ? req.getHeader("x-real-ip") : "Unknown"}
            );
        }
        for ( String r : aclToRemove ) {
            auditIntegration.auditLog(
                    ModuleCatalog.Modules.USERS,
                    ActionCatalog.getActionName(ActionCatalog.Actions.ACL_CHANGE),
                    _user.getLogin(),
                    "User {0} removed ACL {1} from {2} due to group ownership addition",
                    new String[]{_requestor.getLogin(), r, (req.getHeader("x-real-ip") != null) ? req.getHeader("x-real-ip") : "Unknown"}
            );
        }

        // Deletion Cascade for groups with no more owners
        for ( String g : groupsToRemove ) {
            groupsServices.groupCascadeDeletionIfNoOwner(g);
        }

    }

    /**
     * Sub Function - Update ACL, the _resquestor and _user are already identified
     * @param _requestor - Who is requesting the change
     * @param _user - Who is having its group updated
     * @param body - The group list expected
     * @param req - The request for IP tracing
     * @throws ITRightException - When there is a request for a group the requestor cannot assign
     * @throws ITParseException - When the body is not correctly formatted or group does not exist
     */
    protected void userUpdateAcls (
            User _requestor,
            User _user,
            UserUpdateBody body,
            HttpServletRequest req
    ) throws ITRightException, ITParseException {

        // A user can remove a group, in this case, we can have a deletion cascade
        // But no addition
        boolean selfRequest = false;
        if ( _requestor.getLogin().compareTo(_user.getLogin()) == 0 ) {
            selfRequest = true;
        }


        // First pass add acls
        ArrayList<UserAcl> aclToAdd = new ArrayList<>();
        ArrayList<UserAcl> aclToRemove = new ArrayList<>();
        boolean changeMade = false;
        for ( UserAcl a : body.getAcls() ) {

            // search the ACL in the existing ones
            boolean found = false;
            UserAcl userAcl = null;
            for (UserAcl aa : _user.getAcls()) {
                if (aa.getGroup().compareTo(a.getGroup()) == 0) {
                    userAcl = aa;
                    found = true;
                    break;
                }
            }

            // Process the modification of an existing ACL
            if (found) {
                // ACL is found, check the name
                if (userAcl.getLocalName().compareTo(a.getLocalName()) != 0) {
                    // rename the ACL
                    userAcl.setLocalName(a.getLocalName());
                    changeMade = true;
                }
                if (!selfRequest) {
                    // ACL is found, check the rights (self request may not change the rights)
                    // Comparer les rôles entre userAcl et a
                    ArrayList<String> rolesToAdd = new ArrayList<>();
                    ArrayList<String> rolesToRemove = new ArrayList<>();
                    for (String role : a.getRoles()) {
                        if (!userAcl.getRoles().contains(role)) {
                            rolesToAdd.add(role);
                        }
                    }
                    for (String role : userAcl.getRoles()) {
                        if (!a.getRoles().contains(role)) {
                            rolesToRemove.add(role);
                        }
                    }

                    // is requestor owns the group & the role
                    for (String add : rolesToAdd) {
                        // make sure that role exists and is assignable
                        try {
                            Role r = usersRolesCache.getRole(add);
                            if (!r.isAssignable()) throw new ITParseException("user-profile-role-not-assignable");
                        } catch (ITNotFoundException x) {
                            throw new ITParseException("user-profile-role-not-found");
                        }
                        // make sure the requestor owns the group and the role
                        // or is GOD_ADMIN
                        if (groupsServices.isUserInGroup(_requestor, userAcl.getGroup(), true, false, true)   // requestor own or have right on group
                                || _requestor.isInRole(UsersRolesCache.StandardRoles.ROLE_GOD_ADMIN)                            // or is GOD_ADMIN
                        ) {
                            if (_requestor.isInRole(add) || _requestor.isInRole(UsersRolesCache.StandardRoles.ROLE_GOD_ADMIN)) {
                                userAcl.getRoles().add(add);
                                changeMade = true;
                                auditIntegration.auditLog(
                                        ModuleCatalog.Modules.USERS,
                                        ActionCatalog.getActionName(ActionCatalog.Actions.ACL_CHANGE),
                                        _user.getLogin(),
                                        "User {0} add a role {3} in ACL {1} from {2}",
                                        new String[]{_requestor.getLogin(), userAcl.getGroup(), (req.getHeader("x-real-ip") != null) ? req.getHeader("x-real-ip") : "Unknown", add}
                                );
                            } else {
                                throw new ITRightException("user-profile-acl-change-not-owned");
                            }
                        } else {
                            throw new ITRightException("user-profile-acl-change-not-owned");
                        }
                    }
                    // remove roles when the requestor owns the role
                    for (String remove : rolesToRemove) {
                        // Make ure requestor owns the group and the role
                        // or is GOD_ADMIN
                        if (groupsServices.isUserInGroup(_requestor, userAcl.getGroup(), true, false, true)   // requestor own or have right on group
                                || _requestor.isInRole(UsersRolesCache.StandardRoles.ROLE_GOD_ADMIN)                            // or is GOD_ADMIN
                        ) {
                            if (_requestor.isInRole(remove) || _requestor.isInRole(UsersRolesCache.StandardRoles.ROLE_GOD_ADMIN)) {
                                userAcl.getRoles().remove(remove);
                                changeMade = true;
                                auditIntegration.auditLog(
                                        ModuleCatalog.Modules.USERS,
                                        ActionCatalog.getActionName(ActionCatalog.Actions.ACL_CHANGE),
                                        _user.getLogin(),
                                        "User {0} remove a role {3} in ACL {1} from {2}",
                                        new String[]{_requestor.getLogin(), userAcl.getGroup(), (req.getHeader("x-real-ip") != null) ? req.getHeader("x-real-ip") : "Unknown", remove}
                                );
                            } else {
                                throw new ITRightException("user-profile-acl-change-not-owned");
                            }
                        }
                    }
                }
                // Process the creation of a new ACL
            } else {
                // Check the group ownership for the requestor
                // or is GOD_ADMIN
                if (groupsServices.isUserInGroup(_requestor, a.getGroup(), true, false, true)   // requestor own or have right on group
                        || _requestor.isInRole(UsersRolesCache.StandardRoles.ROLE_GOD_ADMIN)                                         // or is GOD_ADMIN
                ) {
                    // Verify the requested Roles
                    // make sure that role exists and are assignable
                    for (String role : a.getRoles()) {
                        try {
                            Role r = usersRolesCache.getRole(role);
                            if (!r.isAssignable()) throw new ITParseException("user-profile-role-not-assignable");
                        } catch (ITNotFoundException x) {
                            throw new ITParseException("user-profile-role-not-found");
                        }
                        if (_requestor.isInRole(role) || _requestor.isInRole(UsersRolesCache.StandardRoles.ROLE_GOD_ADMIN)) {
                            // this role is ok
                            continue;
                        } else {
                            throw new ITRightException("user-profile-acl-change-not-owned");
                        }
                    }
                    // Don't we have it already in hierarchy ?
                    if ( groupsServices.isUserInGroup(_user, a.getGroup(), true, true, false) ) {
                        throw new ITParseException("user-profile-acl-change-already-member");
                    }

                    // All roles are ok, we can add the ACL
                    aclToAdd.add(a);
                }
            }
        }

        // Second pass - Scan ACLs to remove
        for ( UserAcl a : _user.getAcls() ) {
            boolean found = false;
            for ( UserAcl aa : body.getAcls() ) {
                if ( aa.getGroup().compareTo(a.getGroup()) == 0 ) {
                    found = true;
                    break;
                }
            }
            if ( !found ) {
                // verify the requestor can remove that ACL
                if (   groupsServices.isUserInGroup(_requestor, a.getGroup(), true, false, true)   // requestor own or have right on group
                    || _requestor.isInRole(UsersRolesCache.StandardRoles.ROLE_GOD_ADMIN)                                         // or is GOD_ADMIN
                ) {
                    aclToRemove.add(a);
                }
            }
        }

        // Proceed to change
        for ( UserAcl a : aclToAdd ) {
            _user.getAcls().add(a);
            changeMade=true;
        }
        for ( UserAcl a : aclToRemove ) {
            _user.getAcls().remove(a);
            changeMade = true;
        }
        if ( changeMade ) {
            _user.setModificationDate(Now.NowUtcMs());
            userCache.saveUser(_user);
        }

        // Audit logs
        for ( UserAcl a : aclToAdd ) {
            auditIntegration.auditLog(
                    ModuleCatalog.Modules.USERS,
                    ActionCatalog.getActionName(ActionCatalog.Actions.ACL_CHANGE),
                    _user.getLogin(),
                    "User {0} add an ACL for {1} with roles {3} from {2}",
                    new String[]{_requestor.getLogin(), a.getGroup(), (req.getHeader("x-real-ip") != null) ? req.getHeader("x-real-ip") : "Unknown", String.join(",", a.getRoles()) }
            );
        }
        for ( UserAcl a : aclToRemove ) {
            auditIntegration.auditLog(
                    ModuleCatalog.Modules.USERS,
                    ActionCatalog.getActionName(ActionCatalog.Actions.ACL_CHANGE),
                    _user.getLogin(),
                    "User {0} remove an ACL for {1} from {2}",
                    new String[]{_requestor.getLogin(), a.getGroup(), (req.getHeader("x-real-ip") != null) ? req.getHeader("x-real-ip") : "Unknown" }
            );
        }

    }



    /**
     * Update the user, not personal information. This part can be manipulated safely in regard of the GDRP
     * It mostly address the Roles, Group & ACLs management. Most of the modifications require admin rights
     * but not all of them (like when a user share access to a group to another user). The rights are checked
     * for each modification.
     *
     * @param requestor - who is requesting the update
     * @param body - the body of the request containing the user login and the profile information to update
     * @param req - for tracing IP and audit log
     * @throws ITRightException
     * @throws ITParseException
     * @throws ITNotFoundException
     */
    public void userUpdate(
            String requestor,
            UserUpdateBody body,
            HttpServletRequest req
    ) throws ITRightException, ITParseException, ITNotFoundException {

        // We need a body
        if ( body.getLogin() == null || body.getLogin().isEmpty() ) {
            throw new ITParseException("user-profile-login-invalid");
        }

        // We need to be the user ourselves or an admin
        User _requestor = null;
        User _user = null;
        try {
            _requestor = userCache.getUser(requestor);
            String user = body.getLogin();

            if (!userCommon.isLegitAccessRead(_requestor, user, true)) {
                log.warn("[users] Requestor {} does not have access right to user {} profile", requestor, user);
                throw new ITRightException("user-profile-no-access");
            }

            _user = _requestor;
            if (requestor.compareTo(user) != 0) {
                try {
                    _user = userCache.getUser(user);
                } catch (ITNotFoundException x) {
                    log.warn("[users] Searched user does not exists", x);
                    throw new ITNotFoundException("user-profile-user-not-found");
                }
            }
        } catch (ITNotFoundException x) {
            log.error("[users] Requestor does not exists", x);
            throw new ITRightException("user-profile-user-not-found");
        }

        // Now the requestor and user are identified
        // We can process block by block the update
        if ( body.isConsiderRoles() ) {
            // First process the roles, this raise Exception directly
            // User modifications also directly saved
            userUpdateRoles(_requestor, _user, body, req );
        }

        if ( body.isConsiderGroups() ) {
            // Second process the groups, this raise Exception directly
            // User modifications also directly saved
            userUpdateGroups(_requestor, _user, body, req );
        }

        if ( body.isConsiderACLs() ) {
            // Third process the ACLs, this raise Exception directly
            // User modifications also directly saved
            userUpdateAcls(_requestor, _user, body, req );
        }

    }

    @Autowired
    protected UserGroupRolesService userGroupRolesService;

    /**
     * Return the user profile information based on what is expected in terms of information.
     * Then it will be possible to use this content to update the user profile.
     * @param requestor - who is requesting the information
     * @param body - whose information we want and what kind of information
     * @param req - for tracing IP and audit log
     * @return the user profile information as requested
     * @throws ITRightException
     * @throws ITParseException
     */
    public UserUpdateBodyResponse getUserUpdateBodyFromUser(
        String requestor,
        UserUpdateBodyRequest body,
        HttpServletRequest req
    ) throws ITRightException, ITParseException {

        User _requestor = null;
        User _user = null;
        try {
            _requestor = userCache.getUser(requestor);
            String user = body.getLogin();

            if (!userCommon.isLegitAccessRead(_requestor, user, true)) {
                log.warn("[users] Requestor {} does not have access right to user {} profile", requestor, user);
                throw new ITRightException("user-profile-no-access");
            }

            _user = _requestor;
            if (requestor.compareTo(user) != 0) {
                try {
                    _user = userCache.getUser(user);
                } catch (ITNotFoundException x) {
                    log.warn("[users] Searched user does not exists", x);
                    throw new ITNotFoundException("user-profile-user-not-found");
                }
            }
        } catch (ITNotFoundException x) {
            log.error("[users] Requestor does not exists", x);
            throw new ITRightException("user-profile-user-not-found");
        }

        UserUpdateBodyResponse r = new UserUpdateBodyResponse();
        r.setLogin(_user.getLogin());
        if ( body.isConsiderRoles() ) {
            r.setConsiderRoles(true);
            r.setRoles(new ArrayList<>());
            for ( String _role : _user.getRoles() ) {
                try {
                    Role role = usersRolesCache.getRole(_role);
                    r.getRoles().add(RoleItf.getRoleItfFromRole(role));
                } catch ( ITNotFoundException x) {}
            }
        }
        if ( body.isConsiderGroups() ) {
            r.setConsiderGroups(true);
            r.setGroups(new ArrayList<>());
            if ( body.isConsiderSubs() ) {
                try {
                    List<GroupsHierarchySimplified> groups = userGroupRolesService.getAvailableGroups(_user.getLogin(),true,false,true);
                    for ( GroupsHierarchySimplified g : groups ) {
                        r.getGroups().add(GroupItf.getGroupItfFromGroupsHierarchySimplified(g));
                    }
                } catch (ITParseException x) {
                    throw x;
                }
            } else {
                for (String _g : _user.getAllGroups(true,false, true)) {
                    try {
                        Group g = groupsServices.getGroupByShortId(_g);
                        r.getGroups().add(GroupItf.getGroupItfFromGroup(g));
                    } catch (ITNotFoundException x) {
                    }
                }
            }
        }
        if ( body.isConsiderACLs() ) {
            r.setConsiderACLs(true);
            r.setAcls(new ArrayList<>());
            if ( body.isConsiderSubs() ) {
                try {
                    List<GroupsHierarchySimplified> groups = userGroupRolesService.getAvailableGroups(_user.getLogin(),false,true,false);
                    for ( GroupsHierarchySimplified g : groups ) {
                        // search the related ACL
                        for ( UserAcl a : _user.getAcls() ) {
                            if ( a.getGroup().compareTo(g.getShortId()) == 0 ) {
                                r.getAcls().add(AclItf.getAclItfFromGroupsHierarchySimplified(a,g));
                                break;
                            }
                        }
                    }
                } catch (ITParseException x) {
                    throw x;
                }
            } else {
                for ( UserAcl a : _user.getAcls() ) {
                    r.getAcls().add(AclItf.getAclItfFromUserAcl(a));
                }
            }
        }

        return r;
    }


}
