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
import com.disk91.common.tools.exceptions.ITTooManyException;
import com.disk91.groups.tools.GroupsHierarchySimplified;
import com.disk91.groups.config.GroupsConfig;
import com.disk91.groups.mdb.entities.Group;
import com.disk91.groups.mdb.repositories.GroupRepository;
import com.disk91.groups.tools.GroupsList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class GroupsServices {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /*
     * This service provides access to groups and their hierarchy while maintaining the two caches
     * used by the application and enabling the functions necessary for group and rights management.
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
    // CACHES INTERACTIONS
    // =====================================================================================================


    private final Object locker = new Object();

    /**
     * Find a group by its shortId
     * Use cache
     * @param shortId
     * @return
     * @throws ITNotFoundException
     */
    public Group getGroupByShortId(String shortId) throws ITNotFoundException {
        synchronized (locker) {
            try {
                Group g = groupsCache.getGroup(shortId);
                groupsShortIdCache.getGroupList(shortId); // preload the shortId cache
                return g;
            } catch (ITNotFoundException e) {
                throw new ITNotFoundException("groups-not-found");
            }
        }
    }


    /**
     * Find a hierarchy by its head shortId
     * Use cache
     * @param shortId
     * @return
     * @throws ITNotFoundException
     */
    public GroupsList getGroupsListByShortId(String shortId) throws ITNotFoundException {
        synchronized (locker) {
            try {
                groupsCache.getGroup(shortId);
                return groupsShortIdCache.getGroupList(shortId);
            } catch (ITNotFoundException e) {
                throw new ITNotFoundException("groups-get-not-found");
            }
        }
    }


    /**
     * Following a modification of a group, flush the groups from the caches.
     * @param shortId
     */
    public void flushGroup(String shortId) {
        if ( groupsCache.isInCache(shortId) ) {
            try {
                // find all dependent groups
                GroupsList gl = groupsShortIdCache.getGroupList(shortId);
                for ( Group _g : gl.getList() ) {
                   // flush related groups
                   groupsCache.flushGroup(_g.getShortId());
                   // flush related hierarchy
                   groupsShortIdCache.flushGroup(_g);
                }

            } catch (ITNotFoundException x) {
                // just in case
                groupsCache.flushGroup(shortId);
            }
        }
    }

    /**
     * Save the Group structure after an update. The cache is flushed for this user
     * This is not protected against concurrent access on multiple cache service instance
     * There is a preference for flushing before writing to avoid flush inconsistency after the
     * group modification, better be based with previous state to flush.
     * @param g Group to be saved
     */
    synchronized public void saveGroup(Group g) {
        if ( g.getShortId().startsWith("user_") ) return; // virtual group, do nothing
        synchronized (locker) {
            this.flushGroup(g.getShortId());
            groupRepository.save(g);
        }
    }

    /**
     * Generate a new and non-existing shortId for a group
     * Short ID is a Random alphanumeric string of 6 characters
     * @return a random non existing shortId
     */
    public String getNewShortId() throws ITTooManyException {
        int loops = 0;
        do {
            String shortId = EncryptionHelper.getRandomString(groupsConfig.getGroupsShortidSize());
            Group g = groupRepository.findOneGroupByShortId(shortId);
            if ( g == null ) {
                return shortId;
            }
            loops++;
        } while (loops < 10);
        throw new ITTooManyException("groups-generate-shortid-too-many-retry");
    }

    // =====================================================================================================
    // USER GROUPS & RIGHTS
    // =====================================================================================================



    // =====================================================================================================
    // FRONT-END API SERVICES
    // =====================================================================================================

    /**
     * Creation of a set of group hierarchies corresponding to the groups list. This will be called
     * by the User service, as an example, to get the gowned group hierarchy for a user.
     * No right are verified here, it is the responsibility of the caller to ensure that.
     *
     * @param shortIds - list of groups (heads of the hierarchy) to explore
     * @return list of group hierarchies in a format usable by the front-end
     */
    public ArrayList<GroupsHierarchySimplified> getGroupsForDisplay(List<String> shortIds) throws ITNotFoundException {
        ArrayList<GroupsHierarchySimplified> res = new ArrayList<>();

        for ( String shortId : shortIds ) {
            try {
                GroupsList gl = this.getGroupsListByShortId(shortId);
                GroupsHierarchySimplified ghs = GroupsHierarchySimplified.getGroupsHierarchyResponseFromGroupList(gl);
                res.add(ghs);
            } catch (ITNotFoundException e) {
                throw new ITNotFoundException("groups-not-all-found");
            }
        }
        return res;

    }





}
