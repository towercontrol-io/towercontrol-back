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
package com.disk91.capture.mdb.entities;

import com.disk91.capture.mdb.entities.sub.IdStateEnum;
import com.disk91.common.tools.CustomField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "capture_protocol_ids")
@CompoundIndexes({
        @CompoundIndex(name = "cap_pro_ids_capure_id", def = "{'captureId': 'hashed'}"),
        @CompoundIndex(name = "cap_pro_ids_capure_id_state", def = "{'captureId': 'hashed', 'state': 1}"),
})
public class ProtocolIds {

    @Transient
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Id
    private String id;

    // Link with the protocol definition for this ID set. important to filter the IDs when the pool contains different
    // IDs list for different protocol definitions. Also important to retrieve the right protocol definition to interpret
    // the credentials names.
    protected String protocolId;

    // Link with the right ID protocol definition, this is a sub definition inside the protocol definition as a protocol
    // can handle different type of IDs.
    protected String configTypeId;

    // Link with the capture endpoint associated to this ID set, important to filter the IDs based on the capture endpoint
    // as this is the common way we assigned a subscription provider in the platform.
    protected String captureId;

    // Custom Configuration, the protocol defines the mandatory fields to fill in here
    // we can have extra parameters here as well
    protected List<CustomField> customConfig;

    // ID set assignment and subscription state
    protected IdStateEnum state;

    // login of the user who created the protocol entry (even if most of the time it will system)
    protected String creationBy;

    // creation date in MS since epoch
    protected long creationMs;

    // creation date in MS since epoch
    protected long updateMs;

    // Last date in MS the IDs has been scanned by background tasks
    protected long lastScanMs;

    // assignment date in MS since epoch
    protected long assignedMs;

    // assignment date in MS since epoch
    protected long releasedMs;

    // assignment date in MS since epoch
    protected long subscriptionStartMs;

    // assignment date in MS since epoch
    protected long subscriptionEndMs;

    // assignment date in MS since epoch
    protected long removalMs;


    // --------------------------------


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProtocolId() {
        return protocolId;
    }

    public void setProtocolId(String protocolId) {
        this.protocolId = protocolId;
    }

    public String getCaptureId() {
        return captureId;
    }

    public void setCaptureId(String captureId) {
        this.captureId = captureId;
    }

    public String getConfigTypeId() {
        return configTypeId;
    }

    public void setConfigTypeId(String configTypeId) {
        this.configTypeId = configTypeId;
    }

    public List<CustomField> getCustomConfig() {
        return customConfig;
    }

    public void setCustomConfig(List<CustomField> customConfig) {
        this.customConfig = customConfig;
    }

    public IdStateEnum getState() {
        return state;
    }

    public void setState(IdStateEnum state) {
        this.state = state;
    }

    public String getCreationBy() {
        return creationBy;
    }

    public void setCreationBy(String creationBy) {
        this.creationBy = creationBy;
    }

    public long getCreationMs() {
        return creationMs;
    }

    public void setCreationMs(long creationMs) {
        this.creationMs = creationMs;
    }

    public long getUpdateMs() {
        return updateMs;
    }

    public void setUpdateMs(long updateMs) {
        this.updateMs = updateMs;
    }

    public long getAssignedMs() {
        return assignedMs;
    }

    public void setAssignedMs(long assignedMs) {
        this.assignedMs = assignedMs;
    }

    public long getReleasedMs() {
        return releasedMs;
    }

    public void setReleasedMs(long releasedMs) {
        this.releasedMs = releasedMs;
    }

    public long getSubscriptionStartMs() {
        return subscriptionStartMs;
    }

    public void setSubscriptionStartMs(long subscriptionStartMs) {
        this.subscriptionStartMs = subscriptionStartMs;
    }

    public long getSubscriptionEndMs() {
        return subscriptionEndMs;
    }

    public void setSubscriptionEndMs(long subscriptionEndMs) {
        this.subscriptionEndMs = subscriptionEndMs;
    }

    public long getRemovalMs() {
        return removalMs;
    }

    public void setRemovalMs(long removalMs) {
        this.removalMs = removalMs;
    }

    public long getLastScanMs() {
        return lastScanMs;
    }

    public void setLastScanMs(long lastScanMs) {
        this.lastScanMs = lastScanMs;
    }
}
