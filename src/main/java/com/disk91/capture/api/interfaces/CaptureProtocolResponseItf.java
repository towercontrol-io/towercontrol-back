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
package com.disk91.capture.api.interfaces;

import com.disk91.capture.mdb.entities.Protocols;
import com.disk91.capture.mdb.entities.sub.MandatoryField;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.ArrayList;
import java.util.List;

@Tag(name = "Capture Protocols", description = "Capture protocols definition")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CaptureProtocolResponseItf {

    @Schema(
            description = "protocol unique identifier",
            example = "",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String id;

    // Protocol definition version, this allows to detect if related to an evolution of the protocol capabilities we
    // need to update the related capture points or device profiles... with, as an example some new mandatory fields.
    @Schema(
            description = "Protocol version",
            example = "1",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected int version;

    // Protocol family is the higher layer protocol name (eg: LORAWAN, SIGFOX, etc)
    // it is a slug that can be used in i18n on front-end (eg: protocol-lorawan)
    @Schema(
            description = "Protocol family",
            example = "protocol-lorawan",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String protocolFamily;

    // Type is a sub-definition of the protocol, related to the service provider in general
    // ex for LORAWAN it can be HELIUM, THE THINGS NETWORK, ORANGE, ACTILITY, etc
    // it usually defines the payload format to use.
    // The format is a slug as well (eg: protocol-type-helium)
    @Schema(
            description = "Protocol type",
            example = "protocol-type-helium",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String protocolType;

    // Version is the protocol version from time to time evolution of the frame format
    // eg for Helium it can be legacy, chirpstack v4, later v5 ...
    // The format is a slug as well (eg: protocol-version-legacy)
    @Schema(
            description = "Protocol type stack version",
            example = "protocol-version-legacy",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String protocolVersion;

    // Description is a slug description of the protocol definition, so it can be i18n translated on front-end
    // this is more for sharing different information than the protocol hierarchy defined above.
    @Schema(
            description = "Protocol slug description",
            example = "protocol-slug...",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String description;

    // Human-readable English description (short) for non i18n usage
    @Schema(
            description = "Protocol plaintext English description",
            example = "LoRaWAN Helium legacy protocol",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String enDescription;


    @Schema(
            description = "Definition of the mandatory fields to setup a capture endpoint with this protocol",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected List<MandatoryField> mandatoryFields;

    // This indicates the default value for wide open when creating a capture endpoint with this protocol
    @Schema(
            description = "This protocol is wide open by default (no ownership right required to send data)",
            example = "false",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected boolean defaultWideOpen;

    // --------------------------------

    public static CaptureProtocolResponseItf createFromProtocol(Protocols pr) {
        CaptureProtocolResponseItf p = new CaptureProtocolResponseItf();
        p.setId(pr.getId());
        p.setVersion(pr.getVersion());
        p.setProtocolFamily(pr.getProtocolFamily());
        p.setProtocolType(pr.getProtocolType());
        p.setProtocolVersion(pr.getProtocolVersion());
        p.setDescription(pr.getDescription());
        p.setEnDescription(pr.getEnDescription());
        p.setDefaultWideOpen(pr.isDefaultWideOpen());
        p.setMandatoryFields(new ArrayList<>());
        if ( pr.getMandatoryFields() != null ) {
            for ( MandatoryField mf : pr.getMandatoryFields() ) {
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

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
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
