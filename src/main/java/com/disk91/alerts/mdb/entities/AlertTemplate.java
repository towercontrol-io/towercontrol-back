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
package com.disk91.alerts.mdb.entities;

import com.disk91.alerts.mdb.entities.sub.*;
import com.disk91.common.tools.CloneableObject;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;

/**
 * AlertTemplate - Persistent entity representing a reusable alert definition.
 * A template defines the name, description, ordered parameter list, per-locale open/close messages,
 * the firing behavior, the preferred delivery mediums, and an optional auto-close duration.
 * It is referenced by alert instances to render and deliver notifications.
 */
@Document(collection = "alert_templates")
@CompoundIndexes({
        @CompoundIndex(name = "name_idx", def = "{'name': 'hashed'}"),
        @CompoundIndex(name = "owner_idx", def = "{'owner': 'hashed'}"),
        @CompoundIndex(name = "shortId_idx", def = "{'shortId': 1}", unique = true)
})
public class AlertTemplate implements CloneableObject<AlertTemplate> {

    @Transient
    public static final int ALERT_TEMPLATE_VERSION = 1;

    @Id
    protected String id;

    // Short functional identifier used by the API and front-end (6 uppercase letters, unique)
    protected String shortId;

    // Structure version for future migrations
    protected int version;

    // Who has created this template
    protected String owner;

    // Is the template visible globally ?
    protected boolean global;

    // Human-readable name of the template, assigned by the user
    protected String name;

    // Free-text description of the template purpose, assigned by the user
    protected String description;

    // Ordered list of dynamic parameters injected into the message at render time
    protected ArrayList<AlertParameterEntry> parameters;

    // Messages sent when the alert fires (applicable to all behavior modes)
    protected ArrayList<AlertLocaleMessage> open;

    // Messages sent when the alert closes (applicable to FIRE_TO_END mode only)
    protected ArrayList<AlertLocaleMessage> close;

    // Firing and rearming behavior of this alert template
    protected AlertBehavior behavior;

    // Preferred delivery mediums, applied in order when multiple are enabled for a user
    protected ArrayList<AlertMedium> preferred;

    // Duration in milliseconds before the alert is automatically closed (FIRE_TO_END and FIRE_UNTIL modes); 0 means no expiration
    protected long durationMs;

    // ========================================

    /**
     * Factory - Build a minimal AlertTemplate ready to be persisted.
     * @param name        - human-readable template name
     * @param description - free-text description
     * @param behavior    - firing and rearming mode
     * @return initialised AlertTemplate instance with empty lists
     */
    public static AlertTemplate newAlertTemplate(
            String name,
            String description,
            String owner,
            boolean global,
            AlertBehavior behavior
    ) {
        AlertTemplate t = new AlertTemplate();
        t.setVersion(ALERT_TEMPLATE_VERSION);
        t.setName(name);
        t.setOwner(owner);
        t.setGlobal(global);
        t.setDescription(description);
        t.setParameters(new ArrayList<>());
        t.setOpen(new ArrayList<>());
        t.setClose(new ArrayList<>());
        t.setBehavior(behavior);
        t.setPreferred(new ArrayList<>());
        t.setDurationMs(0);
        return t;
    }

    // ========================================

    /**
     * Clone - produce a deep copy of this AlertTemplate.
     * @return a new AlertTemplate instance with all fields copied
     */
    public AlertTemplate clone() {
        AlertTemplate u = new AlertTemplate();
        u.setId(id);
        u.setShortId(shortId);
        u.setVersion(version);
        u.setName(name);
        u.setDescription(description);
        u.setOwner(owner);
        u.setGlobal(global);

        // Deep copy of ordered parameter list
        u.setParameters(new ArrayList<>());
        if (parameters != null) {
            for (AlertParameterEntry p : parameters) {
                u.getParameters().add(p.clone());
            }
        }

        // Deep copy of open locale messages
        u.setOpen(new ArrayList<>());
        if (open != null) {
            for (AlertLocaleMessage m : open) {
                u.getOpen().add(m.clone());
            }
        }

        // Deep copy of close locale messages
        u.setClose(new ArrayList<>());
        if (close != null) {
            for (AlertLocaleMessage m : close) {
                u.getClose().add(m.clone());
            }
        }

        u.setBehavior(behavior);

        // Deep copy of preferred medium list (enum values are immutable)
        u.setPreferred(new ArrayList<>());
        if (preferred != null) {
            u.getPreferred().addAll(preferred);
        }

        u.setDurationMs(durationMs);
        return u;
    }

    // ========================================
    // Getter / Setter

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getShortId() {
        return shortId;
    }

    public void setShortId(String shortId) {
        this.shortId = shortId;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ArrayList<AlertParameterEntry> getParameters() {
        return parameters;
    }

    public void setParameters(ArrayList<AlertParameterEntry> parameters) {
        this.parameters = parameters;
    }

    public ArrayList<AlertLocaleMessage> getOpen() {
        return open;
    }

    public void setOpen(ArrayList<AlertLocaleMessage> open) {
        this.open = open;
    }

    public ArrayList<AlertLocaleMessage> getClose() {
        return close;
    }

    public void setClose(ArrayList<AlertLocaleMessage> close) {
        this.close = close;
    }

    public AlertBehavior getBehavior() {
        return behavior;
    }

    public void setBehavior(AlertBehavior behavior) {
        this.behavior = behavior;
    }

    public ArrayList<AlertMedium> getPreferred() {
        return preferred;
    }

    public void setPreferred(ArrayList<AlertMedium> preferred) {
        this.preferred = preferred;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public boolean isGlobal() {
        return global;
    }

    public void setGlobal(boolean global) {
        this.global = global;
    }
}


