package com.disk91.users.services;

import com.disk91.common.tools.Tools;
import com.disk91.common.tools.exceptions.ITNotFoundException;
import com.disk91.common.tools.exceptions.ITParseException;
import com.disk91.common.tools.exceptions.ITRightException;
import com.disk91.users.api.interfaces.UserAccessibleRolesResponse;
import com.disk91.users.config.UsersConfig;
import com.disk91.users.mdb.entities.Role;
import com.disk91.users.mdb.entities.User;
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
            List<Role> rs = getAvailableRoles(user);
            ArrayList<UserAccessibleRolesResponse> ret = new ArrayList<>();
            for ( Role r : rs ) {
                ret.add(UserAccessibleRolesResponse.getUserAccessibleRolesResponseFromRole(r));
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



}
