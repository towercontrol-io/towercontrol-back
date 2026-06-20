/*
 * Copyright (c) - Paul Pinault (aka disk91) - 2026.
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
package com.disk91.alerts.config;

public class ActionCatalog {
    public enum Actions {
        AUDIT_TEMPLATE_CREATION,
        AUDIT_TEMPLATE_UPDATE,
        AUDIT_TEMPLATE_DELETE,

        AUDIT_ALERT_CREATED,
        AUDIT_ALERT_ENDED,

        UNKNOWN
    }

    public static String getActionName(Actions action) {
        switch (action) {
            case AUDIT_TEMPLATE_CREATION:
                return "alerts-template-created";
            case AUDIT_TEMPLATE_UPDATE:
                return "alerts-template-updated";
            case AUDIT_TEMPLATE_DELETE:
                return "alerts-template-deleted";

            case AUDIT_ALERT_CREATED:
                return "alerts-alert-created";
            case AUDIT_ALERT_ENDED:
                return "alerts-alert-ended";

            case UNKNOWN:
            default:
                return "unknown";
        }
    }
}
