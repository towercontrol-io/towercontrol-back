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

import com.disk91.capture.mdb.entities.sub.MandatoryField;
import com.disk91.common.tools.CloneableObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "capture_protocols")
public class Protocols implements CloneableObject<Protocols> {

    @Transient
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Id
    private String id;

    // Protocol definition version, this allows to detect if related to an evolution of the protocol capabilities we
    // need to update the related capture points or device profiles... with, as an example some new mandatory fields.
    protected int version;

    // Protocol family is the higher layer protocol name (eg: LORAWAN, SIGFOX, etc)
    // it is a slug that can be used in i18n on front-end (eg: protocol-lorawan)
    protected String protocolFamily;

    // Type is a sub-definition of the protocol, related to the service provider in general
    // ex for LORAWAN it can be HELIUM, THE THINGS NETWORK, ORANGE, ACTILITY, etc
    // it usually defines the payload format to use.
    // The format is a slug as well (eg: protocol-type-helium)
    protected String protocolType;

    // Version is the protocol version from time to time evolution of the frame format
    // eg for Helium it can be legacy, chirpstack v4, later v5 ...
    // The format is a slug as well (eg: protocol-version-legacy)
    protected String protocolVersion;

    // Description is a slug description of the protocol definition, so it can be i18n translated on front-end
    // this is more for sharing different information than the protocol hierarchy defined above.
    protected String description;

    // Human-readable English description (short) for non i18n usage
    protected String enDescription;

    // Mandatory fields

    // Processing class name is the full qualified java class name that will process the payload
    // That way, new protocol processing can be added just by adding new classes implementing the processing interface
    // This interface is able to process the authentication verification and the payload decoding and conversion to
    // pivot object.
    protected String processingClassName;

    // login of the user who created the protocol entry (even if most of the time it will system)
    protected String creationBy;

    // creation date in MS since epoch
    protected long creationMs;

    protected List<MandatoryField> mandatoryFields;

    // This indicates the default value for wide open when creating a capture endpoint with this protocol
    protected boolean defaultWideOpen;

    // --------------------------------

    @Override
    public Protocols clone() {
        Protocols p = new Protocols();
        p.setId(this.id);
        p.setProtocolFamily(this.protocolFamily);
        p.setProtocolType(this.protocolType);
        p.setProtocolVersion(this.protocolVersion);
        p.setProcessingClassName(this.processingClassName);
        p.setCreationBy(this.creationBy);
        p.setCreationMs(this.creationMs);
        p.setDescription(this.description);
        p.setEnDescription(this.enDescription);
        p.setDefaultWideOpen(this.defaultWideOpen);
        p.setMandatoryFields(new ArrayList<>());
        if ( this.mandatoryFields != null ) {
            for ( MandatoryField mf : this.mandatoryFields ) {
                p.getMandatoryFields().add( mf.clone() );
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

    public String getProtocolFamily() {
        return protocolFamily;
    }

    public void setProtocolFamily(String protocolFamily) {
        this.protocolFamily = protocolFamily;
    }

    public String getProtocolType() {
        return protocolType;
    }

    public void setProtocolType(String protocolType) {
        this.protocolType = protocolType;
    }

    public String getProtocolVersion() {
        return protocolVersion;
    }

    public void setProtocolVersion(String protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    public String getProcessingClassName() {
        return processingClassName;
    }

    public void setProcessingClassName(String processingClassName) {
        this.processingClassName = processingClassName;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEnDescription() {
        return enDescription;
    }

    public void setEnDescription(String enDescription) {
        this.enDescription = enDescription;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public List<MandatoryField> getMandatoryFields() {
        return mandatoryFields;
    }

    public void setMandatoryFields(List<MandatoryField> mandatoryFields) {
        this.mandatoryFields = mandatoryFields;
    }

    public boolean isDefaultWideOpen() {
        return defaultWideOpen;
    }

    public void setDefaultWideOpen(boolean defaultWideOpen) {
        this.defaultWideOpen = defaultWideOpen;
    }
}
