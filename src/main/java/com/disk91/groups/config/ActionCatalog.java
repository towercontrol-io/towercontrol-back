package com.disk91.groups.config;

public class ActionCatalog {
    public enum Actions {
        CREATION,
        DELETION,

        UNKNOWN
    }

    public static String getActionName(Actions action) {
        switch (action) {
            case CREATION:
                return "creation";
            case DELETION:
                return "deletion";
            case UNKNOWN:
            default:
                return "unknown";
        }
    }
}
