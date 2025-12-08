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

import com.disk91.audit.integration.AuditIntegration;
import com.disk91.common.config.CommonConfig;
import com.disk91.common.config.ModuleCatalog;
import com.disk91.common.tools.HexCodingTools;
import com.disk91.common.tools.Now;
import com.disk91.common.tools.Tools;
import com.disk91.common.tools.exceptions.ITNotFoundException;
import com.disk91.common.tools.exceptions.ITParseException;
import com.disk91.common.tools.exceptions.ITRightException;
import com.disk91.groups.services.GroupsServices;
import com.disk91.users.api.interfaces.UserApiTokenCreationBody;
import com.disk91.users.api.interfaces.UserApiTokenResponse;
import com.disk91.users.config.ActionCatalog;
import com.disk91.users.config.UsersConfig;
import com.disk91.users.mdb.entities.User;
import com.disk91.users.mdb.entities.sub.UserAcl;
import com.disk91.users.mdb.entities.sub.UserApiKeys;
import com.disk91.users.mdb.repositories.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserApiTokenService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * User API Token Service allows to have different JWT tokens the user can have for
     * external integration and automation. Each token can be limited in time and scope.
     * The user can create and delete its own tokens. The user select the token duration even
     * for really long period of time. The token rights are limited to user rights. The token rights
     * are not extendable when the user rights change. When a User loss a right, the token with such
     * right needs to be repudiated and recreated manually.
     */

    @Autowired
    protected CommonConfig commonConfig;

    @Autowired
    protected UsersConfig usersConfig;

    @Autowired
    protected AuditIntegration auditIntegration;

    @Autowired
    protected UserCache userCache;

    @Autowired
    protected UserCommon userCommon;

    @Autowired
    protected UsersRolesCache usersRolesCache;

    @Autowired
    protected UserService userService;

    @Autowired
    protected GroupsServices groupsServices;

    @Autowired
    protected UserRepository userRepository;

    /**
     * Create a new API token for the existing user. The user can't give a right he is not owning.
     * Not all the ROLES are available for API token, only a subset of roles is allowed.
     * The creation can be requested by the user itself or by an admin user on behalf of another user.
     * Most of the errors are not reported (like requesting a non-existing role) because it's up to
     * the front-end to only propose the existing roles. Nothing unsecured will be accepted btw.
     *
     * @param request
     * @param userId
     * @param body
     * @throws ITParseException
     * @throws ITRightException
     * @throws ITNotFoundException
     */
    public void createApiToken(
            HttpServletRequest request,
            String requestorId,
            String userId,
            UserApiTokenCreationBody body
    ) throws ITParseException, ITRightException {

        if ( userId == null || userId.isEmpty() ) {
            throw new ITParseException("user-profile-login-invalid");
        }

        try {
            User _requestor = userCache.getUser(requestorId);

            if (!userCommon.isLegitAccess(requestorId, userId, true)) {
                log.warn("[users] Requestor {} does not have write access right to user {} profile", requestorId, userId);
                throw new ITRightException("user-profile-no-access");
            }

            User _user = _requestor;
            if (requestorId.compareTo(userId) != 0) {
                try {
                    _user = userCache.getUser(userId);
                } catch (ITNotFoundException x) {
                    log.warn("[users] Searched user does not exists", x);
                    throw new ITRightException("user-profile-user-not-found");
                }
            }
            UserApiKeys apiKey = new UserApiKeys();
            apiKey.init();
            // Generate a unique random ID that will be used for identification
            while ( apiKey.getId() == null ){
                String _id = "apikey_"+HexCodingTools.getRandomHexString(8);
                if ( userRepository.findByApiKeyId(_id) == null ) {
                    apiKey.setId(_id);
                }
            }
            apiKey.setName(body.getKeyName());
            apiKey.setSecret(HexCodingTools.getRandomHexString(64));
            apiKey.setExpiration(body.getExpiration());

            // Process Global ROLES
            ArrayList<String> allowedRoles = new ArrayList<>();
            for ( String r : body.getRoles() ) {
                if ( _user.isInRole(r) ) {
                    // some of the role are not assigned to API token or will be assigned after
                    try {
                        if (usersRolesCache.getRole(r).isAssignable()) {
                            // we can add the role if it is in the list of apikey assignable roles
                            if (Tools.isStringInList(r, usersConfig.getUserApiKeyAuthorizedRoles())) {
                                // we can add the role
                                allowedRoles.add(r);
                            } else throw new ITRightException("user-profile-unauthorize-role");
                        }
                    } catch ( ITNotFoundException x) {
                        // role does not exist. skip it
                    }
                } else throw new ITRightException("user-profile-unauthorize-role");
            }
            // add the roles related to the user creation process
            allowedRoles.add(UsersRolesCache.StandardRoles.ROLE_REGISTERED_USER.getRoleName());
            apiKey.setRoles(allowedRoles);

            // We have the right user with the authorization to create the token
            // Now we can process the ACL
            ArrayList<UserAcl> allowedAcls = new ArrayList<>();
            if ( body.getAcls() != null && !body.getAcls().isEmpty() ) {
                for ( UserAcl _acl : body.getAcls() ) {
                   if ( groupsServices.isUserInGroup(_requestor, _acl.getGroup(),  true, true, false) ) {
                       // we are into it, get the information about the right to that group and verify it
                       String parentgroup = groupsServices.findGroupUserForGroup(_user,_acl.getGroup(), false,true, false);
                       if ( parentgroup != null ) {
                           UserAcl newAcl = new UserAcl();
                           newAcl.setGroup(_acl.getGroup());
                           newAcl.setLocalName(_acl.getLocalName());
                           newAcl.setRoles(new ArrayList<>());
                           // user own that group, as the roles match, we are good to go
                           // Check the requested ROLES as part of the user roles
                           for ( String r : _acl.getRoles() ) {
                               if ( ! _user.isInRole(r) ) {
                                   throw new ITRightException("user-profile-unauthorize-role");
                               }
                               newAcl.getRoles().add(r);
                           }
                           allowedAcls.add(newAcl);
                       } else {
                           // search in acls
                           parentgroup = groupsServices.findGroupUserForGroup(_user,_acl.getGroup(), true,false, false);
                           if ( parentgroup != null ) {
                               UserAcl newAcl = new UserAcl();
                               newAcl.setGroup(_acl.getGroup());
                               newAcl.setLocalName(_acl.getLocalName());
                               newAcl.setRoles(new ArrayList<>());
                               // we need to find the matching ACL
                               UserAcl requestorAcl = null;
                               for ( UserAcl a : _user.getAcls() ) {
                                   if ( a.getGroup().compareTo(parentgroup) == 0 ) {
                                       requestorAcl = a;
                                       break;
                                   }
                               }
                               if ( requestorAcl == null ) {
                                   log.error("[users] Requestor ACL {} not found {} for user {} but it was found previously...", requestorId, _acl.getGroup(), userId);
                                   throw new ITRightException("user-profile-unauthorize-role");
                               }
                               // now we need to verify that the requestorAcl contains all the requested roles
                               for ( String r : _acl.getRoles() ) {
                                   for ( String ar : requestorAcl.getRoles() ) {
                                       if ( ar.compareTo(r) == 0 ) {
                                           // role found, we can add it
                                           newAcl.getRoles().add(r);
                                           break;
                                       }
                                   } // else skip it
                               }
                               allowedAcls.add(newAcl);
                           } else {
                               // we should never reach this point as the requestor is in the group
                               log.error("[users] Requestor {} does not have access to group {} for user {} but it was found previously...", requestorId, _acl.getGroup(), userId);
                               throw new ITRightException("user-profile-unauthorize-role");
                           }
                       }
                   }  else throw new ITRightException("user-profile-unauthorize-role");
                }
            }
            apiKey.setAcls(allowedAcls);

            // Add audit trace
            auditIntegration.auditLog(
                    ModuleCatalog.Modules.USERS,
                    ActionCatalog.getActionName(ActionCatalog.Actions.APIKEY_CREATION),
                    _user.getLogin(),
                    "Apikey creation from {0} requested by {1} for user {2} with name {3} and expiration {4}",
                    new String[]{Tools.getRemoteIp(request),_requestor.getLogin(), _user.getLogin(),apiKey.getName(), Long.toString(apiKey.getExpiration())}
            );

            // Add the API key to the user list and store it
            if ( _user.getApiKeys() == null ) _user.setApiKeys(new ArrayList<>());
            _user.getApiKeys().add(apiKey);
            userCache.saveUser(_user);

        } catch (ITNotFoundException x) {
            log.error("[users] Requestor {} not found", requestorId);
            throw new ITRightException("user-profile-user-not-found");
        }
    }

    /**
     * Return the list of User API keys
     * @param request
     * @param requestorId
     * @param userId
     * @return
     * @throws ITParseException
     * @throws ITRightException
     */
    public List<UserApiTokenResponse> getUserApiTokens(
            HttpServletRequest request,
            String requestorId,
            String userId
    ) throws ITParseException, ITRightException {

        if (userId == null || userId.isEmpty()) {
            throw new ITParseException("user-profile-login-invalid");
        }

        try {
            User _requestor = userCache.getUser(requestorId);

            if (!userCommon.isLegitAccess(requestorId, userId, false)) {
                log.warn("[users] Requestor {} does not have read access right to user {} profile", requestorId, userId);
                throw new ITRightException("user-profile-no-access");
            }

            User _user = _requestor;
            if (requestorId.compareTo(userId) != 0) {
                try {
                    _user = userCache.getUser(userId);
                } catch (ITNotFoundException x) {
                    log.warn("[users] Searched user does not exists", x);
                    throw new ITRightException("user-profile-user-not-found");
                }
            }

            ArrayList<UserApiTokenResponse> ret = new ArrayList<>();
            if ( _user.getApiKeys() != null ) {
                for (UserApiKeys k : _user.getApiKeys()) {
                    UserApiTokenResponse _r = UserApiTokenResponse.getInstance(k);
                    ret.add(_r);
                }
            }
            return ret;

        } catch (ITNotFoundException x) {
            log.error("[users] Requestor {} not found", requestorId);
            throw new ITRightException("user-profile-user-not-found");
        }

    }


    /**
     * Delete one API key for the existing user.
     * @param request
     * @param requestorId
     * @param userId
     * @return
     * @throws ITParseException
     * @throws ITRightException
     */
    public void deleteUserApiTokens(
            HttpServletRequest request,
            String requestorId,
            String userId,
            String tokenId
    ) throws ITParseException, ITRightException {

        if (userId == null || userId.isEmpty()) {
            throw new ITParseException("user-profile-login-invalid");
        }

        try {
            User _requestor = userCache.getUser(requestorId);

            if (!userCommon.isLegitAccess(requestorId, userId, true)) {
                log.warn("[users] Requestor {} does not have read access right to user {} profile", requestorId, userId);
                throw new ITRightException("user-profile-no-access");
            }

            User _user = _requestor;
            if (requestorId.compareTo(userId) != 0) {
                try {
                    _user = userCache.getUser(userId);
                } catch (ITNotFoundException x) {
                    log.warn("[users] Searched user does not exists", x);
                    throw new ITRightException("user-profile-user-not-found");
                }
            }

            UserApiKeys toRemove = null;
            for (UserApiKeys k : _user.getApiKeys()) {
                if (k.getId().compareTo(tokenId) == 0) {
                    toRemove = k;
                    break;
                }
            }
            if ( toRemove != null ) {
                // clear the key
                _user.getApiKeys().remove(toRemove);
                userCache.saveUser(_user);

                // Add audit trace
                auditIntegration.auditLog(
                        ModuleCatalog.Modules.USERS,
                        ActionCatalog.getActionName(ActionCatalog.Actions.APIKEY_DELETION),
                        _user.getLogin(),
                        "Apikey deletion from {0} requested by {1} for user {2} with name {3}",
                        new String[]{Tools.getRemoteIp(request),_requestor.getLogin(), _user.getLogin(),toRemove.getName()}
                );

                // clear cache
                userCache.flushApiKey(tokenId);
            } else {
                throw new ITParseException("user-apikey-not-found");
            }

        } catch (ITNotFoundException x) {
            log.error("[users] Requestor {} not found", requestorId);
            throw new ITRightException("user-profile-user-not-found");
        }

    }


    /**
     * Return the JWT corresponding to the given API key
     * The same JWT is generated each time for the same key until expiration
     * @param request
     * @param requestorId
     * @param userId
     * @param tokenId
     * @return
     * @throws ITParseException
     * @throws ITRightException
     */
    public String getJWTForApiKey(
            HttpServletRequest request,
            String requestorId,
            String userId,
            String tokenId
    ) throws ITParseException, ITRightException {

        if (userId == null || userId.isEmpty()) {
            throw new ITParseException("user-profile-login-invalid");
        }

        try {
            User _requestor = userCache.getUser(requestorId);

            if (!userCommon.isLegitAccess(requestorId, userId, false)) {
                log.warn("[users] Requestor {} does not have read access right to user {} profile", requestorId, userId);
                throw new ITRightException("user-profile-no-access");
            }

            User _user = _requestor;
            if (requestorId.compareTo(userId) != 0) {
                try {
                    _user = userCache.getUser(userId);
                } catch (ITNotFoundException x) {
                    log.warn("[users] Searched user does not exists", x);
                    throw new ITRightException("user-profile-user-not-found");
                }
            }

            for (UserApiKeys k : _user.getApiKeys()) {
                if (k.getId().compareTo(tokenId) == 0) {
                    if ( k.getExpiration() < Now.NowUtcMs() ) {
                        throw new ITParseException("user-apikey-expired");
                    }
                    return userService.generateApiKeyJWTForUser(_user,k);
                }
            }
            throw new ITParseException("user-apikey-not-found");

        } catch (ITNotFoundException x) {
            log.error("[users] Requestor {} not found", requestorId);
            throw new ITRightException("user-profile-user-not-found");
        }
    }
}
