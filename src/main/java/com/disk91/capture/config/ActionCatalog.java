package com.disk91.capture.config;

public class ActionCatalog {
    public enum Actions {
        HACKING_DETECTED,


        UNKNOWN
    }

    public static String getActionName(Actions action) {
        switch (action) {
            case HACKING_DETECTED:
                return "hacking_detected";
            case UNKNOWN:
            default:
                return "unknown";
        }
    }
}
