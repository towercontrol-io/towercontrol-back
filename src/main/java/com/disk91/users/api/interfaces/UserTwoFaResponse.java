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

@Tag(name = "User 2FA setup", description = "Returns the 2FA parameters for the user")
public class UserTwoFaResponse {

    @Schema(
            description = "Set the 2FA method to be used (0 - none, 1 - email, 2 - sms, 3 - authenticator app)",
            example = "1",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected int twoFaMethod;

    @Schema(
            description = "Set the 2FA method to be used ('NONE', 'EMAIL', 'SMS', 'AUTHENTICATOR')",
            example = "AUTHENTICATOR",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected TwoFATypes twoFaType;

    @Schema(
            description = "Two Factor secret, used to generate the 2FA code (authenticator app only)",
            example = "otpauth://totp/{servicename}:{username}?secret={secret}&issuer={servicename}",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String secret;


    // ==========================
    // Getters & Setters


    public int getTwoFaMethod() {
        return twoFaMethod;
    }

    public void setTwoFaMethod(int twoFaMethod) {
        this.twoFaMethod = twoFaMethod;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public TwoFATypes getTwoFaType() {
        return twoFaType;
    }

    public void setTwoFaType(TwoFATypes twoFaType) {
        this.twoFaType = twoFaType;
    }
}
