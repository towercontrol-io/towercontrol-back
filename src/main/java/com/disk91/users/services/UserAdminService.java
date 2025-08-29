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
import com.disk91.common.api.interfaces.ActionResult;
import com.disk91.common.config.CommonConfig;
import com.disk91.common.tools.*;
import com.disk91.common.tools.exceptions.ITNotFoundException;
import com.disk91.common.tools.exceptions.ITParseException;
import com.disk91.common.tools.exceptions.ITRightException;
import com.disk91.users.api.interfaces.*;
import com.disk91.users.config.UserMessages;
import com.disk91.users.config.UsersConfig;
import com.disk91.users.mdb.entities.User;
import com.disk91.users.mdb.repositories.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

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

    /**
     * Get the list of users in purgatory
     * @param requester
     * @param req
     * @return
     * @throws ITNotFoundException
     */
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

    /**
     * Get the list of users based on email search
     * @param requester
     * @param req
     * @return
     * @throws ITNotFoundException
     */
    public List<UserListElementResponse> searchUsersByEmail(
            String requester,
            UserSearchBody body,
            HttpServletRequest req
    ) throws ITParseException, ITNotFoundException {
        if ( body.getSearch() == null || body.getSearch().length() < 3 )
            throw new ITNotFoundException("user-search-invalid-input");

        ArrayList<UserListElementResponse> response = new ArrayList<>();

        try {
            List<String> keys = User.encodeSearch(body.getSearch());
            List<User> users = userRepository.findByUserSearchAll(keys);
            if ( users != null && !users.isEmpty() ) {
                for ( User u : users ) {
                    u.setKeys(commonConfig.getEncryptionKey(), commonConfig.getApplicationKey());
                    UserListElementResponse r = new UserListElementResponse();
                    r.buildFromUser(u);
                    response.add(r);
                    u.cleanKeys();
                }
            }
        } catch (ITParseException e) {
            throw new ITParseException("user-search-invalid-input");
        }
        return response;
    }

    /**
     * Get the 10 last connected users, excluding the requester
     * @param requester
     * @param req
     * @return
     * @throws ITNotFoundException
     */
    public List<UserListElementResponse> searchLastConnectecUsers(
            String requester,
            HttpServletRequest req
    ){

        ArrayList<UserListElementResponse> response = new ArrayList<>();
        List<User> users = userRepository.findTop11ByOrderByLastLoginDesc();
        if ( users != null && !users.isEmpty() ) {
            for ( User u : users ) {
                if ( u.getLogin().compareTo(requester) != 0 ) {
                    u.setKeys(commonConfig.getEncryptionKey(), commonConfig.getApplicationKey());
                    UserListElementResponse r = new UserListElementResponse();
                    r.buildFromUser(u);
                    response.add(r);
                    u.cleanKeys();
                }
            }
        }
        return response;

    }


}
