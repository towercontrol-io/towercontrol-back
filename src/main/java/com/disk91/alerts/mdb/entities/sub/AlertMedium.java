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
 * AlertMedium - Supported delivery channels for alert notifications.
 * Individual channels (EMAIL, SMS, PUSH, WHATSAPP) are delivered per user.
 * Collective channels (WEBHOOK, TOPIC) are delivered using group-level settings.
 * DEFAULT is used when no specific channel is forced by the caller.
 */
public enum AlertMedium {
    EMAIL,      // Sent with email
    SMS,        // Sent with short message
    PUSH,       // Sent with a push message on smartphone
    WHATSAPP,   // Sent over Whatsapp channel (Not Implemented)

    WEBHOOK,    // Sent with a webhook
    TOPIC,      // Sent with a mqtt / amqp topic

    DEFAULT,    // Used when the channel is not specified
}

