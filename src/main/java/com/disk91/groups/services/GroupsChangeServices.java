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
package com.disk91.groups.services;

import com.disk91.common.tools.EncryptionHelper;
import com.disk91.common.tools.exceptions.ITNotFoundException;
import com.disk91.common.tools.exceptions.ITParseException;
import com.disk91.common.tools.exceptions.ITRightException;
import com.disk91.common.tools.exceptions.ITTooManyException;
import com.disk91.groups.api.interfaces.GroupCreationBody;
import com.disk91.groups.config.GroupsConfig;
import com.disk91.groups.mdb.entities.Group;
import com.disk91.groups.mdb.repositories.GroupRepository;
import com.disk91.groups.tools.GroupsHierarchySimplified;
import com.disk91.groups.tools.GroupsList;
import com.disk91.users.mdb.entities.User;
import com.disk91.users.mdb.entities.sub.UserAcl;
import com.disk91.users.services.UserCache;
import com.disk91.users.services.UsersRolesCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class GroupsChangeServices {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /*
     * This service provides functions to create, modify, move & delete groups
     * with the necessary operation to check the requestor rights
     */

    @Autowired
    protected GroupsConfig groupsConfig;

    @Autowired
    protected GroupsServices groupsServices;

    @Autowired
    protected GroupRepository groupRepository;

    @Autowired
    protected UserCache userCache;

    // =====================================================================================================
    // CREATE GROUPS
    // =====================================================================================================

    public void createSubGroup(
            String userId,
            GroupCreationBody body
    ) throws ITNotFoundException, ITRightException, ITParseException {

        // Make sure the group exists
        Group group = null;
        try {
            group = groupsServices.getGroupByShortId(body.getParenId());
        } catch (ITNotFoundException x) {
            throw new ITNotFoundException("groups-get-not-found");
        }

        // Make sure the user can create a subgroup
        User user = null;
        try {
            user = userCache.getUser(userId);
            boolean authorized = false;
            // is the user is ADMIN / LOCAL_ADMIN / GOD_ADMIN to manipulate its attached groups
            if (user.isInRole(UsersRolesCache.StandardRoles.ROLE_GOD_ADMIN)) {
                authorized = true;
            } else if (
                    user.isInRole(UsersRolesCache.StandardRoles.ROLE_GROUP_ADMIN)
                 || user.isInRole(UsersRolesCache.StandardRoles.ROLE_GROUP_LADMIN)
            ) {
                // make sure the user attachment to this group
                if ( user.isInGroup(group.getShortId(),false, groupsConfig.isGroupVituralAllowsSub()) ) {
                    authorized = true;
                }
                for ( String upper : group.getReferringGroups()) {
                    if ( user.isInGroup(upper,false, groupsConfig.isGroupVituralAllowsSub()) ) {
                        authorized = true;
                        break;
                    }
                }
            }
            if ( !authorized ) {
                // check the ACL
                try {
                    UserAcl acl = user.searchAclGroup(group.getShortId());
                    if (   acl.isInRole(UsersRolesCache.StandardRoles.ROLE_GROUP_ADMIN)
                        || acl.isInRole(UsersRolesCache.StandardRoles.ROLE_GROUP_LADMIN)
                    ) {
                        authorized = true;
                    }
                    // search hierarchy
                    for ( String upper : group.getReferringGroups()) {
                        acl = user.searchAclGroup(upper);
                        if (   acl.isInRole(UsersRolesCache.StandardRoles.ROLE_GROUP_ADMIN)
                            || acl.isInRole(UsersRolesCache.StandardRoles.ROLE_GROUP_LADMIN)
                        ) {
                            authorized = true;
                            break;
                        }
                    }
                } catch (ITNotFoundException x) {
                    // not authorized, nothing to change
                }
            }
            if (!authorized) throw new ITRightException("groups-group-creation-refused");

        } catch (ITNotFoundException x) {
            // user not found ... not a normal situation
            log.error("[groups] Sub group creation for a not existing user");
            throw new ITParseException("groups-invalid-user-request");
        }

        // Here the user and the group rights are ok to proceed.
        Group newGroup = new Group();
        try {
            String shortId = groupsServices.getNewShortId();
            newGroup.init(body.getName(), body.getDescription(), shortId, user.getLanguage());
            newGroup.addUnderGroup(group, groupsConfig.getGroupsMaxDepth());
            newGroup.setCreationBy(user.getLogin());
            groupsServices.flushGroup(group.getShortId());
            groupsServices.saveGroup(newGroup);
        } catch (ITTooManyException x) {
            throw new ITParseException(x.getMessage());
        }

        // No need to change the user configuration as it is a subgroup, the user keep the upper layer
        // in his structure.

    }


    // =====================================================================================================
    //
    // =====================================================================================================



    // =====================================================================================================
    //
    // =====================================================================================================


}
