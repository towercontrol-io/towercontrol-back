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

package com.disk91.users.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value = {"file:configuration/users.properties"}, ignoreResourceNotFound = true)
public class UsersConfig {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    // ----------------------------------------------
    // Common setup
    // ----------------------------------------------

    // Service Id used for multi-instance environment, must be different for every instances
    @Value("${common.service.id:83DZqvwbXzmtllVq}")
    protected String commonServiceId;
    public String getCommonServiceId() {
        return (commonServiceId.isEmpty())?"83DZqvwbXzmtllVq":commonServiceId;
    }

    // Select the medium to be used for the intracom service communication
    @Value("${users.intracom.medium:db}")
    protected String usersIntracomMedium;
    public String getUsersIntracomMedium() {
        return usersIntracomMedium;
    }

    // ----------------------------------------------
    // Users registration
    // ----------------------------------------------

    @Value("${users.registration.self:false}")
    protected boolean usersRegistrationSelf;
    public boolean isUsersRegistrationSelf() {
        return usersRegistrationSelf;
    }

    @Value("${users.registration.email.maxlength:128}")
    protected int usersRegistrationEmailMaxLength;
    public int getUsersRegistrationEmailMaxLength() {
        return usersRegistrationEmailMaxLength;
    }

    @Value("${users.registration.with.invitecode:fales}")
    protected boolean usersRegistrationWithInviteCode;
    public boolean isUsersRegistrationWithInviteCode() {
        return usersRegistrationWithInviteCode;
    }

    @Value("${users.registration.email.filters:}")
    protected String usersRegistrationEmailFilters;
    public String getUsersRegistrationEmailFilters() {
        return usersRegistrationEmailFilters;
    }

    @Value("${users.registration.link.expiration:3600}")
    protected long usersRegistrationLinkExpiration;
    public long getUsersRegistrationLinkExpiration() {
        return usersRegistrationLinkExpiration;
    }

    @Value("${user.registration.link.byemail:true}")
    protected boolean userRegistrationLinkByEmail;
    public boolean isUserRegistrationLinkByEmail() {
        return userRegistrationLinkByEmail;
    }

    @Value("${user.registration.path:registration/!0!/confirm}")
    protected String userRegistrationPath;
    public String getUserRegistrationPath() {
        return userRegistrationPath;
    }

    // --------------------------------------------
    // User Creation & Password rules
    // --------------------------------------------

    @Value("${users.pending.autovalidation:true}")
    protected boolean usersPendingAutoValidation;
    public boolean isUsersPendingAutoValidation() {
        return usersPendingAutoValidation;
    }

    @Value("${users.creation.need.eula}:false")
    protected boolean usersCreationNeedEula;
    public boolean isUsersCreationNeedEula() {
        return usersCreationNeedEula;
    }

    @Value("${users.password.expiration.days:0}")
    protected int usersPasswordExpirationDays;
    public int getUsersPasswordExpirationDays() {
        return usersPasswordExpirationDays;
    }

    @Value("${users.password.min.size:8}")
    protected int usersPasswordMinSize;
    public int getUsersPasswordMinSize() {
        return usersPasswordMinSize;
    }

    @Value("${users.password.min.uppercase:0}")
    protected int usersPasswordMinUppercase;
    public int getUsersPasswordMinUppercase() {
        return usersPasswordMinUppercase;
    }

    @Value("${users.password.min.lowercase:0}")
    protected int usersPasswordMinLowercase;
    public int getUsersPasswordMinLowercase() {
        return usersPasswordMinLowercase;
    }

    @Value("${users.password.min.numbers:0}")
    protected int usersPasswordMinNumbers;
    public int getUsersPasswordMinNumbers() {
        return usersPasswordMinNumbers;
    }

    @Value("${users.password.min.symbols:0}")
    protected int usersPasswordMinSymbols;
    public int getUsersPasswordMinSymbols() {
        return usersPasswordMinSymbols;
    }



}
