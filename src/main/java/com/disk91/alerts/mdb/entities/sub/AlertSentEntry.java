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

import com.disk91.common.tools.Now;

import java.util.ArrayList;
import java.util.List;

/**
 * AlertSentEntry - Per-user delivery record within an alert instance.
 * One entry exists for each user targeted by the alert fan-out.
 * Each entry holds the list of channel delivery outcomes for that user.
 */
public class AlertSentEntry {

    // Login of the user this delivery record belongs to
    protected String userLogin;

    // Delivery outcome for each channel attempted for this user
    protected List<AlertSentState> state;

    // ==========================
    // upsert State

    public void upsertState(AlertMedium medium, boolean sent, boolean ack, String error) {
        if (state == null) state = new ArrayList<>();
        boolean found = false;
        for (AlertSentState s : state) {
            if (s.getMedium().equals(medium)) {
                // update
                s.setMedium(medium);
                s.setSent(sent);
                if ( sent ) s.setSentMs(Now.NowUtcMs());
                s.setAck(ack);
                if ( ack ) s.setAckMs(Now.NowUtcMs());
                s.setError(error);
                found = true;
            }
        }
        if (!found) {
            AlertSentState s = new AlertSentState();
            s.setMedium(medium);
            s.setSent(sent);
            if ( sent ) s.setSentMs(Now.NowUtcMs());
            s.setAck(ack);
            if ( ack ) s.setAckMs(Now.NowUtcMs());
            s.setError(error);
            state.add(s);
        }
    }

    // ==========================
    // Getters & Setters

    public String getUserLogin() { return userLogin; }
    public void setUserLogin(String userLogin) { this.userLogin = userLogin; }

    public List<AlertSentState> getState() { return state; }
    public void setState(List<AlertSentState> state) { this.state = state; }
}
