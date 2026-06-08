/*
 * Copyright (c) - Paul Pinault (aka disk91) - 2025.
 *
 *    Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 *    and associated documentation files (the "Software"), to deal in the Software without restriction,
 *    including without limitation the rights to use, copy, modify, merge, publish, distribute,
 *    sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 *    furnished to do so, subject to the following conditions:
 *
 *    The above copyright notice and this permission notice shall be included in all copies or
 *    substantial portions of the Software.
 *
 *    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 *    FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
 *    OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 *    WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 *    IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.disk91.common.config;

public class ModuleCatalog {
    public enum Modules {
        USERS,
        GROUPS,
        AUDIT,
        DEVICES,
        CAPTURE,
        BILLING,
        TICKETS,
        FILES,
        ALERTS,
        CUSTOM
    }

    public static String getServiceName(ModuleCatalog.Modules service) {
        if (service == ModuleCatalog.Modules.USERS) {
            return "users";
        } else if (service == ModuleCatalog.Modules.GROUPS) {
            return "groups";
        } else if (service == ModuleCatalog.Modules.AUDIT) {
            return "audit";
        } else if (service == ModuleCatalog.Modules.DEVICES) {
            return "devices";
        } else if (service == ModuleCatalog.Modules.CAPTURE) {
            return "capture";
        } else if (service == ModuleCatalog.Modules.BILLING) {
            return "billing";
        } else if (service == ModuleCatalog.Modules.TICKETS) {
            return "tickets";
        } else if (service == ModuleCatalog.Modules.FILES) {
            return "files";
        } else if (service == ModuleCatalog.Modules.ALERTS) {
            return "alerts";
        } else if (service == ModuleCatalog.Modules.CUSTOM) {
            return "custom";
        }
        return null;
    }
}