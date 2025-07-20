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
        UNKNOWN,
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
            case UNKNOWN:
            default:
                return "unknown";
        }
    }
}
