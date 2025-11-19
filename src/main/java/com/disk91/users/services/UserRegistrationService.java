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
import com.disk91.common.tools.EmailTools;
import com.disk91.common.tools.Now;
import com.disk91.common.tools.Tools;
import com.disk91.common.tools.exceptions.ITParseException;
import com.disk91.common.tools.exceptions.ITRightException;
import com.disk91.common.tools.exceptions.ITTooManyException;
import com.disk91.users.api.interfaces.UserAccountRegistrationBody;
import com.disk91.users.config.ActionCatalog;
import com.disk91.users.config.UserMessages;
import com.disk91.users.config.UsersConfig;
import com.disk91.users.mdb.entities.User;
import com.disk91.users.mdb.entities.UserRegistration;
import com.disk91.users.mdb.repositories.UserRegistrationRepository;
import com.disk91.users.mdb.repositories.UserRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.function.Supplier;

@Service
public class UserRegistrationService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());


    /*
     * Account registration will be performed using a login that consists of an email address.
     * Accounts pending creation are stored in a separate table that is progressively cleaned up—either because the
     * accounts have been created or because they did not pass the next stage and have been rejected. An account with
     * an email that already exists will be rejected without any special notification.
     */


    @Autowired
    protected UsersConfig usersConfig;

    @Autowired
    protected CommonConfig commonConfig;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected UserRegistrationRepository userRegistrationRepository;

    @Autowired
    protected EmailTools emailTools;

    @Autowired
    protected UserMessages userMessages;

    @Autowired
    protected AuditIntegration auditIntegration;

    /**
     * Create a UserPending entry when the user request format is valid, email not filtered and do not already exist
     * The function will add delay to limit the ability to understand the underlying behavior and even if reports
     * the reason of refusal, the upper layer may silently refuse the request (responding OK en errors)
     * The function sent Email on success
     * @throws ITParseException
     * @throws ITTooManyException
     */
    public void requestAccountCreation(UserAccountRegistrationBody body, HttpServletRequest req)
        throws ITParseException, ITTooManyException, ITRightException {

        this.incRegistrationAttempt();
        // Is service open ?
        if (!usersConfig.isUsersRegistrationSelf()) {
            Now.randomSleep(50, 350);
            this.incRegistrationFailed();
            throw new ITParseException("Account self registration is not allowed");
        }

        if ( req == null ) {
            Now.randomSleep(50, 350);
            this.incRegistrationFailed();
            throw new ITParseException("Request is missing");
        }

        // Is having a valid invite code ?
        if (usersConfig.isUsersRegistrationWithInviteCode()) {
            // @TODO - Manage the Invite code verification until going further
        }

        // Check the requestor IP
        // @TODO - Check the IP is not blacklisted
        // @TODO - Check the IP is not already used for pending requests ( not sure it's a good idea - class room) - see what about request rate ?

        // Check the email format
        if (body.getEmail() == null || body.getEmail().isEmpty() || body.getEmail().length() > usersConfig.getUsersRegistrationEmailMaxLength()) {
            Now.randomSleep(50, 350);
            this.incRegistrationFailed();
            throw new ITParseException("Email is missing or too long");
        }
        body.setEmail(body.getEmail().toLowerCase());
        if (!Tools.isValidEmailSyntax(body.getEmail())) {
            Now.randomSleep(50, 350);
            this.incRegistrationFailed();
            throw new ITParseException("Email format is not valid");
        }
        if (!Tools.isAcceptedEmailSyntax(body.getEmail(), usersConfig.getUsersRegistrationEmailFilters())) {
            Now.randomSleep(50, 350);
            this.incRegistrationFailed();
            throw new ITParseException("Email pattern rejected");
        }

        // ----
        // At this point the email format is accepted

        // Make sure the user does not already exist
        User u = userRepository.findOneUserByLogin(User.encodeLogin(body.getEmail()));
        if (u != null) {
            Now.randomSleep(50, 300);
            this.incRegistrationFailed();
            throw new ITTooManyException("User already registered");
        }

        // Make sure we don't have another pending request for the same email
        UserRegistration ur = new UserRegistration();
        ur.init(
                body.getEmail(),
                body.getRegistrationCode(),
                (req.getHeader("x-real-ip") != null) ? req.getHeader("x-real-ip") : "Unknown",
                usersConfig.getUsersRegistrationLinkExpiration() * 1000,
                commonConfig.getEncryptionKey()
        );

        // search with encrypted email
        UserRegistration exists = userRegistrationRepository.findOneUserRegistrationByEmail(ur.getEmail());
        if (exists != null) {
            Now.randomSleep(50, 300);
            this.incRegistrationFailed();
            throw new ITTooManyException("User already registering");
        }

        // ----
        // At this point the registration is valid and we can make it pending
        // and send an email if needed

        userRegistrationRepository.save(ur);
        if (usersConfig.isUsersRegistrationLinkByEmail()) {
            Locale locale = emailTools.extractLocale(req, Locale.forLanguageTag(commonConfig.getCommonLangDefault()));
            // In case the registration path is empty, the validation code is directly printed in the email, in the other case,
            // the email contains the link to the frontend.
            if ( usersConfig.getUsersRegistrationPath().isEmpty() ) {
                Object[] args = {commonConfig.getCommonServiceName(), ur.getValidationId()};
                String _subject = userMessages.messageSource().getMessage("users.messages.registration.subject", args, locale);
                String _body = userMessages.messageSource().getMessage("users.messages.registration.code.body", args, locale);
                emailTools.send(body.getEmail(), _body, _subject, commonConfig.getCommonMailSender());
            } else {
                String _path = usersConfig.getUsersRegistrationPath().replace("!0!", ur.getValidationId());
                String _link = commonConfig.getCommonServiceUrl(_path, true);

                Object[] args = {commonConfig.getCommonServiceName(), _link};
                String _subject = userMessages.messageSource().getMessage("users.messages.registration.subject", args, locale);
                String _body = userMessages.messageSource().getMessage("users.messages.registration.body", args, locale);
                emailTools.send(body.getEmail(), _body, _subject, commonConfig.getCommonMailSender());
            }
        }
        Now.randomSleep(50, 250);

        // Update stats & add traces
        this.incRegistrationSuccess();
        auditIntegration.auditLog(
                ModuleCatalog.Modules.USERS,
                ActionCatalog.getActionName(ActionCatalog.Actions.REGISTRATION),
                User.encodeLogin(body.getEmail()),
                "{0} registration from IP {1}",
                new String[]{body.getEmail(), (req.getHeader("x-real-ip") != null) ? req.getHeader("x-real-ip") : "Unknown"}
        );

    }

    /*
     * The function will check the expiration date. When the Registration is expired, the function will delete the entry
     * in the database. Scanned on every 60 seconds
     */
    @Scheduled(fixedRate = 60000)
    void processExpiredRegistrations() {
        // remove all the expired registrations
        long now = Now.NowUtcMs();
        userRegistrationRepository.deleteByExpirationDateLowerThan(Now.NowUtcMs());
    }



    // ==========================================================================
    // Metrics
    // ==========================================================================

    @Autowired
    protected MeterRegistry meterRegistry;

    @PostConstruct
    private void initUserRegistrationService() {
        log.info("[users][registration] User registration service initialized");
        Gauge.builder("users_service_registration_attempt", this.getRegistrationsAttempts())
                .description("Number of registration attempts")
                .register(meterRegistry);
        Gauge.builder("users_service_registration_failed", this.getRegistrationsFailed())
                .description("Number of registration failures")
                .register(meterRegistry);
        Gauge.builder("users_service_registration_success", this.getRegistrationsSuccess())
                .description("Number of registration success (waiting for email confirmation)")
                .register(meterRegistry);
    }

    private long registrationsAttempts = 0;
    private long registrationsFailed = 0;
    private long registrationsSuccess = 0;

    protected synchronized void incRegistrationAttempt() {
        registrationsAttempts++;
    }

    protected synchronized void incRegistrationFailed() {
        registrationsFailed++;
    }

    protected synchronized void incRegistrationSuccess() {
        registrationsSuccess++;
    }

    protected Supplier<Number> getRegistrationsAttempts() {
        return ()->registrationsAttempts;
    }

    protected Supplier<Number>  getRegistrationsFailed() {
        return ()->registrationsFailed;
    }
    protected Supplier<Number>  getRegistrationsSuccess() {
        return ()->registrationsSuccess;
    }
}
