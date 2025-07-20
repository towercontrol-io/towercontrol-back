package com.disk91.common.config;

public class ModuleCatalog {
    public enum Modules {
        USERS,
        GROUPS,
        AUDIT
    }

    public static String getServiceName(ModuleCatalog.Modules service) {
        if (service == ModuleCatalog.Modules.USERS) {
            return "users";
        } else if (service == ModuleCatalog.Modules.GROUPS) {
            return "groups";
        } else if (service == ModuleCatalog.Modules.AUDIT) {
            return "audit";
        }
        return null;
    }
}