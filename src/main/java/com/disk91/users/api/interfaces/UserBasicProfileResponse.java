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

import com.disk91.common.tools.CustomField;
import com.disk91.common.tools.exceptions.ITNotFoundException;
import com.disk91.common.tools.exceptions.ITParseException;
import com.disk91.users.mdb.entities.User;
import com.disk91.users.mdb.entities.sub.UserAcl;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.ArrayList;
import java.util.List;

@Tag(name = "User Basic Profile", description = "User profile with minimum information for front display")
public class UserBasicProfileResponse {

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
            description = "First Name",
            example = "John",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String firstName;

    @Schema(
            description = "Last Name",
            example = "Doe",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String lastName;

    @Schema(
            description = "The password expiration date, in epoch ms",
            example = "172545052000",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected long passwordExpirationMs;


    @Schema(
            description = "User language",
            example = "en",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String language;

    @Schema(
            description = "Last communication seen, used for getting the pending one",
            example = "172545052000",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected long lastComMessageSeen;

    @Schema(
            description = "Roles list",
            example = "[ ROLE_DEVICE_READ, ROLE_REGISTERED_USER, ROLE_USER_ADMIN... ]",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected List<String> roles;


    @Schema(
            description = "Acls list",
            example = "[ { group : mygroup, roles : [ ROLE_DEVICE_READ ] }, ...  ]",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected List<UserAcl> acls;

    @Schema(
            description = "List of profile custom fields starting by basic_ as a key, decrypted",
            example = "[ { name : basic_xxx, value : xxxx }, ...  ]",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected List<CustomField> customFields;


    // ==========================
    // Build from User Object
    public void buildFromUser(User u) {
        try {
            this.email = u.getEncEmail();
            this.firstName = u.getEncProfileFirstName();
            this.lastName = u.getEncProfileLastName();
        } catch (ITParseException x) {
            this.email = "";
            this.firstName = "";
            this.lastName = "";
        }
        this.login = u.getLogin();
        this.language = u.getLanguage();
        this.acls = new ArrayList<>();
        for ( UserAcl a : u.getAcls() ) this.acls.add(a.clone());
        this.roles = new ArrayList<>(u.getRoles());
        this.lastComMessageSeen = u.getLastComMessageSeen();
        this.passwordExpirationMs = u.getExpiredPassword();
        this.customFields = new ArrayList<>();
        for ( CustomField cf : u.getCustomFields() ) {
            if ( cf.getName().startsWith("basic_") ) {
                try {
                    this.customFields.add(u.getEncCustomField(cf.getName()));
                } catch (ITParseException | ITNotFoundException x) {
                    // In this case just not return the custom field, this should not happen btw
                }
            }
        }
    }


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

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public long getPasswordExpirationMs() {
        return passwordExpirationMs;
    }

    public void setPasswordExpirationMs(long passwordExpirationMs) {
        this.passwordExpirationMs = passwordExpirationMs;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public long getLastComMessageSeen() {
        return lastComMessageSeen;
    }

    public void setLastComMessageSeen(long lastComMessageSeen) {
        this.lastComMessageSeen = lastComMessageSeen;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public List<UserAcl> getAcls() {
        return acls;
    }

    public void setAcls(List<UserAcl> acls) {
        this.acls = acls;
    }

    public List<CustomField> getCustomFields() {
        return customFields;
    }

    public void setCustomFields(List<CustomField> customFields) {
        this.customFields = customFields;
    }
}
