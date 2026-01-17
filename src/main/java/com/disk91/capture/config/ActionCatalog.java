package com.disk91.capture.config;

public class ActionCatalog {
    public enum Actions {
        HACKING_DETECTED,
        INGEST_QUEUE_FULL,
        INGEST_SERVICE_RESTORED,
        CREATE,
        DELETE,

        UNKNOWN
    }

    public static String getActionName(Actions action) {
        switch (action) {
            case HACKING_DETECTED:
                return "hacking_detected";
            case INGEST_QUEUE_FULL:
                return "ingest_queue_full";
            case INGEST_SERVICE_RESTORED:
                return "ingest_service_restored";
            case CREATE:
                return "create";
            case DELETE:
                return "delete";
            case UNKNOWN:
            default:
                return "unknown";
        }
    }
}
