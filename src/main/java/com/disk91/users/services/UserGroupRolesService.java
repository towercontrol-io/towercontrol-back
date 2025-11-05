package com.disk91.users.services;

import com.disk91.common.tools.Tools;
import com.disk91.common.tools.exceptions.ITNotFoundException;
import com.disk91.common.tools.exceptions.ITParseException;
import com.disk91.common.tools.exceptions.ITRightException;
import com.disk91.groups.mdb.entities.Group;
import com.disk91.groups.services.GroupsServices;
import com.disk91.groups.tools.GroupsHierarchySimplified;
import com.disk91.users.api.interfaces.UserAccessibleRolesResponse;
import com.disk91.users.config.UsersConfig;
import com.disk91.users.mdb.entities.Role;
import com.disk91.users.mdb.entities.User;
import com.disk91.users.mdb.entities.sub.UserAcl;
import com.disk91.users.mdb.entities.sub.UserApiKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserGroupRolesService {

    /**
     * This class groups functions related to access rights management for users,
     * with respect to groups, ACLs, and roles.
     */

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private UsersConfig usersConfig;

    @Autowired
    private UserCache userCache;

    @Autowired
    private UsersRolesCache usersRolesCache;

    @Autowired
    private GroupsServices groupsServices;

    // =====================================================================
    // ROLE MANAGEMENT
    // =====================================================================


    /**
     * Get the list of the role a user can assign to another, based on his own roles
     * Only the assignable roles are returned
     * @param u User
     * @return
     */
    public List<Role> getAvailableRoles(User u) {
        List<Role> ret = new ArrayList<>();
        for ( String _r : u.getRoles() ) {
            try {
                Role r = usersRolesCache.getRole(_r);
                if ( r.isAssignable() ) ret.add(r);
            } catch (ITNotFoundException x) {
                // if the role does not exist, we do not affect it.
            }
        }
        return ret;
    }


    /**
     * Get the list of the role a user can assign to another, based on his own roles
     * Only the assignable roles are returned
     * @param u - user ID
     * @return
     */
    public List<UserAccessibleRolesResponse> getAvailableRoles(String u)
    throws ITRightException {

        try {
            User user = userCache.getUser(u);
            ArrayList<UserAccessibleRolesResponse> ret = new ArrayList<>();
            if ( User.isApiKey(u) ) {
                UserApiKeys k = user.getApiKey(u);
                for ( String r : k.getRoles() ) {
                    try {
                        Role role = usersRolesCache.getRole(r);
                        ret.add(UserAccessibleRolesResponse.getUserAccessibleRolesResponseFromRole(role));
                    } catch (ITNotFoundException x) {
                        // if the role does not exist, we do not affect it.
                    }
                }
            } else {

                if (user.isInRole(UsersRolesCache.StandardRoles.ROLE_GOD_ADMIN.name())) {
                    // all roles are accessible
                    List<Role> _ret = usersRolesCache.getRoles();
                    for (Role r : _ret) {
                        ret.add(UserAccessibleRolesResponse.getUserAccessibleRolesResponseFromRole(r));
                    }
                } else {
                    List<Role> rs = getAvailableRoles(user);
                    for (Role r : rs) {
                        ret.add(UserAccessibleRolesResponse.getUserAccessibleRolesResponseFromRole(r));
                    }
                }
            }
            return ret;
        } catch (ITNotFoundException x) {
            log.error("[users] user requesting group roles does not exist : {}", u);
            throw new ITRightException("user-role-user-does-not-exist");
        }

    }

    /**
     * Get a ROLE list based on the template (private version) or the default setting.
     * The invitationCode is not processed in community version as a single profile
     * is used for that version and is default, defined in configuration file.
     *
     * @param invitationCode - invitation code is provided by user on registration
     * @return list of Roles with cleaning (affectable, existing...)
     */
    public List<Role> getInvitationCodeRoles(String invitationCode) {

        // @TODO - for non community version, switch to specific behavior based on custom templates

        List<String> _roles = Tools.getStringListFromParam(usersConfig.getUserDefaultRoles());
        ArrayList<Role> roles = new ArrayList<>();
        for ( String _r : _roles ) {
            try {
                Role r = usersRolesCache.getRole(_r.toUpperCase());
                if ( r.isAssignable() ) roles.add(r);
            } catch (ITNotFoundException x) {
                // if the role does not exist, we do not affect it.
            }
        }
        return roles;

    }

    // =====================================================================
    // GROUP RIGHT MANAGEMENT
    // =====================================================================

    /**
      * Enriches the group hierarchy with the user's ACLs.
      *
      * @param h The simplified group hierarchy.
      * @param acls The list of the user's ACLs.
      */
    protected void enrichGroupHierarchyWithUserAcls(GroupsHierarchySimplified h, List<UserAcl> acls) {
        for ( UserAcl acl : acls ) {
            if ( acl.getGroup().compareTo(h.getShortId()) == 0 ) {
                h.setRoles(acl.getRoles());
                h.setName(acl.getLocalName());
                break;
            }
        }
        for ( GroupsHierarchySimplified child : h.getChildren() ) {
            enrichGroupHierarchyWithUserAcls(child, acls);
        }
    }

     /**
      * Retrieves the available groups for a user, with options to include
      * groups, ACLs and virtual groups.
      *
      * @param u The user identifier.
      * @param includesGroups Indicates whether groups should be included.
      * @param includesAcls Indicates whether ACLs should be included.
      * @param includesVirtual Indicates whether virtual groups should be included.
      * @return A list of simplified group hierarchies.
      * @throws ITParseException If some group elements are missing.
      */
    public List<GroupsHierarchySimplified> getAvailableGroups(String u, boolean includesGroups, boolean includesAcls, boolean includesVirtual)
        throws ITParseException {
        try {

            if ( User.isApiKey(u) ) {
                User user = userCache.getUser(u);
                UserApiKeys k = user.getApiKey(u);
                ArrayList<String> userGroups = new ArrayList<String>();
                for ( UserAcl acl : k.getAcls() ) {
                    userGroups.add(acl.getGroup());
                }
                return groupsServices.getGroupsForDisplay(userGroups);
            } else {
                User user = userCache.getUser(u);
                // compose the user group list (head of the hierarchy)
                // this includes the user default group and the standard groups and acls
                ArrayList<String> userGroups = user.getAllGroups(includesGroups, includesAcls, includesVirtual);
                List<GroupsHierarchySimplified> ret = groupsServices.getGroupsForDisplay(userGroups);
                // We need to enrich with the ACLs local modification if any
                List<UserAcl> acls = user.getAcls();
                for (GroupsHierarchySimplified h : ret) {
                    enrichGroupHierarchyWithUserAcls(h, acls);
                }
                return ret;
            }
        } catch (ITNotFoundException x) {
            log.warn("[users] user or some of the groups assigned to user {} do not exist : {}", u, x.getMessage());
            throw new ITParseException("user-group-missing-elements");
        }
    }

    /**
     * Add a group to the user profile after a group creation request
     * @param u
     * @param g
     * @throws ITNotFoundException
     */
    public void addGroup(User u, Group g) throws ITNotFoundException {
        // make sure we have the last version
        try {
            User _u = userCache.getUser(u.getLogin());
            u.getGroups().add(g.getShortId());
            userCache.saveUser(u);
        } catch (ITNotFoundException x) {
            throw new ITNotFoundException("user-profile-user-not-found");
        }
    }

}
