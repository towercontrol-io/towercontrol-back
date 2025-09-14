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
    protected GroupsShortIdCache groupsShortIdCache;

    @Autowired
    protected GroupsCache groupsCache;

    @Autowired
    protected GroupRepository groupRepository;

    // =====================================================================================================
    // CREATE GROUPS
    // =====================================================================================================

    public void createSubGroup(
            String userId,
            GroupCreationBody body
    ) throws ITNotFoundException, ITRightException, ITParseException {

        // @TODO
        // check the user exitance and rights on the parent group
        // create the group and update the hierarchy and caches
        // no need to update the user groups & acl as the user is creating a sub group
        // check if the parent is a virtual group, in this case reject the creation




    }


    // =====================================================================================================
    //
    // =====================================================================================================



    // =====================================================================================================
    //
    // =====================================================================================================


}
