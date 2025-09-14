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
import com.disk91.common.pdb.entities.Param;
import com.disk91.common.pdb.repositories.ParamRepository;
import com.disk91.common.tools.Now;
import com.disk91.common.tools.exceptions.ITNotFoundException;
import com.disk91.common.tools.exceptions.ITParseException;
import com.disk91.common.tools.exceptions.ITRightException;
import com.disk91.common.tools.exceptions.ITTooManyException;
import com.disk91.users.api.interfaces.UserAccountCreationBody;
import com.disk91.users.api.interfaces.UserApiTokenCreationBody;
import com.disk91.users.config.ActionCatalog;
import com.disk91.users.config.UsersConfig;
import com.disk91.users.mdb.entities.Role;
import com.disk91.users.mdb.entities.User;
import com.disk91.users.mdb.entities.UserRegistration;
import com.disk91.users.mdb.entities.sub.TwoFATypes;
import com.disk91.users.mdb.entities.sub.UserAcl;
import com.disk91.users.mdb.entities.sub.UserApiKeys;
import com.disk91.users.mdb.repositories.UserRegistrationRepository;
import com.disk91.users.mdb.repositories.UserRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.function.Supplier;

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
    protected UserRepository userRepository;

    @Autowired
    protected ParamRepository paramRepository;

    @Autowired
    protected AuditIntegration auditIntegration;

    @Autowired
    protected UserCache userCache;

    @Autowired
    protected UserCommon userCommon;

    @Autowired
    protected UsersRolesCache usersRolesCache;

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
    protected void createApiToken(
            HttpServletRequest request,
            String requestorId,
            String userId,
            UserApiTokenCreationBody body
    ) throws ITParseException, ITRightException, ITNotFoundException {

        if ( userId == null || userId.isEmpty() ) {
            throw new ITParseException("user-profile-login-invalid");
        }

        try {
            User _requestor = userCache.getUser(requestorId);

            if (!userCommon.isLegitAccessRead(_requestor, userId, true)) {
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

            // We have the right user with the authorization to create the token
            // Check the requested ROLES
            ArrayList<String> allowedRoles = new ArrayList<>();
            for ( String r : body.getRoles() ) {
                if ( _user.isInRole(r) ) {
                    // some of the role are not assigned to API token or will be assigned after
                    try {
                        if (usersRolesCache.getRole(r).isAssignable()) {
                            // we can add the role
                            allowedRoles.add(r);
                        }
                    } catch ( ITNotFoundException x) {
                        // role does not exist. skip it
                    }
                } else throw new ITRightException("user-profile-unauthorize-role");
            }
            // add the roles related to the user creation process
            allowedRoles.add(UsersRolesCache.StandardRoles.ROLE_REGISTERED_USER.getRoleName());
            allowedRoles.add(UsersRolesCache.StandardRoles.ROLE_LOGIN_API.getRoleName());
            apiKey.setRoles(allowedRoles);


            // Now we can process the ACL
            ArrayList<UserAcl> allowedAcls = new ArrayList<>();
            if ( body.getAcls() != null && !body.getAcls().isEmpty() ) {
                for ( UserAcl _acl : body.getAcls() ) {

                    // check if the user is in group
                    // @TODO
                    // check if the user is in Role

                }
            }


        } catch (ITNotFoundException x) {
            log.error("[users] Requestor {} not found", requestorId);
            throw new ITRightException("user-profile-user-not-foundd");
        }

    }


}
