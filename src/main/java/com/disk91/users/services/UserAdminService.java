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
import com.disk91.common.tools.*;
import com.disk91.common.tools.exceptions.ITNotFoundException;
import com.disk91.users.api.interfaces.*;
import com.disk91.users.config.UserMessages;
import com.disk91.users.config.UsersConfig;
import com.disk91.users.mdb.entities.User;
import com.disk91.users.mdb.repositories.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserAdminService {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * In this service, we will find all the functions that allow the search & updates of users
     * specific to administrators, not accessible by the user himself
     */

    @Autowired
    protected UserCache userCache;

    @Autowired
    protected CommonConfig commonConfig;

    @Autowired
    protected UsersConfig usersConfig;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected AuditIntegration auditIntegration;

    @Autowired
    protected EmailTools emailTools;

    @Autowired
    protected UserMessages userMessages;

    @Autowired
    protected UserCommon userCommon;


    // ==========================================================================
    // Search functions
    // ==========================================================================

    public List<UserListElementResponse> searchUsersInPurgatory(
            String requester,
            HttpServletRequest req
    ) throws ITNotFoundException {

        // Security is checked by the API layer
        // @TODO - Later for User_Admin we can limit to the users belonging to the groups owned by admin groups...

        List<User> users = userRepository.findUserInPurgatory(Now.NowUtcMs());
        if ( users.isEmpty() ) throw new ITNotFoundException("user-none-in-purgatory");

        ArrayList<UserListElementResponse> response = new ArrayList<>();
        for(User u : users) {
            u.setKeys(commonConfig.getEncryptionKey(), commonConfig.getApplicationKey());
            UserListElementResponse r = new UserListElementResponse();
            r.buildFromUser(u);
            response.add(r);
            u.cleanKeys();
        }

        return response;
    }


}
