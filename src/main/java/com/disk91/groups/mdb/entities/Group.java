/*
 * Copyright (c) - Paul Pinault (aka disk91) - 2024.
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

/**
 * UserPending -  This class stores account creation requests awaiting email validation and handles confirmations.
 * Entries are automatically removed either upon activation or after a specified period.
 */

package com.disk91.groups.mdb.entities;


import com.disk91.common.tools.CloneableObject;
import com.disk91.common.tools.Now;
import com.disk91.common.tools.exceptions.ITParseException;
import com.disk91.common.tools.exceptions.ITTooManyException;
import com.disk91.groups.mdb.entities.sub.GroupAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "groups")
@CompoundIndexes({
      @CompoundIndex(name = "name", def = "{'name': 'hashed'}"),
      @CompoundIndex(name = "shortId", def = "{'shortId': 'hashed'}"),
})
public class Group implements CloneableObject<Group> {

    @Transient
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Transient
    public static final int GROUP_VERSION = 1;

    @Id
    protected String id;

    // group unique identifier
    protected String shortId;

    // The group structure version
    protected int version;

    // The group name
    protected String name;

    // The group description
    protected String description;

    // The group associated language
    protected String language;

    // The group state (active / inactive) equivalent to deleted
    protected boolean active;

    // The group is virtual (not stored in the database)
    protected boolean virtual;

    // The group creation date
    protected long creationDateMs;

    // The group initiator
    protected String creationBy;

    // The group last update date
    protected long modificationDateMs;

    // The group deletion date request when the group has no more owners, keep it  bit before destorying it
    protected long deletionDateMs;

    // The group attributes to store per module information
    protected List<GroupAttribute> attributes;

    // The group referring groups
    protected List<String> referringGroups;

    // ========================================
    // Initialization
    public void init(String _name,String _description, String _shortId, String _language) {
        this.setShortId(_shortId);
        this.setName(_name);
        this.setDescription(_description);
        this.language = _language;

        this.version = GROUP_VERSION;
        this.active = true;
        this.virtual = false;
        this.creationDateMs = Now.NowUtcMs();
        this.modificationDateMs = this.creationDateMs;
        this.deletionDateMs = 0;
        this.attributes = new ArrayList<>();
        this.referringGroups = new ArrayList<>();
    }

    // ========================================
    public Group clone() {
        Group u = new Group();
        u.setId(id);
        u.setShortId(shortId);
        u.setVersion(version);
        u.setName(name);
        u.setDescription(description);
        u.setLanguage(language);
        u.setActive(active);
        u.setVirtual(virtual);
        u.setCreationDateMs(creationDateMs);
        u.setCreationBy(creationBy);
        u.setModificationDateMs(modificationDateMs);
        u.setDeletionDateMs(deletionDateMs);
        u.setAttributes(new ArrayList<>());
        for ( GroupAttribute attribute : attributes) {
            u.getAttributes().add(attribute.clone());
        }
        u.setReferringGroups(new ArrayList<>());
        for ( String group : referringGroups) {
            u.getReferringGroups().add(group);
        }
        return u;
    }

    /**
     * Add this group under another one, creating the hierarchy
     * Update the referring groups list automatically
     * The depth protection is to manipulate carefully as it is not a hierarchy depth but a number of
     *  reference and when a group is attached to multiple parents, the depth can grow quickly even
     *  if the hierarchy depth is low
     * @param under - group attachment
     * @maxDepth - maximum depth authorized
     * @throws ITTooManyException - in case of max depth
     * @Throws ITParseException - in case of loop detected
     */
    public void addUnderGroup(Group under, int maxDepth) throws ITTooManyException, ITParseException {
        if ( this.referringGroups == null ) this.referringGroups = new ArrayList<>();
        if ( under.getReferringGroups().contains(this.getShortId())) throw new ITParseException("group-loop-detected");
        if ( under.getShortId().compareTo(this.getShortId()) == 0 ) throw new ITParseException("group-loop-detected");
        if ( under.referringGroups.size() >= maxDepth ) throw new ITTooManyException("group-hierarchy-too-deep");
        this.referringGroups.addAll(under.referringGroups);
        this.referringGroups.add(under.getShortId());
    }


    // ========================================

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public long getCreationDateMs() {
        return creationDateMs;
    }

    public void setCreationDateMs(long creationDateMs) {
        this.creationDateMs = creationDateMs;
    }

    public String getCreationBy() {
        return creationBy;
    }

    public void setCreationBy(String creationBy) {
        this.creationBy = creationBy;
    }

    public long getModificationDateMs() {
        return modificationDateMs;
    }

    public void setModificationDateMs(long modificationDateMs) {
        this.modificationDateMs = modificationDateMs;
    }

    public List<GroupAttribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<GroupAttribute> attributes) {
        this.attributes = attributes;
    }

    public boolean isVirtual() {
        return virtual;
    }

    public void setVirtual(boolean virtual) {
        this.virtual = virtual;
    }

    public String getShortId() {
        return shortId;
    }

    public void setShortId(String shortId) {
        this.shortId = shortId;
    }

    public List<String> getReferringGroups() {
        return referringGroups;
    }

    public void setReferringGroups(List<String> referringGroups) {
        this.referringGroups = referringGroups;
    }

    public long getDeletionDateMs() {
        return deletionDateMs;
    }

    public void setDeletionDateMs(long deletionDateMs) {
        this.deletionDateMs = deletionDateMs;
    }
}
