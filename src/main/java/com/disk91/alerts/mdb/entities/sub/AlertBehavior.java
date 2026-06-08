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
 * AlertBehavior - Defines how an alert template fires and rearms itself.
 * SILENT      : no notification is raised; only an audit log entry is written.
 * FIRE_FORGET : fire once and terminate immediately; a new trigger creates a new alarm.
 * FIRE_TO_END : fire and wait for an explicit cancellation before allowing a new fire; an expiration can also close it.
 * FIRE_UNTIL  : fire and wait for cancellation or the configured durationMs expiration before allowing a new fire.
 */
public enum AlertBehavior {
    SILENT,         // The Alert is not raised, but an audit log keeps a trace
    FIRE_FORGET,    // Raise the alarm and terminate it, the next trigger will raise a new alarm
    FIRE_TO_END,    // Raise the alarm and wait for termination to FIRE a new one, even if a cancel action is expected an expiration can be set
    FIRE_UNTIL,     // Raise the alarm and wait for termination or given expiration duration to FIRE a new one

    UNKNOWN,
}

