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
 * AlertState - Lifecycle states of an alert instance.
 * Transitions follow the alert behavior (FIRE_FORGET, FIRE_TO_END, FIRE_UNTIL, SILENT).
 */
public enum AlertState {
    PENDING,    // Alert created and stored, waiting for async processing
    PENDING_QUEUE, // Technical state, where the alert is in the queue before being processed
    FIRED,      // Notification sent (FIRE_FORGET only, transient before ENDED)
    RUNNING,    // Alert active, waiting for end signal or expiration (FIRE_TO_END / FIRE_UNTIL)
    ENDING,     // End event received, close notification pending
    ENDING_QUEUE, // Same as Pending Queue
    ENDED,      // Alert fully processed, retained for history until purge

    UNKNOWN,
}
