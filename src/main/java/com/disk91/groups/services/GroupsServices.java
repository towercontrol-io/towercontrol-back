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

import com.disk91.common.api.interfaces.ActionResult;
import com.disk91.common.config.ModuleCatalog;
import com.disk91.common.tools.EncryptionHelper;
import com.disk91.common.tools.Now;
import com.disk91.common.tools.exceptions.ITNotFoundException;
import com.disk91.common.tools.exceptions.ITParseException;
import com.disk91.common.tools.exceptions.ITTooManyException;
import com.disk91.groups.tools.GroupsHierarchySimplified;
import com.disk91.groups.config.GroupsConfig;
import com.disk91.groups.mdb.entities.Group;
import com.disk91.groups.mdb.repositories.GroupRepository;
import com.disk91.groups.tools.GroupsList;
import com.disk91.integration.api.interfaces.IntegrationCallback;
import com.disk91.integration.api.interfaces.IntegrationQuery;
import com.disk91.integration.services.IntegrationService;
import com.disk91.users.mdb.entities.User;
import com.disk91.users.mdb.entities.sub.UserAcl;
import com.disk91.users.services.UsersRolesCache;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.disk91.groups.integration.GroupActions.GROUPS_ACTION_FLUSH_CACHE_GROUP;
import static com.disk91.groups.integration.GroupActions.GROUPS_ACTION_FLUSH_CACHE_SHORTID;

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

    @Autowired
    protected IntegrationService integrationService;

    // =====================================================================================================
    // SERVICE INIT
    // =====================================================================================================

    @PostConstruct
    public void init() {
        log.info("[groups] Init service");

        // Init the integration actions
        try {
            integrationService.registerCallback(
                    ModuleCatalog.Modules.GROUPS,
                    new IntegrationCallback() {
                        @Override
                        public void onIntegrationEvent(IntegrationQuery q) {
                            if ( q.getAction() == GROUPS_ACTION_FLUSH_CACHE_GROUP.ordinal() ) {
                                String shortid = (String) q.getQuery();
                                groupsCache.flushGroup(shortid);
                                // terminate the action
                                q.setResponse(ActionResult.OK("Group cache flushed")); // fire & forget, success on every actions
                                q.setResult(null);
                                q.setState(IntegrationQuery.QueryState.STATE_DONE);
                                q.setResponse_ts(Now.NanoTime());
                            } else if ( q.getAction() == GROUPS_ACTION_FLUSH_CACHE_SHORTID.ordinal() ) {
                                Group g = (Group) q.getQuery();
                                groupsShortIdCache.flushGroup(g);
                                // terminate the action
                                q.setResponse(ActionResult.OK("Group hierarchy cache flushed")); // fire & forget, success on every actions
                                q.setResult(null);
                                q.setState(IntegrationQuery.QueryState.STATE_DONE);
                                q.setResponse_ts(Now.NanoTime());
                            } else {
                                log.error("[groups] Receiving a unknown message from integration");
                                // terminate the action
                                q.setResponse(ActionResult.BADREQUEST("groups-integration-unknown-action"));
                                q.setResult(null);
                                q.setState(IntegrationQuery.QueryState.STATE_ERROR);
                                q.setResponse_ts(Now.NanoTime());
                            }
                        }
                    }
            );
        } catch (ITParseException | ITTooManyException x) {
            log.error("[users] Failed to register groups integration callback: {}", x.getMessage());
        }

    }


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
        if ( Group.isVirtualGroup(g.getShortId()) ) return; // virtual group, do nothing
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

    /**
     * Check if a user is in a group, directly or through sub-groups.
     * When scanAcl is true, we verify if the user have the admin rights in case of ACL depends on forAdmin
     * considerVirtual is to check the virtual group membership (default group)
     *
     * @param user -  User who have the list of groups & right access
     * @param groupShortId - The group we search in the hierarchy
     * @param scanAcl - To also scan in the ACLs
     * @param considerVirtual - To also scan in the virtual groups
     * @param forAdmin - To check for admin right in case of scanAcl ; if not, the group is skipped
     * @return true when the user is in the group with enough rights for ACL (Groups right must be checked outside)
     */
    public boolean isUserInGroup(User user, String groupShortId, boolean scanAcl, boolean considerVirtual, boolean forAdmin) {
        return findGroupUserForGroup(user, groupShortId, scanAcl, considerVirtual, forAdmin) != null;
    }

    /**
     * Search if a user is in a group, directly or through sub-groups.
     * When scanAcl is true, we verify if the user have the admin rights in case of ACL depends on forAdmin
     * considerVirtual is to check the virtual group membership (default group)
     *
     * @param user -  User who have the list of groups & right access
     * @param groupShortId - The group we search in the hierarchy
     * @param scanAcl - To also scan in the ACLs
     * @param considerVirtual - To also scan in the virtual groups
     * @param forAdmin - To check for admin right in case of scanAcl ; if not, the group is skipped
     * @return the user group / acl corresponding to the group searched (considering the hierarchy) or null if not found
     */
    public String findGroupUserForGroup(User user, String groupShortId, boolean scanAcl, boolean considerVirtual, boolean forAdmin) {
        // check the virtual group case
        if ( considerVirtual && groupShortId.compareTo(user.getDefaultGroupId()) == 0 ) {
            return groupShortId;
        }

        try {
            Group g = this.getGroupByShortId(groupShortId);
            for ( String userGroup : user.getAllGroups(true,false,considerVirtual) ) {
                if ( userGroup.compareTo(g.getShortId()) == 0 ) {
                    // direct membership
                    return userGroup;
                }
                for ( String refGroup : g.getReferringGroups() ) {
                    if ( userGroup.compareTo(refGroup) == 0 ) {
                        // indirect membership - through parent group
                        return userGroup;
                    }
                }
            }
            if ( scanAcl ) {
                for (UserAcl acl : user.getAcls() ) {
                    if ( acl.getGroup().compareTo(g.getShortId()) == 0 ) {
                        if ( forAdmin ) {
                            if ( acl.isInRole(UsersRolesCache.StandardRoles.ROLE_GROUP_ADMIN) ) return acl.getGroup();
                        } else {
                            return acl.getGroup();
                        }
                    }
                    for ( String refGroup : g.getReferringGroups() ) {
                        if ( acl.getGroup().compareTo(refGroup) == 0 ) {
                            if ( forAdmin ) {
                                if ( acl.isInRole(UsersRolesCache.StandardRoles.ROLE_GROUP_ADMIN) ) return acl.getGroup();;
                            } else {
                                return acl.getGroup();
                            }
                        }
                    }
                }
            }
            return null;
        } catch (ITNotFoundException x) {
            return null;
        }
    }



    /**
     * Check if a group is in another one, directly or through parents.
     * @param groupToSearch - The group we search in the hierarchy
     * @param groupToCheck - The group used as base of the hierarchy
     * @return true when the groupToCheck is in the groupToSearch
     */
    public boolean isInGroup(String groupToSearch, String groupToCheck) {
        if ( groupToSearch.compareTo(groupToCheck) == 0 ) return true;
        try {
            Group g = this.getGroupByShortId(groupToCheck);
            for ( String refGroup : g.getReferringGroups() ) {
                if ( groupToSearch.compareTo(refGroup) == 0 ) return true;
            }
            return false;
        } catch (ITNotFoundException x) {
            return false;
        }
    }


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


    // =====================================================================================================
    // GROUP DELETION
    // =====================================================================================================

    /**
     * When a group is removed from a user profile, we need to check if that group still has an owner
     * If there are no-more owner, the group and its sub-groups must be deleted, this can also make
     * devices to be deleted. We could eventually mark the group as to be deleted, so the group could
     * be in a sort of purgatory and restored by GROUP_ADMIN later on.
     *
     * We have to check also the hierarchy under this group is not associated also. The group is market as inactive
     * and the deletion date is set. The groups.retention.ms.before.delete config variable gives the retention duration
     *
     * This take in consideration the current branch and a group in different branch may not be marked as to be deleted
     *
     * @param groupId
     */
    public void groupCascadeDeletionIfNoOwner(String groupId) {
        // @TODO - to be implemented
    }

    /**
     * When a group has a new owner and was pending deletion, we can restore it to active state
     * We also need to restore the hierarchy under this group.
     * @param groupId
     */
    public void groupCascadeResume(String groupId) {
        // @TODO - to be implemented
        /*
        _g.setActive(true);
        _g.setDeletionDateMs(0);
        _g.setModificationDateMs(Now.NowUtcMs());
        groupsServices.saveGroup(_g);
        */
    }


    /**
     * On group deletion cascade. When a group is deleted, we need to clear all the reference in the other groups
     * and clear the lower level groups ; making sure there have no other links ...
     */
}
