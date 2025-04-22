package com.disk91.users.config;

public class ActionCatalog {
    public enum Actions {
        REGISTRATION,
        CREATION,
        EULA_VALIDATION,
        REACTIVATION,
        UNKNOWN,
    }

    public static String getActionName(Actions action) {
        switch (action) {
            case REGISTRATION:
                return "registration";
            case CREATION:
                return "creation";
            case EULA_VALIDATION:
                return "eula_validation";
            case REACTIVATION:
                return "reactivation";
            case UNKNOWN:
            default:
                return "unknown";
        }
    }
}
