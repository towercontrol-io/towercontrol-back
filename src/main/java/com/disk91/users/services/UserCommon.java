package com.disk91.users.services;

import com.disk91.users.mdb.entities.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UserCommon {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * Verify a requestor can access a user profile for read or write. Currently, the detailed ACL are not managed
     * so R/W access is not supported and only global admin can access foreign accounts
     * @param _requestor
     * @param user
     * @return true when requestor can R/W access the user profile
     */
    public boolean isLegitAccessRead(User _requestor, String user, boolean writeAccess) {
        if ( ! _requestor.isActive() || _requestor.isLocked() ) return false;
        if ( ! _requestor.isInRole(UsersRolesCache.StandardRoles.ROLE_REGISTERED_USER)) return false;

        if (    _requestor.isInRole(UsersRolesCache.StandardRoles.ROLE_GOD_ADMIN)
                ||  _requestor.isInRole(UsersRolesCache.StandardRoles.ROLE_USER_ADMIN)
                ||  _requestor.getLogin().compareTo(user) == 0
        ) {
            // the requestion is the user searched himself or it have admin / user admin global right
            // so we can hase the information
            return true;
        } else {
            // The user is not a global user admin. We may verify if the user can be a local user admin
            // for a group where this user is...
            // @TODO - manage the group ACL
            return false;
        }
    }
}
