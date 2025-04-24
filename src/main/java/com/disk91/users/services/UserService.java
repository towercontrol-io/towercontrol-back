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
import com.disk91.common.tools.HexCodingTools;
import com.disk91.common.tools.Now;
import com.disk91.common.tools.exceptions.ITNotFoundException;
import com.disk91.common.tools.exceptions.ITParseException;
import com.disk91.common.tools.exceptions.ITRightException;
import com.disk91.users.api.interfaces.UserLoginBody;
import com.disk91.users.api.interfaces.UserLoginResponse;
import com.disk91.users.config.ActionCatalog;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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

        // Get the hash of the user login
        String loginHash = User.encodeLogin(body.getEmail());

        try {
            User u = userCache.getUser(loginHash);
            // Check if authorized
            if ( u.isLocked() || !u.isActive() ) {
                log.info("[users][service] Inactive user {} attempted ot login", u.getLogin());
                this.incLoginFailed();
                throw new ITRightException("User is not authorized");
            }

            // Verify the user as the minimum role to login.
            if ( ! u.isInRole(UsersRolesCache.StandardRoles.ROLE_REGISTERED_USER.getRoleName()) ) {
                log.info("[users][service] User {} does not have the right role to login", u.getLogin());
                this.incLoginFailed();
                throw new ITRightException("User does not have the right role");
            }

            // check the password
            u.setKeys(commonConfig.getEncryptionKey(), commonConfig.getApplicationKey());
            String _password = usersConfig.getUsersPasswordHeader() + body.getPassword() + usersConfig.getUsersPasswordFooter();
            if ( !u.isRightPassword(_password) ) {
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
            response.setPasswordExpired( u.getExpiredPassword() > Now.NowUtcMs() );
            response.setConditionToValidate(( p == null || p.getStringValue().isEmpty() || u.getConditionValidationVer().compareTo(p.getStringValue()) == 0 ));
            response.setTwoFARequired( u.getTwoFAType() != TwoFATypes.NONE );

            // Generate the Role list based on the user roles & status
            ArrayList<String> roles = new ArrayList<>();
            roles.add(UsersRolesCache.StandardRoles.ROLE_LOGIN_1FA.getRoleName());
            if ( u.getTwoFAType() != TwoFATypes.NONE ) {
                // 2FA Mechanism is active, generate the token with a limited role 1FA
                roles.add(UsersRolesCache.StandardRoles.ROLE_REGISTERED_USER.getRoleName());
            } else {
                // 2FA Mechanism is not active, generate the token with the full role list
                // check the password expiration to route the user to the password change page - update it
                if ( u.getExpiredPassword() > Now.NowUtcMs() || ( p == null || p.getStringValue().isEmpty() || u.getConditionValidationVer().compareTo(p.getStringValue()) == 0 ) ) {
                    roles.addAll(u.getRoles());
                    roles.add(UsersRolesCache.StandardRoles.ROLE_LOGIN_COMPLETE.getRoleName());
                } else {
                    // Reduce the role to the 1FA ROLE and ROLE_REGISTERED_USER to allow password change or condition validation but not more
                    roles.add(UsersRolesCache.StandardRoles.ROLE_REGISTERED_USER.getRoleName());
                }
            }
            response.setJwtToken(this.generateJWTForUser(u, roles));
            this.incLoginSuccess();
            return response;

        } catch (ITNotFoundException x ) {
            this.incLoginFailed();
            throw new ITRightException("User not found");
        }
    }

    //@TODO : need a solution to increase the log level once the password change is made and the eula are ok

    //@TODO : need to upgrade the session once the 2FA is completed

    //@TODO : need a password change solution with  a valid session

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
     * @return
     */
    protected String generateJWTForUser(User u, ArrayList<String> roles) {
        long exp;
        if ( u.isApiAccount() ) {
            if ( usersConfig.getUsersSessionApiTimeoutSec() > 0 ) {
                exp = Now.NowUtcMs() + usersConfig.getUsersSessionApiTimeoutSec() * 1000;
            } else {
                exp = Now.NowUtcMs() + 30*365*Now.ONE_FULL_DAY; // inifinite session is 30 Years
            }
        } else {
            if ( usersConfig.getUsersSessionTimeoutSec() > 0 ) {
                exp = Now.NowUtcMs() + usersConfig.getUsersSessionTimeoutSec() * 1000;
            } else {
                exp = Now.NowUtcMs() + 30*365*Now.ONE_FULL_DAY; // inifinite session is 30 Years
            }
        }
        Claims claims = Jwts.claims()
                .subject(u.getLogin())
                .expiration(new Date(exp))
                .add("roles", roles)
                .build();

        String token = Jwts.builder()
                .header().add("typ", "JWT")
                .add("sub", u.getLogin())
                .and()
                .claims().empty().add(claims)
                .and()
                .expiration(new Date(exp))
                .signWith(this.generateKeyForUser(u))
                .compact();
        return token;
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


    // ==========================================================================
    // Metrics
    // ==========================================================================

    @Autowired
    protected MeterRegistry meterRegistry;

    @PostConstruct
    private void initUserRegistrationService() {
        log.info("[users][service] User service initialized");
        Gauge.builder("users.login.attempt", this.getLoginAttempts())
                .description("Number of login attempts")
                .register(meterRegistry);
        Gauge.builder("users.login.failed", this.getLoginFailed())
                .description("Number of login failures")
                .register(meterRegistry);
        Gauge.builder("users.login.success", this.getCreationsSuccess())
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



}
