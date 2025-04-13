package com.disk91.users.config;

public class ActionCatalog {
    public enum Actions {
        REGISTRATION,
        UNKNOWN,
    }

    public static String getActionName(Actions action) {
        switch (action) {
            case REGISTRATION:
                return "registration";
            case UNKNOWN:
            default:
                return "unknown";
        }
    }
}
