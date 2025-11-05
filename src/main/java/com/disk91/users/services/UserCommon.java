package com.disk91.users.services;

import com.disk91.common.tools.exceptions.ITNotFoundException;
import com.disk91.common.tools.exceptions.ITRightException;
import com.disk91.groups.mdb.entities.Group;
import com.disk91.groups.services.GroupsServices;
import com.disk91.users.mdb.entities.User;
import com.disk91.users.mdb.entities.sub.UserAcl;
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

    @Autowired
    protected GroupsServices groupsServices;

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
                    // self operation no write
                    // at least it needs to be ROLE_USER_ADMIN
                    if ( __requestor.compareTo(user) == 0 || !k.isInRole(UsersRolesCache.StandardRoles.ROLE_USER_ADMIN) ) return false;

                    // We need to check if we have common groups between the apikey and the user to manage
                    try {
                        User _user = userCache.getUser(user);
                        // search in groups
                        for ( String ug : _user.getGroups() ) {
                            Group g = groupsServices.getGroupByShortId(ug);
                            for (UserAcl a : k.getAcls()) {
                                if (g.isAChildOf(a.getGroup())) return true;
                            }
                        }
                        // search in ACL
                        for (UserAcl aUser : _user.getAcls()) {
                            Group g = groupsServices.getGroupByShortId(aUser.getGroup());
                            for (UserAcl a : k.getAcls()) {
                                if (g.isAChildOf(a.getGroup())) return true;
                            }
                        }
                        return false;
                    } catch (ITNotFoundException e) {
                        log.warn("[user] User {} or Group not found", user);
                        return false;
                    }
                } else {
                    // self operation read ok
                    if (  __requestor.compareTo(user) == 0 ) return true;
                    // ROLE_USER_ADMIN requires for the others
                    if (  !k.isInRole(UsersRolesCache.StandardRoles.ROLE_USER_ADMIN) ) return false;

                    // We need to check if we have common groups between the apikey and the user to manage
                    try {
                        User _user = userCache.getUser(user);
                        // search in groups
                        for ( String ug : _user.getGroups() ) {
                            Group g = groupsServices.getGroupByShortId(ug);
                            for (UserAcl a : k.getAcls()) {
                                if (g.isAChildOf(a.getGroup())) return true;
                            }
                        }
                        // search in ACL
                        for (UserAcl aUser : _user.getAcls()) {
                            Group g = groupsServices.getGroupByShortId(aUser.getGroup());
                            for (UserAcl a : k.getAcls()) {
                                if (g.isAChildOf(a.getGroup())) return true;
                            }
                        }
                        return false;
                    } catch (ITNotFoundException e) {
                        log.warn("[user] User {} or Group not found", user);
                        return false;
                    }
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

                //if ( writeAccess ) { // The way it's managed today is the same for read and write
                    // self modification ok
                    if ( _requestor.getLogin().compareTo(user) == 0 ) return true;
                    // God admin always win
                    if ( _requestor.isInRole(UsersRolesCache.StandardRoles.ROLE_GOD_ADMIN) ) return true;
                    // For the other, needs to be an admin and have common groups
                    User _user = userCache.getUser(user);
                    if ( _requestor.isInRole(UsersRolesCache.StandardRoles.ROLE_USER_ADMIN) ) {
                        // search in groups
                        for (String rg : _requestor.getAllGroups(true, false, false)) {
                            for (String ug : _user.getAllGroups(true, true, true)) {
                                Group g = groupsServices.getGroupByShortId(ug);
                                if (g.isAChildOf(rg)) return true;
                            }
                        }
                    }
                    // search in ACL only with right check (whatever the global roles)
                    for (UserAcl ra : _requestor.getAcls()) {
                        if ( ra.isInRole(UsersRolesCache.StandardRoles.ROLE_USER_ADMIN) ) {
                            for (String ug : _user.getAllGroups(true, true, true)) {
                                Group g = groupsServices.getGroupByShortId(ug);
                                if (g.isAChildOf(ra.getGroup())) return true;
                            }
                        }
                    }
                    // Not found
                    return false;
                //}
            } catch (ITNotFoundException e) {
                log.warn("[user] User {} not found", __requestor);
                return false;
            }
        }
    }


    /**
     * This verifies the access rights of a user with required Role for a given Group
     * The GOD_ADMIN role comes over the requiredRole and the standard required role (non affectable)
     * are checked first.
     * @param login - user login, it can be an apikey
     * @param requiredRole - role expected, can be null when no role required
     * @param orRole - second possible role expected, inclusive or
     * @param groupShort - group short id, can be null when no group context
     * @return the User structure (not the apikey entry, related user)
     * @throws ITNotFoundException - when not found user / apikey
     * @throws ITRightException - when the user does not have the required role or group context
     */
    public User getUserWithRolesAndGroups(
            String login,
            String requiredRole,
            String orRole,
            String groupShort,
            boolean includesVirtualGroups
    ) throws ITNotFoundException, ITRightException
    {
        if ( User.isApiKey(login) ) {
            // ---
            // API KEYS
            // check the rights of the apikey.
            // apikey can't be GOD_ADMIN
            try {
                User _u = userCache.getUserByApiKey(login);
                UserApiKeys k = _u.getApiKey(login);
                if ( ! _u.isActive() || _u.isLocked() ) throw new ITRightException("users-rights-inactive-locked");
                if ( ! _u.isInRole(UsersRolesCache.StandardRoles.ROLE_REGISTERED_USER)) throw new ITRightException("users-rights-not-registered-user");
                if ( ! _u.isInRole(UsersRolesCache.StandardRoles.ROLE_LOGIN_API)) throw new ITRightException("users-rights-not-signed-user");

                if ( groupShort != null ) {
                    // We need to get the group for checking rights
                    Group g = groupsServices.getGroupByShortId(groupShort); // raise ITNotFoundException if not found
                    for (UserAcl a : k.getAcls()) {
                        if (g.isAChildOf(a.getGroup())) {
                            // Found the group, check the role
                            if ( requiredRole != null && a.isInRole(requiredRole)
                                || ( orRole != null && a.isInRole(orRole) )
                            ) {
                                return _u;
                            } else {
                                if ( requiredRole != null || orRole != null ) {
                                    throw new ITRightException("users-rights-role-not-found-in-apikey");
                                } else {
                                    // No specific role required, group found is enough
                                    return _u;
                                }
                            }
                        }
                    }
                    throw new ITRightException("users-rights-role-not-found-in-apikey");
                } else {
                    // No group required, check the role on the apikey directly
                    if ( requiredRole != null && k.isInRole(requiredRole)
                            || ( orRole != null && k.isInRole(orRole) )
                    ) {
                        return _u;
                    } else {
                        if (requiredRole != null || orRole != null) {
                            throw new ITRightException("users-rights-role-not-found-in-apikey");
                        } else {
                            // No specific role required, group found is enough
                            return _u;
                        }
                    }
                }
            } catch (ITNotFoundException e) {
                throw new ITNotFoundException("users-apikey-or-group-not-found");
            }
        } else {
            // ----
            // Regular accounts works a bit differently, we have the GOD_ADMIN case
            try {
                User _u = userCache.getUser(login);
                if ( ! _u.isActive() || _u.isLocked() ) throw new ITRightException("users-rights-inactive-locked");
                if ( ! _u.isInRole(UsersRolesCache.StandardRoles.ROLE_REGISTERED_USER)) throw new ITRightException("users-rights-not-registered-user");

                if ( groupShort != null ) {
                    // We need to get the group for checking rights
                    Group g = groupsServices.getGroupByShortId(groupShort); // raise ITNotFoundException if not found
                    for ( String ug : _u.getAllGroups(true,false,includesVirtualGroups) ) {
                        if ( g.isAChildOf(ug) ) {
                            // Found the group, check the role
                            if ( requiredRole != null && _u.isInRole(requiredRole)
                                    || ( orRole != null &&  _u.isInRole(orRole) )
                            ) {
                                return _u;
                            } else {
                                if (requiredRole != null || orRole != null) {
                                    throw new ITRightException("users-rights-role-not-found-in-apikey");
                                } else {
                                    // No specific role required, group found is enough
                                    return _u;
                                }
                            }
                        }
                    }
                    // Search in ACL
                    for (UserAcl a : _u.getAcls()) {
                        if (g.isAChildOf(a.getGroup())) {
                            // Found the group, check the role
                            if ( requiredRole != null && a.isInRole(requiredRole)
                                    || ( orRole != null &&  a.isInRole(orRole) )
                            ) {
                                return _u;
                            } else {
                                if (requiredRole != null || orRole != null) {
                                    throw new ITRightException("users-rights-role-not-found-in-apikey");
                                } else {
                                    // No specific role required, group found is enough
                                    return _u;
                                }
                            }
                        }
                    }
                    throw new ITRightException("users-rights-role-not-found-in-apikey");
                } else {
                    // No group required, check the role on the apikey directly
                    if ( requiredRole != null && _u.isInRole(requiredRole)
                            || ( orRole != null &&  _u.isInRole(orRole) )
                    ) {
                        return _u;
                    } else {
                        if (requiredRole != null || orRole != null) {
                            throw new ITRightException("users-rights-role-not-found-in-apikey");
                        } else {
                            // No specific role required, group found is enough
                            return _u;
                        }
                    }
                }
            } catch (ITNotFoundException e) {
                throw new ITNotFoundException("users-group-not-found");
            }
        }
    }

}
