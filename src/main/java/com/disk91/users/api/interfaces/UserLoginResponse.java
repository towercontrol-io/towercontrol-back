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

import com.disk91.users.mdb.entities.sub.TwoFATypes;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "User Login", description = "Response User Login")
public class UserLoginResponse {

    @Schema(
            description = "User Email",
            example = "john.doe@foo.bar",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String email;

    @Schema(
            description = "User login (hash)",
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String login;

    @Schema(
            description = "JWT Token to be used for authentication, you will need to add Bearer to the token",
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpYXQiOjE0ODA5MjkyODIsImV4cCI6MTQ4MDkzMjg2OCwibmFtZSI6IlVzZXJuYW1lIn0.gZeuWNbjO8kyEX92AjgX5oLy5qhu6YWTPr6vtYELZQ4",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String jwtToken;

    @Schema(
            description = "JWT with limited access for Token renewal, this one have a longer expiration time",
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpYXQiOjE0ODA5MjkyODIsImV4cCI6MTQ4MDkzMjg2OCwibmFtZSI6IlVzZXJuYW1lIn0.gZeuWNbjO8kyEX92AjgX5oLy5qhu6YWTPr6vtYELZQ4",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String jwtRenewalToken;

    @Schema(
            description = "The password is expired, change required, roles will be restricted until changed",
            example = "false",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected boolean passwordExpired;

    @Schema(
            description = "The user condition has been changed, user need to accept the new condition to gain access",
            example = "false",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected boolean conditionToValidate;

    @Schema(
            description = "First authentication factor is ok, need to process second FA to get full access",
            example = "false",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected boolean twoFARequired;

    @Schema(
            description = "First and Second authentication factor is ok",
            example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected boolean twoFAValidated;


    @Schema(
            description = "The 2FA expected code size, this helps the front-end to display the right input field",
            example = "6",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected int twoFASize;

    @Schema(
            description = "The 2FA type, this helps the front-end to display the right information",
            example = "AUTHENTICATOR",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected TwoFATypes twoFAType;


    // ==========================
    // Getters & Setters


    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getJwtToken() {
        return jwtToken;
    }

    public void setJwtToken(String jwtToken) {
        this.jwtToken = jwtToken;
    }

    public boolean isPasswordExpired() {
        return passwordExpired;
    }

    public void setPasswordExpired(boolean passwordExpired) {
        this.passwordExpired = passwordExpired;
    }

    public boolean isConditionToValidate() {
        return conditionToValidate;
    }

    public void setConditionToValidate(boolean conditionToValidate) {
        this.conditionToValidate = conditionToValidate;
    }

    public boolean isTwoFARequired() {
        return twoFARequired;
    }

    public void setTwoFARequired(boolean twoFARequired) {
        this.twoFARequired = twoFARequired;
    }

    public String getJwtRenewalToken() {
        return jwtRenewalToken;
    }

    public void setJwtRenewalToken(String jwtRenewalToken) {
        this.jwtRenewalToken = jwtRenewalToken;
    }

    public int getTwoFASize() {
        return twoFASize;
    }

    public void setTwoFASize(int twoFASize) {
        this.twoFASize = twoFASize;
    }

    public TwoFATypes getTwoFAType() {
        return twoFAType;
    }

    public void setTwoFAType(TwoFATypes twoFAType) {
        this.twoFAType = twoFAType;
    }

    public boolean isTwoFAValidated() {
        return twoFAValidated;
    }

    public void setTwoFAValidated(boolean twoFAValidated) {
        this.twoFAValidated = twoFAValidated;
    }
}
