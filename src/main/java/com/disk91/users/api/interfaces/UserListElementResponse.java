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
package com.disk91.users.api.interfaces;

import com.disk91.common.tools.Now;
import com.disk91.common.tools.exceptions.ITParseException;
import com.disk91.users.mdb.entities.User;
import com.disk91.users.mdb.entities.sub.TwoFATypes;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;


@Tag(name = "User description for a list of users", description = "One of the element of a list of users, used by different type of searches")
public class UserListElementResponse {

    @Schema(
            description = "User login (hash)",
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String login;

    @Schema(
            description = "User email",
            example = "john.doe@foo.bar",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String email;

    @Schema(
            description = "Last login date (timestamp in ms)",
            example = "1697056894000",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected long lastLogin;

    @Schema(
            description = "Number of login for that user",
            example = "10",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected long countLogin;

    @Schema(
            description = "Registration date (timestamp in ms)",
            example = "1697056894000",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected long registrationDate;

    @Schema(
            description = "Deletion date (timestamp in ms)",
            example = "1697056894000",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected long deletionDate;


    @Schema(
            description = "User is active",
            example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected boolean isActive;

    @Schema(
            description = "User is locked",
            example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected boolean isLocked;

    @Schema(
            description = "User password is expired",
            example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected boolean isPasswordExpired;

    @Schema(
            description = "User is an API account",
            example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected boolean isApiAccount;

    @Schema(
            description = "User 2 Factor Authentication mode (NONE, EMAIL, SMS, AUTHENTICATOR)",
            example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected TwoFATypes twoFa;


    // ==========================
    // Build from User Object
    // (encryption key must be injected)
    public void buildFromUser(User u) {
        try {
            this.email = u.getEncEmail();
        } catch (ITParseException x) {
            this.email = "encrypted";
        }
        this.login = u.getLogin();
        this.lastLogin = u.getLastLogin();
        this.countLogin = u.getCountLogin();
        this.registrationDate = u.getRegistrationDate();
        this.deletionDate = u.getDeletionDate();
        this.isActive = u.isActive();
        this.isLocked = u.isLocked();
        this.isPasswordExpired = (u.getExpiredPassword() > 0 && u.getExpiredPassword() < Now.NowUtcMs());
        this.isApiAccount = u.isApiAccount();
        this.twoFa = u.getTwoFAType();
    }


    // ==========================
    // Getters & Setters

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public long getDeletionDate() {
        return deletionDate;
    }

    public void setDeletionDate(long deletionDate) {
        this.deletionDate = deletionDate;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public boolean isLocked() {
        return isLocked;
    }

    public void setLocked(boolean locked) {
        isLocked = locked;
    }

    public boolean isPasswordExpired() {
        return isPasswordExpired;
    }

    public void setPasswordExpired(boolean passwordExpired) {
        isPasswordExpired = passwordExpired;
    }

    public boolean isApiAccount() {
        return isApiAccount;
    }

    public void setApiAccount(boolean apiAccount) {
        isApiAccount = apiAccount;
    }

    public TwoFATypes getTwoFa() {
        return twoFa;
    }

    public void setTwoFa(TwoFATypes twoFa) {
        this.twoFa = twoFa;
    }
}
