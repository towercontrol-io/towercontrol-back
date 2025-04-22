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
package com.disk91.users.services;

import com.disk91.common.tools.Now;
import com.disk91.common.tools.exceptions.ITNotFoundException;
import com.disk91.common.tools.exceptions.ITParseException;
import com.disk91.common.tools.exceptions.ITTooManyException;
import com.disk91.users.mdb.entities.Role;
import com.disk91.users.mdb.repositories.RolesRepository;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UsersRolesCache {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    // No need for a complex cache, we just need to load the roles at startup and maintaining it
    // up to date. As the service can be deployed in a cluster, we need to be able to reload the
    // cache on demand. The collection is small enough to have a full reload when required

    // global cache of roles
    protected ConcurrentHashMap<String,Role> rolesCache = new ConcurrentHashMap<>();

    @Autowired
    protected RolesRepository rolesRepository;

    // Platform roles to setup for database init
    private String [] pfRoles = {
        "{'version':1, 'platform':true, 'name':'ROLE_GOD_ADMIN','description':'role-god-admin-desc', 'enDescription':'super administrator', 'creationBy':'system', 'creationMs':0}",
        "{'version':1, 'platform':true, 'name':'ROLE_USER_ADMIN','description':'role-user-admin-desc', 'enDescription':'user administrator', 'creationBy':'system', 'creationMs':0}",
        "{'version':1, 'platform':true, 'name':'ROLE_GROUP_ADMIN','description':'role-group-admin-desc', 'enDescription':'group administrator', 'creationBy':'system', 'creationMs':0}",
        "{'version':1, 'platform':true, 'name':'ROLE_GROUP_LADMIN','description':'role-group-ladmin-desc', 'enDescription':'local group administrator', 'creationBy':'system', 'creationMs':0}",
        "{'version':1, 'platform':true, 'name':'ROLE_DEVICE_ADMIN','description':'role-device-admin-desc', 'enDescription':'device administrator', 'creationBy':'system', 'creationMs':0}",
        "{'version':1, 'platform':true, 'name':'ROLE_DEVICE_READ','description':'role-device-read-desc', 'enDescription':'device reader', 'creationBy':'system', 'creationMs':0}",
        "{'version':1, 'platform':true, 'name':'ROLE_DEVICE_WRITE','description':'role-device-write-desc', 'enDescription':'device writer', 'creationBy':'system','creationMs':0}",
        "{'version':1, 'platform':true, 'name':'ROLE_DEVICE_CONFIG','description':'role-device-config-desc', 'enDescription':'device configurator', 'creationBy':'system','creationMs':0}",
        "{'version':1, 'platform':true, 'name':'ROLE_DEVICE_ALERTING','description':'role-device-alerting-desc', 'enDescription':'device alert receiver', 'creationBy':'system','creationMs':0}",
        "{'version':1, 'platform':true, 'name':'ROLE_BACKEND_CAPTURE','description':'role-backend-capture-desc', 'enDescription':'backend capture authorized member', 'creationBy':'system','creationMs':0}",
        "{'version':1, 'platform':true, 'name':'ROLE_PENDING_USER','description':'role-pending-user-desc', 'enDescription':'not yet registered user', 'creationBy':'system', 'creationMs':0}",
        "{'version':1, 'platform':true, 'name':'ROLE_REGISTERED_USER','description':'role-registered-user-desc', 'enDescription':'registered user', 'creationBy':'system','creationMs':0}",
        "{'version':1, 'platform':true, 'name':'ROLE_LOGIN_1FA','description':'role-login-1fa-desc', 'enDescription':'logged user with 1 factor authentication', 'creationBy':'system','creationMs':0}",
        "{'version':1, 'platform':true, 'name':'ROLE_LOGIN_2FA','description':'role-login-2fa-desc', 'enDescription':'logged user with 2 factor authentication', 'creationBy':'system','creationMs':0}",
        "{'version':1, 'platform':true, 'name':'ROLE_LOGIN_COMPLETE','description':'role-login-complete-desc', 'enDescription':'login completed', 'creationBy':'system','creationMs':0}"
    };

    // Standard roles strings
    public enum StandardRoles {
        ROLE_GOD_ADMIN("ROLE_GOD_ADMIN"),
        ROLE_USER_ADMIN("ROLE_USER_ADMIN"),
        ROLE_GROUP_ADMIN("ROLE_GROUP_ADMIN"),
        ROLE_GROUP_LADMIN("ROLE_GROUP_LADMIN"),
        ROLE_DEVICE_ADMIN("ROLE_DEVICE_ADMIN"),
        ROLE_DEVICE_READ("ROLE_DEVICE_READ"),
        ROLE_DEVICE_WRITE("ROLE_DEVICE_WRITE"),
        ROLE_DEVICE_CONFIG("ROLE_DEVICE_CONFIG"),
        ROLE_DEVICE_ALERTING("ROLE_DEVICE_ALERTING"),
        ROLE_BACKEND_CAPTURE("ROLE_BACKEND_CAPTURE"),
        ROLE_PENDING_USER("ROLE_PENDING_USER"),
        ROLE_REGISTERED_USER("ROLE_REGISTERED_USER"),
        ROLE_LOGIN_1FA("ROLE_LOGIN_1FA"),
        ROLE_LOGIN_2FA("ROLE_LOGIN_2FA"),
        ROLE_LOGIN_COMPLETE("ROLE_LOGIN_COMPLETE");

        private String roleName;
        StandardRoles(String roleName) {
            this.roleName = roleName;
        }
        public String getRoleName() {
            return roleName;
        }
    }

    /**
     * Return the number of pfRoles expected in the cache from the initialization list above
     * This methods is mostly for tests
     */
    public int __countPfRoles() { return pfRoles.length; }

    /**
     * Returns the number of Roles in cache, also mostly used by tests.
     */
    public int __countRolesInCache() { return this.rolesCache.size(); }

    @PostConstruct
    public void initRolesCache() {
        log.debug("[users] Roles init");
        this.loadRoles();
    }

    /**
     * Search a Role in the Cache and return a clone of it to make sure it will not
     * be modified by the caller
     */
    public Role getRole(
        String name
    ) throws ITNotFoundException {
        Role r = rolesCache.get(name);
        if ( r == null ) {
            log.warn("[users] Role {} searched but not found", name);
            throw new ITNotFoundException("Role "+name+" not found");
        }
        return r.clone();
    }

    /**
     * Add one Role, internal API. When the role is created, it is added to the database and
     * added to the cache. We need to update the cache of the different instances and for this
     * we need to call the message bus entity.
     * It's not possible to add an already existing role, in this cas it returns ITTooManyException
     */
    public void addRole(
        String name,
        String description,
        String enDescription,
        String userName
    ) throws ITTooManyException, ITParseException {
        if ( rolesCache.containsKey(name) ) {
            log.error("[users] Role creation impossible: {} already exists", name);
            throw new ITTooManyException("Role "+name+" already exists");
        }
        if ( ! description.matches("^[a-z\\-]*$") ) {
            log.error("[users] Role description must be lowercase a-z and - only, was {}",description);
            throw new ITParseException("Role description is not valid");
        }
        Role role = new Role();
        role.setName(name);
        role.setDescription(description);
        role.setEnDescription(enDescription);
        role.setPlatform(false);
        role.setVersion(1);
        role.setCreationBy(userName);
        role.setCreationMs(Now.NowUtcMs());
        rolesRepository.save(role);
        rolesCache.put(role.getName(), role);
        // @TODO - send message to other instances to reload roles.
    }



    /**
     * Load or reload the roles from the database
     * This method is synchronized to avoid multiple reload at the same time
     */
    synchronized private void loadRoles() {
        List<Role> all = rolesRepository.findAll();
        rolesCache.clear();
        // load the platform role from String structure
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        for ( String r : pfRoles ) {
            try {
                Role role = mapper.readValue(r.replace("'", "\""), Role.class);
                role.setCreationMs(Now.NowUtcMs());
                rolesCache.putIfAbsent(role.getName(), role);
            } catch (JsonProcessingException e) {
                log.error("[users] Error loading platform role {} due to {}", r, e.getMessage());
            }
        }
        // load the custom roles if any
        all.forEach((r) -> rolesCache.putIfAbsent(r.getName(), r));
        log.info("[users] {} Roles (re)loaded", rolesCache.size());
    }




}
