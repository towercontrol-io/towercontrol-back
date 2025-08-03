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
}
