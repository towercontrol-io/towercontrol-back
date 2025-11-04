package com.disk91.users.services;

import com.disk91.common.tools.exceptions.ITNotFoundException;
import com.disk91.users.mdb.entities.User;
import com.disk91.users.mdb.entities.sub.UserApiKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserCommon {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected UserCache userCache;

    /**
     * Verify a requestor can access a user profile for read or write. Currently, the detailed ACL are not managed
     * so R/W access is not supported and only global admin can access foreign accounts.
     * The requestor can be an APIKEY
     * @param __requestor
     * @param user
     * @return true when requestor can R/W access the user profile
     */
    public boolean isLegitAccess(String __requestor, String user, boolean writeAccess) {
        if ( User.isApiKey(__requestor) ) {
            // check the rights of the apikey.
            // apikey can only make change on users when it has the ROLE_USER_ADMIN
            // apikey can't be GOD_ADMIN
            // apikey can only read is account if not ROLE_USER_ADMIN
            try {
                User _requestor = userCache.getUserByApiKey(__requestor);
                UserApiKeys k = _requestor.getApiKey(__requestor);
                if ( ! _requestor.isActive() || _requestor.isLocked() ) return false;
                if ( ! _requestor.isInRole(UsersRolesCache.StandardRoles.ROLE_REGISTERED_USER)) return false;

                if ( writeAccess ) {
                    return ( k.isInRole(UsersRolesCache.StandardRoles.ROLE_USER_ADMIN) );
                    // @TODO manage groups
                } else {
                    return ( _requestor.getLogin().compareTo(user) == 0
                          || k.isInRole(UsersRolesCache.StandardRoles.ROLE_USER_ADMIN) );
                    // @TODO manage groups
                }
            } catch (ITNotFoundException e) {
                log.warn("[user] API Key {} not found", __requestor);
                return false;
            }
        } else {
            // Regular accounts works a bit differently, we have the GOD_ADMIN case
            try {
                User _requestor = userCache.getUser(__requestor);
                if ( ! _requestor.isActive() || _requestor.isLocked() ) return false;
                if ( ! _requestor.isInRole(UsersRolesCache.StandardRoles.ROLE_REGISTERED_USER)) return false;

                if ( writeAccess ) {
                    return (_requestor.isInRole(UsersRolesCache.StandardRoles.ROLE_GOD_ADMIN)
                            || _requestor.isInRole(UsersRolesCache.StandardRoles.ROLE_USER_ADMIN)
                            || _requestor.getLogin().compareTo(user) == 0
                    );
                    // @TODO manage groups
                } else {
                    return (_requestor.isInRole(UsersRolesCache.StandardRoles.ROLE_GOD_ADMIN)
                            || _requestor.isInRole(UsersRolesCache.StandardRoles.ROLE_USER_ADMIN)
                            || _requestor.getLogin().compareTo(user) == 0
                    );
                    // @TODO manage groups
                }
            } catch (ITNotFoundException e) {
                log.warn("[user] User {} not found", __requestor);
                return false;
            }
        }

    }
}
