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

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "User module Configuration", description = "Response User Module Configuration")
public class UserConfigResponse {

    @Schema(
            description = "Self registration is allowed",
            example = "true",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected boolean selfRegistration;

    @Schema(
            description = "Invitation code required",
            example = "true",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected boolean invitationCodeRequired;

    @Schema(
            description = "Registration link will be sent by email",
            example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected boolean registrationLinkByEmail;

    @Schema(
            description = "User auto-validation is allowed / admin must not manually validate the user",
            example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected boolean autoValidation;

    @Schema(
            description = "EULA validation is required",
            example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected boolean eulaRequired;


    @Schema(
            description = "Password minimum size",
            example = "8",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected int passwordMinSize;

    @Schema(
            description = "Password minimum number of upper case characters",
            example = "0",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected int passwordMinUpperCase;

    @Schema(
            description = "Password minimum number of lower case characters",
            example = "0",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected int passwordMinLowerCase;

    @Schema(
            description = "Password minimum number of digit characters",
            example = "0",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected int passwordMinDigits;

    @Schema(
            description = "Password minimum number of symbols characters",
            example = "0",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected int passwordMinSymbols;

    @Schema(
            description = "Account deletion, purgatory delay in hours (0 means immediate deletion)",
            example = "24",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected long deletionPurgatoryDelayHours;


    // ==========================
    // Getters & Setters


    public boolean isSelfRegistration() {
        return selfRegistration;
    }

    public void setSelfRegistration(boolean selfRegistration) {
        this.selfRegistration = selfRegistration;
    }

    public boolean isInvitationCodeRequired() {
        return invitationCodeRequired;
    }

    public void setInvitationCodeRequired(boolean invitationCodeRequired) {
        this.invitationCodeRequired = invitationCodeRequired;
    }

    public boolean isRegistrationLinkByEmail() {
        return registrationLinkByEmail;
    }

    public void setRegistrationLinkByEmail(boolean registrationLinkByEmail) {
        this.registrationLinkByEmail = registrationLinkByEmail;
    }

    public boolean isAutoValidation() {
        return autoValidation;
    }

    public void setAutoValidation(boolean autoValidation) {
        this.autoValidation = autoValidation;
    }

    public boolean isEulaRequired() {
        return eulaRequired;
    }

    public void setEulaRequired(boolean eulaRequired) {
        this.eulaRequired = eulaRequired;
    }

    public int getPasswordMinSize() {
        return passwordMinSize;
    }

    public void setPasswordMinSize(int passwordMinSize) {
        this.passwordMinSize = passwordMinSize;
    }

    public int getPasswordMinUpperCase() {
        return passwordMinUpperCase;
    }

    public void setPasswordMinUpperCase(int passwordMinUpperCase) {
        this.passwordMinUpperCase = passwordMinUpperCase;
    }

    public int getPasswordMinLowerCase() {
        return passwordMinLowerCase;
    }

    public void setPasswordMinLowerCase(int passwordMinLowerCase) {
        this.passwordMinLowerCase = passwordMinLowerCase;
    }

    public int getPasswordMinDigits() {
        return passwordMinDigits;
    }

    public void setPasswordMinDigits(int passwordMinDigits) {
        this.passwordMinDigits = passwordMinDigits;
    }

    public int getPasswordMinSymbols() {
        return passwordMinSymbols;
    }

    public void setPasswordMinSymbols(int passwordMinSymbols) {
        this.passwordMinSymbols = passwordMinSymbols;
    }

    public long getDeletionPurgatoryDelayHours() {
        return deletionPurgatoryDelayHours;
    }

    public void setDeletionPurgatoryDelayHours(long deletionPurgatoryDelayHours) {
        this.deletionPurgatoryDelayHours = deletionPurgatoryDelayHours;
    }
}
