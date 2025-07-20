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

@Tag(name = "User Account Creation", description = "Request User Account Creation")
public class UserAccountCreationBody {

    @Schema(
            description = "Email for the account creation",
            example = "john.doe@foo.bar",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String email;     // Email is not required when the user uses a registration link, keep it empty or null


    @Schema(
            description = "User Password, used for the account creation",
            example = "changeme",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String password;

    @Schema(
            description = "User checked condition Validation, not mandatory on registration but will be requested later",
            example = "true",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected boolean conditionValidation;

    @Schema(
            description = "Validation ID received by user on registration link",
            example = "true",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String validationID;



    // ==========================
    // Getters & Setters


    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isConditionValidation() {
        return conditionValidation;
    }

    public void setConditionValidation(boolean conditionValidation) {
        this.conditionValidation = conditionValidation;
    }

    public String getValidationID() {
        return validationID;
    }

    public void setValidationID(String validationID) {
        this.validationID = validationID;
    }
}
