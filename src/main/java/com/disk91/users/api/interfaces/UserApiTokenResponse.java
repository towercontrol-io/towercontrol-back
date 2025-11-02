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

import com.disk91.users.mdb.entities.sub.UserAcl;
import com.disk91.users.mdb.entities.sub.UserApiKeys;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.ArrayList;
import java.util.List;

@Tag(name = "Api Token", description = "User API Token details")
public class UserApiTokenResponse {

    @Schema(
            description = "Api Token ID (uniq string)",
            example = "apikey_A3E5C710",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String id;


    @Schema(
            description = "User defined token name, for user reference",
            example = "My Api Key",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String keyName;

    @Schema(
            description = "Key expiration date in epoch format (ms since 1970)",
            example = "1777777777777",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected long expiration;

    @Schema(
            description = "List of requested roles for this token",
            example = "ROLE_USER_ADMIN,ROLE_DEVICE_READ...",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected List<String> roles;

    @Schema(
            description = "List of acl on groups for this token",
            example = "[{\"group\":\"xxxxxx\",\"localName\":\"my group\",\"roles\":[\"ROLE_DEVICE_READ\",\"ROLE_DEVICE_WRITE\"]}]",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected List<UserAcl> acls;

    // ==========================
    // Constructor

    public static UserApiTokenResponse getInstance(UserApiKeys key) {
        UserApiTokenResponse r = new UserApiTokenResponse();
        r.setId(key.getId());
        r.setExpiration(key.getExpiration());
        r.setKeyName(key.getName());
        r.setAcls(key.getAcls());
        r.setRoles(key.getRoles());
        return r;
    }


    // ==========================
    // Getters & Setters


    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    public long getExpiration() {
        return expiration;
    }

    public void setExpiration(long expiration) {
        this.expiration = expiration;
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
