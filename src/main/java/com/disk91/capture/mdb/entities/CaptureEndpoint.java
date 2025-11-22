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

import com.disk91.common.tools.CloneableObject;
import com.disk91.common.tools.CustomField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "capture_endpoints")
public class CaptureEndpoint implements CloneableObject<CaptureEndpoint> {

    @Transient
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Id
    private String id;

    // User-friendly name for the capture endpoint, free text
    protected String name;

    // User description of the capture endpoint, free text
    protected String description;

    // Reference, this is a générated key used to identify the protocol from the data flow, shorter than an ID
    protected String ref;

    // Ower who created the capture endpoint
    protected String owner;

    // Creation time
    protected long creationMs;

    // Link with the protocol associated to this capture endpoint
    protected String protocolId;

    // Custom Configuration, the protocol defines the mandatory fields to fill in here
    // we can have extra parameters here as well
    protected List<CustomField> customConfig;


    // --------------------------------

    @Override
    public CaptureEndpoint clone() {
        CaptureEndpoint p = new CaptureEndpoint();
        p.setId(this.id);
        p.setName(this.name);
        p.setDescription(this.description);
        p.setRef(this.ref);
        p.setOwner(this.owner);
        p.setCreationMs(this.creationMs);
        p.setProtocolId(this.protocolId);
        p.setCustomConfig(new ArrayList<>());
        if ( this.customConfig != null ) {
            for ( CustomField cf : this.customConfig ) {
                p.getCustomConfig().add( cf.clone() );
            }
        }
        return p;
    }

    // --------------------------------


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public long getCreationMs() {
        return creationMs;
    }

    public void setCreationMs(long creationMs) {
        this.creationMs = creationMs;
    }

    public String getProtocolId() {
        return protocolId;
    }

    public void setProtocolId(String protocolId) {
        this.protocolId = protocolId;
    }

    public List<CustomField> getCustomConfig() {
        return customConfig;
    }

    public void setCustomConfig(List<CustomField> customConfig) {
        this.customConfig = customConfig;
    }
}
