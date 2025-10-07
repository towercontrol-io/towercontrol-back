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
import com.disk91.common.tools.EmailTools;
import com.disk91.common.tools.HexCodingTools;
import com.disk91.common.tools.Now;
import com.disk91.common.tools.exceptions.ITNotFoundException;
import com.disk91.common.tools.exceptions.ITParseException;
import com.disk91.common.tools.exceptions.ITRightException;
import com.disk91.groups.config.GroupsConfig;
import com.disk91.users.api.interfaces.UserConfigResponse;
import com.disk91.users.api.interfaces.UserLoginBody;
import com.disk91.users.api.interfaces.UserLoginResponse;
import com.disk91.users.config.ActionCatalog;
import com.disk91.users.config.UserMessages;
import com.disk91.users.config.UsersConfig;
import com.disk91.users.mdb.entities.User;
import com.disk91.users.mdb.entities.sub.TwoFATypes;
import com.disk91.users.mdb.repositories.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.bouncycastle.util.encoders.Base32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.Supplier;

@Service
public class UserService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * The User Service covers functions for accessing users, including allowing the user to
     * create a session. It also manages encryption keys to automatically obfuscate
     * personal and sensitive data after a certain period. It handles the creation and destruction of
     * JWT tokens.
     */

    @Autowired
    protected CommonConfig commonConfig;

    @Autowired
    protected UsersConfig usersConfig;

    @Autowired
    protected UserCache userCache;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected AuditIntegration auditIntegration;

    @Autowired
    protected ParamRepository paramRepository;

    @Autowired
    protected EmailTools emailTools;

    @Autowired
    protected UserMessages userMessages;

    @Autowired
    protected GroupsConfig groupsConfig;

    /**
     * User Login verification, search for a corresponding user & email.
     * When the user has been deactivated, this reactivates the user. The
     * JWT token is generated based on the user information, including
     * groups. Salt for JWT is only regenerated on sign out to allow
     * multiple sessions.
     *
     * @param body
     * @param req
     * @return
     * @throws ITParseException
     * @throws ITRightException
     */
    public UserLoginResponse userLogin(
            UserLoginBody body,
            HttpServletRequest req
    ) throws ITParseException, ITRightException {

        this.incLoginAttempts();

        // Check the entry
        if (body.getEmail() == null || body.getEmail().isEmpty()) {
            this.incLoginFailed();
            throw new ITParseException("Email is empty");
        }
        if (body.getPassword() == null || body.getPassword().isEmpty()) {
            this.incLoginFailed();
            throw new ITParseException("Password is empty");
        }

        // Make sure the email is in lowercase
        body.setEmail(body.getEmail().toLowerCase());

        // Get the hash of the user login
        String loginHash = User.encodeLogin(body.getEmail());

        // Verify Brute Force Blocking
        if ( this.isBlocked(loginHash, (req.getHeader("x-real-ip") != null) ? req.getHeader("x-real-ip") : "Unknown")) {
            this.incLoginFailed();
            throw new ITParseException("Possible brute force attack");
        }

        try {
            User u = userCache.getUser(loginHash);
            // Check if authorized
            if ( u.isLocked() || !u.isActive() ) {
                log.info("[users][service] Inactive user {} attempted ot login", u.getLogin());
                this.incLoginFailed();
                throw new ITRightException("User is not authorized");
            }
            if ( u.getDeletionDate() > 0 ) {
                log.info("[users][service] User in purgatory {} attempted ot login", u.getLogin());
                this.incLoginFailed();
                throw new ITRightException("User is not authorized");
            }

            // Verify the user as the minimum role to login.
            if ( ! u.isInRole(UsersRolesCache.StandardRoles.ROLE_REGISTERED_USER) ) {
                log.info("[users][service] User {} does not have the right role to login", u.getLogin());
                this.incLoginFailed();
                throw new ITRightException("User does not have the right role");
            }

            // check the password
            u.setKeys(commonConfig.getEncryptionKey(), commonConfig.getApplicationKey());
            String _password = usersConfig.getUsersPasswordHeader() + body.getPassword() + usersConfig.getUsersPasswordFooter();
            if ( !u.isRightPassword(_password) ) {
                this.registerFailure(u, (req.getHeader("x-real-ip") != null) ? req.getHeader("x-real-ip") : "Unknown");
                this.incLoginFailed();
                throw new ITRightException("Invalid password");
            }

            // The password is correct, check if we need to reactivate the encryption key
            u.setLastLogin(Now.NowUtcMs());
            u.setCountLogin(u.getCountLogin()+1);
            boolean updateCaches = false;
            if ( u.getUserSecret() == null || u.getUserSecret().isEmpty() ) {
                log.info("[users][service] User {} reactivated", u.getLogin());
                u.restoreUserSecret(body.getPassword());
                // in case the search key was not set, set it now
                if ( u.getUserSearch() == null || u.getUserSearch().isEmpty() ) {
                    u.setEncLoginSearch(body.getEmail());
                }
                u.setModificationDate(Now.NowUtcMs());
                // add a trace of this action in the Audit Log
                auditIntegration.auditLog(
                        ModuleCatalog.Modules.USERS,
                        ActionCatalog.getActionName(ActionCatalog.Actions.REACTIVATION),
                        u.getLogin(),
                        "{0} Reactivated account after a long period of inactivity",
                        new String[]{body.getEmail()}
                );
                updateCaches = true;
            }

            // Write the user back to the database and request caches for flush (only if th key changed, if only the counters, keep wrong value in read-only cache)
            userRepository.save(u);
            if ( updateCaches ) {
                userCache.flushUser(u.getLogin());
            }

            Param p = paramRepository.findByParamKey("users.condition.version");
            UserLoginResponse response = new UserLoginResponse();
            response.setLogin(u.getLogin());
            response.setEmail(body.getEmail());
            response.setPasswordExpired( u.getExpiredPassword() > 0 && u.getExpiredPassword() < Now.NowUtcMs() );
            response.setConditionToValidate(( p != null && !p.getStringValue().isEmpty() && u.getConditionValidationVer().compareTo(p.getStringValue()) != 0 ));
            response.setTwoFARequired( u.getTwoFAType() != TwoFATypes.NONE );
            response.setTwoFAType(u.getTwoFAType());
            response.setTwoFAValidated(false);
            switch (u.getTwoFAType()) {
                case NONE -> response.setTwoFASize(0); // no 2FA
                case SMS -> response.setTwoFASize(4); // 2FA code is 4 bytes long
                case EMAIL,AUTHENTICATOR -> response.setTwoFASize(6); // Authenticator code is 6 bytes long
            }

            // Generate the Role list based on the user roles & status
            ArrayList<String> roles = new ArrayList<>();
            ArrayList<String> extraRoles = new ArrayList<>();
            roles.add(UsersRolesCache.StandardRoles.ROLE_LOGIN_1FA.getRoleName());
            boolean twoFaSession = false;
            if ( u.getTwoFAType() != TwoFATypes.NONE ) {
                // 2FA Mechanism is active, generate the token with a limited role 1FA
                roles.add(UsersRolesCache.StandardRoles.ROLE_REGISTERED_USER.getRoleName());
                twoFaSession = true; // the duration of the JWT will be reduces to 10 minutes, time to get the 2FA.
                switch(u.getTwoFAType()) {
                    case EMAIL -> {
                        // create a random hex string 4 bytes long
                        String code = HexCodingTools.getRandomHexString(4);
                        u.setEncTwoFASecret(Now.NowUtcMs()+"-"+code);   // store the time to manage timeout on code.
                        // Send an email with this string
                        Locale locale = emailTools.extractLocale(req, Locale.forLanguageTag(commonConfig.getCommonLangDefault()));
                        Object[] args = { commonConfig.getCommonServiceName(), code };
                        String _subject = userMessages.messageSource().getMessage("users.messages.2fa.email.subject", args, locale);
                        String _body = userMessages.messageSource().getMessage("users.messages.2fa.email.body", args, locale);
                        emailTools.send(body.getEmail(), _body, _subject, commonConfig.getCommonMailSender());
                        userCache.saveUser(u);
                    }
                    case SMS -> {
                        // create a random hex string 4 bytes long
                        String code = HexCodingTools.getRandomHexString(4);
                        u.setEncTwoFASecret(Now.NowUtcMs()+"-"+code);   // store the time to manage timeout on code.
                        // Send an SMS with this string
                        // @TODO - send SMS with the code
                        userCache.saveUser(u);
                    }
                }
            } else {
                // 2FA Mechanism is not active, generate the token with the full role list
                // check the password expiration to route the user to the password change page - update it
                if ( (u.getExpiredPassword() == 0 || u.getExpiredPassword() > Now.NowUtcMs() )
                        && ( p == null || p.getStringValue().isEmpty() || u.getConditionValidationVer().compareTo(p.getStringValue()) == 0 )
                ) {
                    extraRoles.addAll(u.getRoles());
                    roles.add(UsersRolesCache.StandardRoles.ROLE_LOGIN_COMPLETE.getRoleName());
                } else {
                    // Reduce the role to the 1FA ROLE and ROLE_REGISTERED_USER to allow password change or condition validation but not more
                    roles.add(UsersRolesCache.StandardRoles.ROLE_REGISTERED_USER.getRoleName());
                }
            }
            response.setJwtRenewalToken(this.generateJWTForUser(u,roles,twoFaSession,true));
            roles.addAll(extraRoles);
            response.setJwtToken(this.generateJWTForUser(u, roles, twoFaSession,false));

            // Add audit trace
            auditIntegration.auditLog(
                    ModuleCatalog.Modules.USERS,
                    ActionCatalog.getActionName(ActionCatalog.Actions.LOGIN),
                    u.getLogin(),
                    "Login from {0}",
                    new String[]{(req.getHeader("x-real-ip") != null) ? req.getHeader("x-real-ip") : "Unknown"}
            );
            // And metrics
            this.incLoginSuccess();
            return response;

        } catch (ITNotFoundException x ) {
            this.incLoginFailed();
            throw new ITRightException("User not found");
        }
    }


    /**
     * Upgrade the user session, after fixing the password expiration of User Condition acceptance or the 2FA
     * the user can upgrade its session to access the full API. This will return a new JWT token if all the
     * condition are met. This can be called multiple times like for 2FA, then to validate password expiration...
     *
     * @param user
     * @param req
     * @return
     * @throws ITParseException
     * @throws ITRightException
     */
    public UserLoginResponse upgradeSession(
            String user,
            String twoFaCode,
            HttpServletRequest req
    ) throws ITParseException, ITRightException {

        this.incLoginAttempts();

        // Check the entry
        if (user == null || user.isEmpty()) {
            this.incLoginFailed();
            throw new ITParseException("user-upgrade-missing-login");
        }

        // Get the list of Roles currently in the JWT
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        boolean has1fa = false;
        boolean has2fa = false;
        boolean hasComplete = false;

        for ( GrantedAuthority a : authorities ) {
            if (a.getAuthority().equalsIgnoreCase("ROLE_LOGIN_1FA")) has1fa=true;
            if (a.getAuthority().equalsIgnoreCase("ROLE_LOGIN_2FA")) has2fa=true;
            if (a.getAuthority().equalsIgnoreCase("ROLE_LOGIN_COMPLETE")) hasComplete=true;
        }

        // Make sure the user have the minimum role to login
        if ( !has1fa && !has2fa && !hasComplete ) {
            this.incLoginFailed();
            throw new ITRightException("user-upgrade-missing-role");
        }

        // Verify Brute Force Blocking
        if ( this.isBlocked(user, (req.getHeader("x-real-ip") != null) ? req.getHeader("x-real-ip") : "Unknown")) {
            this.incLoginFailed();
            throw new ITParseException("user-upgrade-brute-force");
        }


        // Search the user
        try {
            User u = userCache.getUser(user);
            u.setKeys(commonConfig.getEncryptionKey(), commonConfig.getApplicationKey());

            // check if the user password & condition is ok
            Param p = paramRepository.findByParamKey("users.condition.version");
            UserLoginResponse response = new UserLoginResponse();
            response.setLogin(u.getLogin());
            response.setEmail(u.getEncEmail());
            response.setPasswordExpired( u.getExpiredPassword() > 0 && u.getExpiredPassword() < Now.NowUtcMs() );
            response.setConditionToValidate(( p != null && !p.getStringValue().isEmpty() && u.getConditionValidationVer().compareTo(p.getStringValue()) != 0 ));
            response.setTwoFARequired( u.getTwoFAType() != TwoFATypes.NONE );
            response.setTwoFAType(u.getTwoFAType());
            response.setTwoFAValidated(false); // default value, will be set to true if the 2FA is ok
            switch (u.getTwoFAType()) {
                case NONE -> response.setTwoFASize(0); // no 2FA
                case EMAIL,SMS -> response.setTwoFASize(4); // 2FA code is 4 hexString long
                case AUTHENTICATOR -> response.setTwoFASize(6); // Authenticator code is 6 bytes long
            }

            // Generate the Role list based on the user roles & status
            ArrayList<String> roles = new ArrayList<>();
            roles.add(UsersRolesCache.StandardRoles.ROLE_LOGIN_1FA.getRoleName());
            boolean sessionOk = false;
            if ( u.getTwoFAType() != TwoFATypes.NONE && !has2fa ) {
                // 2FA Mechanism is active, verify the code
                switch ( u.getTwoFAType() ) {
                    case EMAIL, SMS -> {
                        if (     twoFaCode != null              // we have a code provided
                             && !twoFaCode.isEmpty()            // not empty
                             && this.isTOTPCodeValid(u,twoFaCode)      // with a correct value
                        ) {
                            // The 2FA code is Validated
                            roles.add(UsersRolesCache.StandardRoles.ROLE_LOGIN_2FA.getRoleName());
                            response.setTwoFAValidated(true);
                            sessionOk = true;
                        } else if ( twoFaCode != null              // we have a code provided
                                && !twoFaCode.isEmpty()            // not empty
                        ) {
                            // this can be a brute force attack
                            this.registerFailure(u,(req.getHeader("x-real-ip") != null) ? req.getHeader("x-real-ip") : "Unknown");
                        }
                    }
                    case AUTHENTICATOR -> {
                        if (    twoFaCode != null                       // a code is provided
                             && twoFaCode.length() == 6                 // the size is correct
                             && twoFaCode.matches("[0-9]+")       // only digits
                             && this.isTOTPCodeValid(u, twoFaCode)      // the code is the right one
                        ) {
                            // The 2FA code is Validated
                            roles.add(UsersRolesCache.StandardRoles.ROLE_LOGIN_2FA.getRoleName());
                            response.setTwoFAValidated(true);
                            sessionOk = true;
                        } else if (    twoFaCode != null                       // a code is provided
                                && twoFaCode.length() == 6                     // the size is correct
                                && twoFaCode.matches("[0-9]+")           // only digits
                        ) {
                            // Even if the code is rolling, we can imagine someone trying to brute force the code
                            // in the limited amount of time the code is valid.
                            this.registerFailure(u,(req.getHeader("x-real-ip") != null) ? req.getHeader("x-real-ip") : "Unknown");
                        }
                    }
                }
            } else if ( has2fa ) {
                // 2FA already validated
                roles.add(UsersRolesCache.StandardRoles.ROLE_LOGIN_2FA.getRoleName());
                response.setTwoFAValidated(true);
            } else {
                // 2FA, not required
                sessionOk = true;
            }

            // check the password expiration to route the user to the password change page - update it
            if ( sessionOk
                    && ( u.getExpiredPassword() == 0 || u.getExpiredPassword() > Now.NowUtcMs() )
                    && ( p == null || p.getStringValue().isEmpty() || u.getConditionValidationVer().compareTo(p.getStringValue()) == 0 )
            ) {
                // all good make the final session
                roles.add(UsersRolesCache.StandardRoles.ROLE_LOGIN_COMPLETE.getRoleName());
                response.setJwtRenewalToken(this.generateJWTForUser(u,roles,false,true));
                roles.addAll(u.getRoles());
                response.setJwtToken(this.generateJWTForUser(u, roles, false,false));
                this.incLoginSuccess();
            } else if ( sessionOk ) {
                // 2FA ok, but some problem with the password or condition we have a long session with limited roles
                response.setJwtRenewalToken(this.generateJWTForUser(u,roles,false,true));
                response.setJwtToken(this.generateJWTForUser(u, roles, false,false));
            } else {
                // Missing 2FA, renew the 1FA Tokens, that's it
                response.setJwtRenewalToken(this.generateJWTForUser(u,roles,true,true));
                response.setJwtToken(this.generateJWTForUser(u, roles, true,false));
            }

            incLoginSuccess();
            return response;

        } catch ( ITNotFoundException x) {
            this.incLoginFailed();
            throw new ITParseException("user-upgrade-missing-user");
        }

    }


    /**
     * On every 24 hours, we scan the user database to identify the Users not connected since the expiration period
     * to deactivate the ability to decrypt the personal & sensitive data by removing the userSecret key. This one will
     * be regenerated on the next login from the password.
     * This does not apply to API users
     */
    @Scheduled(fixedRateString = "PT24H", initialDelayString = "PT1H")
    protected void clearPrivacyDataOnUnusedAccounts() {
        long exp = Now.NowUtcMs() - (usersConfig.getUsersDataPrivacyExpirationDays()*Now.ONE_FULL_DAY);
        List<User> users = userRepository.findExpiratedUsers(exp);
        for (User u : users) {
            if ( u.getUserSecret() != null && !u.getUserSecret().isEmpty() ) {
                log.info("[users][service] User {} privacy data expired, removing user secret", u.getLogin());
                u.setUserSecret("");
                u.setModificationDate(Now.NowUtcMs());
                userRepository.save(u);
                userCache.flushUser(u.getLogin());
            }
        }
    }


    /**
     * Create a JWT token for the user with the given list of roles
     * The role list must be created according to the user roles outside this function
     *
     * @param u
     * @param roles
     * @param twoSession - if true, the expiration time is reduced to 10 minutes (2FA)
     * @param renewal - when true, the token is a renewal token, it gets a longer expiration time, does not apply to 2FA
     * @return
     */
    protected String generateJWTForUser(User u, ArrayList<String> roles, boolean twoSession, boolean renewal) {
        long exp;
        if ( twoSession ) {
            if ( usersConfig.getUsersSession2faTimeoutSec() > 0 ) {
                exp = Now.NowUtcMs() + usersConfig.getUsersSession2faTimeoutSec() * 1000;
            } else {
                exp = Now.NowUtcMs() + 10 * Now.ONE_MINUTE; // 10 minutes minium
            }
        } else {
            if (u.isApiAccount()) {
                if (usersConfig.getUsersSessionApiTimeoutSec() > 0) {
                    exp = Now.NowUtcMs() + usersConfig.getUsersSessionApiTimeoutSec() * 1000;
                } else {
                    exp = Now.NowUtcMs() + 30 * 365 * Now.ONE_FULL_DAY; // inifinite session is 30 Years
                }
            } else {
                if (usersConfig.getUsersSessionTimeoutSec() > 0) {
                    exp = Now.NowUtcMs() + usersConfig.getUsersSessionTimeoutSec() * 1000;
                } else {
                    exp = Now.NowUtcMs() + 30 * 365 * Now.ONE_FULL_DAY; // inifinite session is 30 Years
                }
            }
            if ( renewal ) {
                exp += usersConfig.getUsersSessionRenewalExtraSec() * 1000; // The renewal token is valid a bit more time bt have less rights
            }
        }
        Claims claims = Jwts.claims()
                .subject(u.getLogin())
                .expiration(new Date(exp))
                .add("roles", roles)
                .build();

        return Jwts.builder()
                .header().add("typ", "JWT")
                .add("sub", u.getLogin())
                .and()
                .claims().empty().add(claims)
                .and()
                .expiration(new Date(exp))
                .signWith(this.generateKeyForUser(u))
                .compact();
    }


    /**
     * Compute a key to sign the JWT token for the user
     * This key is based on the user Session key, this one is renewed on user logout
     * and the server session key. This one is statis but can be renewed to clear all the
     * existing sessions
     *
     * @param u
     * @return
     */
    public Key generateKeyForUser(User u) {
        // Generate the key for the user
        String srvKey = usersConfig.getUsersSessionKey();
        String userSecret = u.getSessionSecret();
        byte[] _srvKey = HexCodingTools.getByteArrayFromHexString(srvKey);          // 32 bytes
        byte[] _userSecret = HexCodingTools.getByteArrayFromHexString(userSecret);  // 32 bytes
        byte[] key = new byte[64];
        for (int i = 0; i < 32; i++) {
            key[i] = (byte)(_srvKey[i] ^ _userSecret[i]);
            key[i+32] = (byte)(_srvKey[i] ^ _userSecret[i]);
        }
        return Keys.hmacShaKeyFor(key);
    }

    // --------------------------------------------------------------
    // TOTP Management / RFC 6238
    // --------------------------------------------------------------

    /**
     * Verify the TOTP code for the given user, from an API request
     * @param user - user login
     * @param secondFactor - code to verify
     * @param req - to get Request IP eventually
     * @return true when the given code is valid
     * @throws ITParseException
     * @throws ITRightException
     */
    public boolean verifyTOTPCode(
            String user,
            String secondFactor,
            HttpServletRequest req
    ) throws ITParseException, ITRightException {
        try {
            User _user = userCache.getUser(user);
            _user.setKeys(commonConfig.getEncryptionKey(), commonConfig.getApplicationKey());
            return this.isTOTPCodeValid(_user, secondFactor);
        } catch (ITNotFoundException x){
            log.warn("[users] Searched user does not exists", x);
            throw new ITRightException("user-profile-user-not-found");
        }

    }

    /**
     * Check the TOTP code, return true when the code is valid.
     * @param u
     * @param code
     * @return
     */
    public boolean isTOTPCodeValid(User u, String code) {
        switch ( u.getTwoFAType() ) {
            case EMAIL, SMS ->{
                try {
                    String[] codes = u.getEncTwoFASecret().split("-");
                    if (codes.length != 2) {
                        log.error("[users] Internal failure, 2FA code format");
                        return false;
                    }
                    long time = Long.parseLong(codes[0]);
                    if (code != null               // we have a code provided
                            && !code.isEmpty()            // not empty
                            && (time + usersConfig.getUsersSession2faTimeoutSec() * 1000) > Now.NowUtcMs() // in the validity period
                            && code.equals(codes[1])      // with a correct value
                    ) {
                        return true; // The 2FA code is Validated
                    }
                } catch (ITParseException x) {
                    log.error("[users] Internal failure, 2FA code format in invalid");
                    return false;
                }
            }
            case AUTHENTICATOR -> {
                try {
                    long now = Now.NowUtcMs() - 30_000; // previous block;
                    for ( int i = 0 ; i < 3 ; i++ ) {
                        long _code = this.getTotpCodeForTimeRef(u, now);
                        if ( _code == Long.parseLong(code) ) {
                            return true;
                        }
                        now += 30_000; // next block
                    }
                    return false;
                } catch (ITParseException e) {
                    log.error("[users] Internal failure, 2FA code format invalid");
                    return false;
                }
            }
            case NONE -> {
                return true;
            }
        }
        return false;
    }


    /**
     * Get a TOTP code for the given user and time reference, this follows the RFC 6238
     *
     * @param u - User where we can find the secret
     * @param timeRefMs - the time reference for this code in MS, good to test current, previous and next in case of clock shift
     * @return - the TOTP code for the given user and time reference
     * @throws ITParseException
     */
    private long getTotpCodeForTimeRef(User u, long timeRefMs)
    throws ITParseException {

        byte[] secret = Base32.decode(u.getEncTwoFASecret());

        // The TimeRefMs is the current time but we can go for previous session eventually
        // The timeRef is the time in seconds since 1970, divided by 30s slots
        long timeRef = timeRefMs / 30_000;

        byte[] _timeRef = new byte[8];
        for (int i = 7; i >= 0; i--) {
            _timeRef[i] = (byte) (timeRef & 0xFF);
            timeRef >>= 8;
        }

        try {
            SecretKeySpec signature = new SecretKeySpec(secret, "HmacSHA1");
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(signature);
            byte[] hash = mac.doFinal(_timeRef);

            // hash[19] gives the offset in hash for calculating the code, a value between 0 and 15
            int s = hash[19] & 0x0F;
            // The code is composed by the 4 bytes starting at the offset ; the first one is masked to
            // make sure we have a positive value
            long code = hash[s] & 0x7F;
            code = (code << 8) | (hash[s + 1] & 0xFF);
            code = (code << 8) | (hash[s + 2] & 0xFF);
            code = (code << 8) | (hash[s + 3] & 0xFF);
            // The code value is in between 0 and 999 999 (included)
            return code % 1000000;

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("[users] Internal failure, TOTP generation failure");
            throw new ITParseException("user-totp-generation-failure");
        }
    }


    // ==========================================================================
    // Metrics
    // ==========================================================================

    @Autowired
    protected MeterRegistry meterRegistry;

    @PostConstruct
    private void initUserRegistrationService() {
        log.info("[users][service] User service initialized");
        Gauge.builder("users_service_login_attempt", this.getLoginAttempts())
                .description("Number of login attempts")
                .register(meterRegistry);
        Gauge.builder("users_service_login_failed", this.getLoginFailed())
                .description("Number of login failures")
                .register(meterRegistry);
        Gauge.builder("users_service_login_success", this.getCreationsSuccess())
                .description("Number of login success")
                .register(meterRegistry);
    }

    private long loginAttempts = 0;
    private long loginFailed = 0;
    private long loginSuccess = 0;

    protected synchronized void incLoginAttempts() {
        loginAttempts++;
    }
    protected synchronized void incLoginFailed() {
        loginFailed++;
    }
    protected synchronized void incLoginSuccess() {
        loginSuccess++;
    }

    protected Supplier<Number> getLoginAttempts() {
        return ()-> loginAttempts;
    }
    protected Supplier<Number> getLoginFailed() {
        return ()-> loginFailed;
    }
    protected Supplier<Number> getCreationsSuccess() {
        return ()-> loginSuccess;
    }

    // ==========================================================================
    // Brute force protection
    // ==========================================================================

    protected static class FailureCounter {
        public String login;
        public String ip;
        public long count;
        public long lastAttempt;

        public FailureCounter(
                String login,
                String ip,
                long count,
                long lastAttempt
        ) {
            this.login = login;
            this.ip = ip;
            this.count = count;
            this.lastAttempt = lastAttempt;
        }
    }


    // This will store the failures by login and by IP, We will have a clean
    // mechanism, a limit of attempts in the given period of time by IP and
    // by login. We also have a limit in term of size of the hashmap. All parameters
    protected final HashMap<String, FailureCounter> failureCounters = new HashMap<>();

    /*
     * When a new login or session upgrade is received, we verify the login attempt is
     * lower than the limit fixed by users.session.security.max.login.failed for the user and
     * users.session.security.max.ip.failed for the IP. The number of trial per IP is higher
     * due to the possible multiple users behind a NAT in companies or university. This only
     * counts the failures. When the failure max is reached, the next attempts are blocked
     * and the 2FA code if exist is reset.
     * If the hashmap size limit has been reached, all the errors make the 2fa to be reset.
     * on every 5 minutes, the cache is cleaned to remove the old entries (no schedule, executed
     * on call)
     */

    private long lastFailureCountersClean = Now.NowUtcMs();

    /**
     * Call the function on every failure we want to keep trace. Creates or update
     * the entry for the UserLogin and for the IP
     * @param user
     * @param ip
     */
    protected synchronized void registerFailure(User user, String ip) {

        // Clean the cache
        if ((Now.NowUtcMs() - lastFailureCountersClean) > 5 * Now.ONE_MINUTE) {
            lastFailureCountersClean = Now.NowUtcMs();
            ArrayList<String> toClean = new ArrayList<>();
            for ( String fck : failureCounters.keySet()) {
                FailureCounter fc =  failureCounters.get(fck);
                if ( fc != null && fc.lastAttempt < ( Now.NowUtcMs() - usersConfig.getUsersSessionSecurityBlockPeriod()*1000)) {
                    // candidate for cleanup
                    toClean.add(fck);
                }
            }
            synchronized (failureCounters) {
                for ( String s : toClean ) {
                    failureCounters.remove(s);
                }
            }
        }

        // Search if user exists
        FailureCounter fcUser = failureCounters.get(user.getLogin());
        if ( fcUser == null ) {
            // create a new one
            if ( failureCounters.size() < usersConfig.getUsersSessionSecurityHashMapSize() ) {
                // Ok we can make it
                fcUser = new FailureCounter(user.getLogin(), ip, 1, Now.NowUtcMs());
                failureCounters.put(user.getLogin(),fcUser);
            } else {
                // We can't use the hashmap, remove the 2FA secret we are at risk
                if ( user.getTwoFAType() == TwoFATypes.EMAIL ) {
                    user.setTwoFASecret("");
                    userCache.saveUser(user);
                }
            }
        } else {
            // update structure
            fcUser.lastAttempt=Now.NowUtcMs();
            fcUser.count++;
        }

        // Search by IP
        FailureCounter fcIp = failureCounters.get(ip);
        if ( fcIp == null ) {
            // create a new one
            if ( failureCounters.size() < usersConfig.getUsersSessionSecurityHashMapSize() ) {
                // Ok we can make it
                fcIp = new FailureCounter(user.getLogin(), ip, 1, Now.NowUtcMs());
                failureCounters.put(ip,fcIp);
            } else {
                // We can't use the hashmap, remove the 2FA secret we are at risk
                if ( user.getTwoFAType() == TwoFATypes.EMAIL ) {
                    user.setTwoFASecret("");
                    userCache.saveUser(user);
                }
            }
        } else {
            // update structure
            fcIp.lastAttempt=Now.NowUtcMs();
            fcIp.count++;
        }
    }

    /**
     * Check if the user of ip should be blocked based on the failures, return true
     * when the user/IP is blocked.
     *
     * @param user
     * @param ip
     * @return
     */
    protected boolean isBlocked(String user, String ip) {
        synchronized (failureCounters) {
            FailureCounter fcUser = failureCounters.get(user);
            if (fcUser != null
                    && fcUser.lastAttempt > (Now.NowUtcMs() - usersConfig.getUsersSessionSecurityBlockPeriod() * 1000) // in a validity perior
                    && fcUser.count > usersConfig.getUsersSessionSecurityMaxLoginFailed()
            ) {
                fcUser.lastAttempt = Now.NowUtcMs();
                return true;
            }
            FailureCounter fcIp = failureCounters.get(ip);
            if (fcIp != null
                    && fcIp.lastAttempt > (Now.NowUtcMs() - usersConfig.getUsersSessionSecurityBlockPeriod() * 1000) // in a validity perior
                    && fcIp.count > usersConfig.getUsersSessionSecurityMaxIpFailed()
            ) {
                fcIp.lastAttempt = Now.NowUtcMs();
                return true;
            }
        }
        return false;
    }

    /**
     * Get the configuration of the user module to apply on the frontend behavior
     */
    protected UserConfigResponse userConfigResponse = null;
    public UserConfigResponse getUserModuleConfig() {
        if ( userConfigResponse == null ) {
            userConfigResponse = new UserConfigResponse();
            userConfigResponse.setAutoValidation(usersConfig.isUsersPendingAutoValidation());
            userConfigResponse.setEulaRequired(usersConfig.isUsersCreationNeedEula());
            userConfigResponse.setInvitationCodeRequired(usersConfig.isUsersRegistrationWithInviteCode());
            userConfigResponse.setSelfRegistration(usersConfig.isUsersRegistrationSelf());
            userConfigResponse.setRegistrationLinkByEmail(usersConfig.isUsersRegistrationLinkByEmail());
            userConfigResponse.setPasswordMinSize(usersConfig.getUsersPasswordMinSize());
            userConfigResponse.setPasswordMinUpperCase(usersConfig.getUsersPasswordMinUppercase());
            userConfigResponse.setPasswordMinLowerCase(usersConfig.getUsersPasswordMinLowercase());
            userConfigResponse.setPasswordMinSymbols(usersConfig.getUsersPasswordMinSymbols());
            userConfigResponse.setPasswordMinDigits(usersConfig.getUsersPasswordMinNumbers());
            userConfigResponse.setDeletionPurgatoryDelayHours(usersConfig.getUsersDeletionPurgatoryDuration());
            userConfigResponse.setSubGroupUnderVirtualAllowed(groupsConfig.isGroupVituralAllowsSub());
        }
        return userConfigResponse;
    }

}
