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
 * AlertSentState - Delivery outcome for a single channel within one user's alert delivery record.
 * Nested inside AlertSentEntry, one instance per attempted medium.
 */
public class AlertSentState {

    // Delivery channel attempted for this entry
    protected AlertMedium medium;

    // True when the notification was submitted to the delivery provider
    protected boolean sent;

    // Sent timestamps
    protected long sentMs;

    // True when an acknowledgement was received from the provider (best-effort)
    protected boolean ack;

    // Ack Timestamp
    protected long ackMs;

    // Error description when the delivery failed; null or empty on success
    protected String error;

    // ==========================
    // Getters & Setters

    public AlertMedium getMedium() { return medium; }
    public void setMedium(AlertMedium medium) { this.medium = medium; }

    public boolean isSent() { return sent; }
    public void setSent(boolean sent) { this.sent = sent; }

    public boolean isAck() { return ack; }
    public void setAck(boolean ack) { this.ack = ack; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public long getSentMs() {
        return sentMs;
    }

    public void setSentMs(long sentMs) {
        this.sentMs = sentMs;
    }

    public long getAckMs() {
        return ackMs;
    }

    public void setAckMs(long ackMs) {
        this.ackMs = ackMs;
    }
}
