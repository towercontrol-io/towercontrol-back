package com.disk91.users.config;

public class ActionCatalog {
    public enum Actions {
        REGISTRATION,
        CREATION,
        DELETION,
        EULA_VALIDATION,
        REACTIVATION,
        LOGIN,
        LOGOUT,
        PASSWORD_RESET,
        PASSWORD_CHANGE,
        TWOFACTOR_CHANGE,
        PROFILE_UPDATE,
        RESTORATION,                // Restore from logical deletion
        ROLE_CHANGE,
        GROUP_CHANGE,
        ACL_CHANGE,
        APIKEY_CREATION,
        APIKEY_DELETION,
        APIKEY_RENEWAL,

        UNKNOWN
    }

    public static String getActionName(Actions action) {
        switch (action) {
            case REGISTRATION:
                return "registration";
            case CREATION:
                return "creation";
            case DELETION:
                return "deletion";
            case EULA_VALIDATION:
                return "eula_validation";
            case REACTIVATION:
                return "reactivation";
            case LOGIN:
                return "login";
            case LOGOUT:
                return "logout";
            case PASSWORD_RESET:
                return "password_reset";
            case PASSWORD_CHANGE:
                return "password_change";
            case TWOFACTOR_CHANGE:
                return "twofactor_change";
            case PROFILE_UPDATE:
                return "profile_update";
            case RESTORATION:
                return "restore";
            case ROLE_CHANGE:
                return "role_change";
            case GROUP_CHANGE:
                return "group_change";
            case ACL_CHANGE:
                return "acl_change";
            case APIKEY_CREATION:
                return "apikey_creation";
            case APIKEY_DELETION:
                return "apikey_deletion";
            case APIKEY_RENEWAL:
                return "apikey_renewal";
            case UNKNOWN:
            default:
                return "unknown";
        }
    }
}
