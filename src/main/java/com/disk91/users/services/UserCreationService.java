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

import com.disk91.common.config.CommonConfig;
import com.disk91.common.pdb.entities.Param;
import com.disk91.common.pdb.repositories.ParamRepository;
import com.disk91.common.tools.Now;
import com.disk91.common.tools.exceptions.ITNotFoundException;
import com.disk91.common.tools.exceptions.ITParseException;
import com.disk91.common.tools.exceptions.ITRightException;
import com.disk91.common.tools.exceptions.ITTooManyException;
import com.disk91.users.api.interfaces.UserAccountCreationBody;
import com.disk91.users.config.UsersConfig;
import com.disk91.users.mdb.entities.User;
import com.disk91.users.mdb.entities.UserRegistration;
import com.disk91.users.mdb.repositories.UserRegistrationRepository;
import com.disk91.users.mdb.repositories.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserCreationService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * User creation can be done either in self-service mode,
     * where the user receives a code allowing them to create their account, or by
     * an administrator who has the right to create user accounts. Depending on
     * the configuration, the user must either be validated by an admin or is
     * directly validated. When created by an administrator, the password
     * must be changed at the next login.
     * The registration ID is null when the request comes from an admin, this
     * must be verifier before calling createUser_unsecured.
     */

    @Autowired
    protected CommonConfig commonConfig;

    @Autowired
    protected UsersConfig usersConfig;

    @Autowired
    protected UserRegistrationRepository userRegistrationRepository;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected ParamRepository paramRepository;

    /**
     * Once the validity of the request has been verified, the user is created
     * When a validation ID has been provided, email field is ignored if present
     * and the referenced email is used instead. When the validation ID is missing
     * the email field is mandatory.
     * @param body
     * @param req
     * @param forceResetPass - force to reset password on next login
     * @throws ITRightException
     * @throws ITParseException
     * @throws ITTooManyException
     */
    protected synchronized User createUser_unsecured(
        UserAccountCreationBody body,
        HttpServletRequest req,
        boolean forceResetPass
    ) throws ITRightException, ITParseException, ITTooManyException {

        if ( body.getValidationID() != null && ! body.getValidationID().isEmpty() ) {
            // Request from Self Service
            UserRegistration ur = userRegistrationRepository.findOneUserRegistrationByRegistrationCode(body.getValidationID());
            if ( ur == null || ur.getExpirationDate() < Now.NowUtcMs() ) {
                log.warn("[users] User creation request with invalid registration code");
                throw new ITRightException("Invalid registration code");
            }

            // get email for registration
            try {
                body.setEmail(ur.getEncEmail(commonConfig.getEncryptionKey()));
                ur.setExpirationDate(Now.NowUtcMs()-1); // expire
                userRegistrationRepository.save(ur);
            } catch (ITParseException | ITNotFoundException e) {
                log.error("[users] Error while decrypting email for user creation", e);
                throw new ITRightException("Error while decrypting email for user creation");
            }

        }

        // The Request has a valid email
        // Check if not already existing
        User u = userRepository.findOneUserByLogin(User.encodeLogin(body.getEmail()));
        if (u != null) {
            log.warn("[users] User creation, already registered ");
            throw new ITTooManyException("User already registered");
        }

        long now = Now.NowUtcMs();

        // Set user with minimal profile
        u = new User();
        u.setKeys(commonConfig.getEncryptionKey(), commonConfig.getApplicationKey());
        u.setEncLogin(body.getEmail());
        u.changePassword(body.getPassword(),true);
        u.setCountLogin(0);
        u.setRegistrationDate(now);
        u.setModificationDate(now);
        u.setLocked(false);
        u.setApiAccount(false);
        u.setApiAccountOwner("");
        u.setConditionValidation(body.isConditionValidation());
        if (body.isConditionValidation()) {
            // manage the current version based on parameters
            Param p = paramRepository.findByParamKey("users.condition.version");
            if ( p != null ) {
                u.setConditionValidationVer(p.getStringValue());
                u.setConditionValidationDate(now);
            } else {
                u.setConditionValidationVer("");
                u.setConditionValidationDate(0);
            }
        }

        // Set fields
        u.setEncRegistrationIP( ( req != null && req.getHeader("x-real-ip") != null )?req.getHeader("x-real-ip"):"Unknown" );
        u.setEncEmail(body.getEmail());
        // Init fields
        u.setEncProfileGender("");
        u.setEncProfileFirstName("");
        u.setEncProfileLastName("");
        u.setEncProfilePhone("");
        u.setEncProfileAddress("");
        u.setEncProfileCity("");
        u.setEncProfileZipCode("");
        u.setEncProfileCountry("");
        u.setEncBillingGender("");
        u.setEncBillingFirstName("");
        u.setEncBillingLastName("");
        u.setEncBillingPhone("");
        u.setEncBillingAddress("");
        u.setEncBillingCity("");
        u.setEncBillingZipCode("");
        u.setEncBillingCountry("");
        u.setEncBillingCompanyName("");
        u.setEncBillingVatNumber("");
        u.setEncBillingCountryCode("");
        u.setLastLogin(0);
        u.setPasswordResetId("");
        u.setPasswordResetExp(0);
        u.setLanguage("");
        u.setLastComMessageSeen(0);
        u.getAlertPreference().setEmailAlert(false);
        u.getAlertPreference().setPushAlert(false);
        u.getAlertPreference().setSmsAlert(false);

        u.getRoles().add("ROLE_PENDING_USER");

        // will depend on config / condition validation
        if ( usersConfig.isUsersPendingAutoValidation() ) {
            u.getRoles().add("ROLE_REGISTERED_USER");
            u.setActive(true);
        }

        if ( forceResetPass ) {
            u.setExpiredPassword(now);
        } else if ( usersConfig.getUsersPasswordExpirationDays() > 0 ) {
            u.setExpiredPassword(Now.NowUtcMs() + (usersConfig.getUsersPasswordExpirationDays() * Now.ONE_FULL_DAY));
        } else {
            u.setExpiredPassword(0);
        }

        // User is ready
        u.cleanKeys();
        u = userRepository.save(u);
        return u;
    }





}
