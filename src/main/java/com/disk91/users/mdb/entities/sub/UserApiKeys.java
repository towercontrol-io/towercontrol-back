package com.disk91.users.mdb.entities.sub;

import com.disk91.common.tools.CloneableObject;
import com.disk91.common.tools.HexCodingTools;
import com.disk91.common.tools.Now;

import java.util.ArrayList;
import java.util.List;

public class UserApiKeys implements CloneableObject<UserApiKeys> {

    // API key id, used to identify the right key, 6 hex char, random, unique for a user
    private String id;

    // API key name, given by user, used to identify the key
    private String name;

    // API key secret, used to sign JWTs
    private String secret;

    // API key expiration date in MS since epoch, 0 means the key has been disabled
    // User choice to diable it or automatic removal due to user right change
    private long expiration;

    // Store the roles associated to this key to easily identify the key to remove when the user
    // right change.
    private List<String> roles;

    // List of ACL associated to this key with some specific rights on these groups.
    private List<UserAcl> acls;

    // === FUNCTIONALITY ===

    /**
     * Init the structure
     */
    public void init() {
        this.roles = new ArrayList<String>();
        this.acls = new ArrayList<UserAcl>();
        this.id = null;
    }

    /**
     * This will delete the ability to reuse the key and kill the current JWTs using his key
     */
    public void disable() {
        this.expiration = 0; // Set expiration at 0 means the key has been expired
        this.secret = HexCodingTools.getRandomHexString(64); // new random secret
    }

    // === GETTER / SETTER ===

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpiration() {
        return expiration;
    }

    public void setExpiration(long expiration) {
        this.expiration = expiration;
    }

    public List<UserAcl> getAcls() {
        return acls;
    }

    public void setAcls(List<UserAcl> acls) {
        this.acls = acls;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    // === CLONE ===

    public UserApiKeys clone() {
        UserApiKeys u = new UserApiKeys();
        u.setId(this.id);
        u.setName(this.name);
        u.setSecret(this.secret);
        u.setExpiration(this.expiration);
        if (this.acls != null) {
            ArrayList<UserAcl> cf = new ArrayList<>();
            for (UserAcl c : this.acls) {
                cf.add(c.clone());
            }
            u.setAcls(cf);
        }
        if (this.roles != null) {
            ArrayList<String> rf = new ArrayList<>(this.roles);
            u.setRoles(rf);
        }
        return u;
    }

}
