package com.disk91.users.config;

public class ActionCatalog {
    public enum Actions {
        REGISTRATION,
        CREATION,
        UNKNOWN,
    }

    public static String getActionName(Actions action) {
        switch (action) {
            case REGISTRATION:
                return "registration";
            case CREATION:
                return "creation";
            case UNKNOWN:
            default:
                return "unknown";
        }
    }
}
