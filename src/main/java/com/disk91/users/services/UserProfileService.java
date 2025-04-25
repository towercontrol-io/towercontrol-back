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

import com.disk91.common.config.CommonConfig;
import com.disk91.common.tools.exceptions.ITNotFoundException;
import com.disk91.common.tools.exceptions.ITRightException;
import com.disk91.users.api.interfaces.UserBasicProfileResponse;
import com.disk91.users.mdb.entities.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserProfileService {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * In this service, we will find all the functions that allow the consultation, the search of users
     * The update of profile data, the deletion of users.
     */

    @Autowired
    protected UserCache userCache;

    @Autowired
    protected CommonConfig commonConfig;


    /**
     * Return user basic profile for a given user. Only Admin can acces user information or you can access for yourself
     * @param requestor
     * @return
     */
    public UserBasicProfileResponse getMyUserBasicProfile(String requestor, String user)
    throws ITRightException {

        try {
            User _requestor = userCache.getUser(requestor);

            boolean accessRight = false;
            if (    _requestor.isInRole(UsersRolesCache.StandardRoles.ROLE_GOD_ADMIN.getRoleName())
                ||  _requestor.isInRole(UsersRolesCache.StandardRoles.ROLE_USER_ADMIN.getRoleName())
                ||  requestor.compareTo(user) == 0
            ) {
                // the requestion is the user searched himself or it have admin / user admin global right
                // so we can hase the information
                accessRight = true;
            } else {
                // The user is not a global user admin. We may verify if the user can be a local user admin
                // for a group where this user is...
                // @TODO - manage the group ACL
            }
            if ( !accessRight ) {
                log.warn("[users] Requestor {} does not have access right to user {} profile", requestor, user);
                throw new ITRightException("user-profile-no-access");
            }

            User _user = _requestor;
            if ( requestor.compareTo(user) != 0 ) {
                try {
                    _user = userCache.getUser(user);
                } catch (ITNotFoundException x){
                    log.warn("[users] Searched user does not exists", x);
                    throw new ITRightException("user-profile-user-not-found");
                }
            }

            _user.setKeys(commonConfig.getEncryptionKey(), commonConfig.getApplicationKey());
            UserBasicProfileResponse r = new UserBasicProfileResponse();
            r.buildFromUser(_user);
            _user.cleanKeys();
            return r;

        } catch (ITNotFoundException x) {
            log.error("[users] Requestor does not exists", x);
            throw new ITRightException("user-profile-user-not-found");
        }
    }



}
