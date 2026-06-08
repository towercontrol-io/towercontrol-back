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
package com.disk91.alerts.mdb.entities.sub;

/**
 * AlertParameter - Enumeration of all supported dynamic parameters injectable into an alert message.
 * CUSTOM_PARAM entries carry an associated string value giving the custom parameter name.
 * ALERT_LINK carries an associated URL template where {aid}, {did}, {pubID} are replaced at render time.
 */
public enum AlertParameter {
    DEVICE_ID,
    DEVICE_NAME,
    GROUP_NAME,
    USER_FIRSTNAME,
    USER_LASTNAME,
    USER_GENDER,
    ALERT_TIME,         // ex 18:45
    ALERT_DATE_TIME,    // ex 2026-06-08 18:45 (UTC)
    CUSTOM_PARAM,       // alert specific: custom parameter, associated value gives the parameter name (lowercase)
    SERVICE_NAME,       // Name of the service (from configuration file)
    SERVICE_HOME,       // Service home page (from configuration file)
    ALERT_LINK,         // http link, associated value is the link containing {aid} {did} {pubID} placeholders

    UNKNOWN,
}

