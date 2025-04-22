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
import com.disk91.common.tools.exceptions.ITParseException;
import com.disk91.common.tools.exceptions.ITRightException;
import com.disk91.users.api.interfaces.UserLoginBody;
import com.disk91.users.config.UsersConfig;
import com.disk91.users.mdb.entities.User;
import com.disk91.users.mdb.repositories.UserRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    protected UserRepository userRepository;

    @Autowired
    protected AuditIntegration auditIntegration;

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
    public String UserLogin(
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

        User u = userRepository.findOneUserByLogin(loginHash);



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
