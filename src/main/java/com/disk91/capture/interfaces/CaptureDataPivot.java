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
package com.disk91.capture.interfaces;

import com.disk91.capture.interfaces.sub.CaptureError;
import com.disk91.capture.interfaces.sub.CaptureMetaData;
import com.disk91.capture.interfaces.sub.CaptureNwkStation;
import com.disk91.common.tools.CloneableObject;
import com.disk91.common.tools.CustomField;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The Pivot format brings data from diverse sources and multiple protocols into a
 * common structure while preserving shared elements. Not all fields are
 * necessarily mapped — this depends on the protocol — but a value must be present
 * in each field for it to be interpretable by the functions that will later
 * process that data. Mainly metadata is handled here; there is no interpretation
 * of business data.
 * This structure is stored as Raw Data to keep metadata information when required.
 * for this a derivative class CaptureDataPivot is used to store the pivot information.
 *
 * See pivot_object.md for details.
 */
@Tag(name = "Capture data pivot format", description = "Common format to represent captured data from various protocols")
public class CaptureDataPivot implements CloneableObject<CaptureDataPivot>  {

    // ----------------------------------------------
    // Information enriched from the capture process

    @Schema(
            description = "Unique identified for a given received data frame",
            example = "xxxxx-yyyyy-xxxx-zzzz",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected UUID rxUuid;


    @Schema(
            description = "Server side reception timestamp in Ms since epoch",
            example = "1762503324",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected long rxTimestampMs;

    @Schema(
            description = "The associated CaptureEndpoint Reference ID",
            example = "AZGFBRDJ",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String rxCaptureRef;

    @Schema(
            description = "The raw payload from the network, stored as base64 encoded string, encrypted when required, encrypted string starts with $",
            example = "....",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    protected String payload;

    public enum NetworkStatus {
        NWK_STATUS_SUCCESS,
        NWK_STATUS_FAILURE,
    }

    @Schema(
            description = "The network status for the received frame",
            example = "NWK_STATUS_SUCCESS",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected NetworkStatus nwkStatus;

    public enum CaptureStatus {
        CAP_STATUS_SUCCESS, // Data received and fully processed
        CAP_STATUS_PARTIAL, // Data not completely received, processing decision to be managed by the next layer
        CAP_STATUS_FAILURE, // Functional Failure during capture processing, the frame will be stored but not processed further
    }

    @Schema(
            description = "The capture transformation and processing status for the received frame",
            example = "CAP_STATUS_SUCCESS",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected CaptureStatus status;

    @Schema(
            description = "In case of ingestion error, the protocol can store as raw data the incoming packet for later analysis. empty most of the time. String Base64 encoded, encrypted when required",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String coredDump;

    @Schema(
            description = "Store the owner identity (LoginID) coming from the JWT token used during ingestion",
            example = "D1C445E8...CA25",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String ingestOwnerId;

    @Schema(
            description = "IP address of the incoming data frame for traceability, filtering.... encrypted",
            example = "10.0.1.2",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected String fromIp;


    @Schema(
            description = "Headers received from the network or capture point, stored as key/value pairs",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected List<CustomField> headers;

    @Schema(
            description = "Errors reported by the network",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected List<CaptureError> nwkErrors;

    @Schema(
            description = "Errors reported by the capture process",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected List<CaptureError> errors;

    @Schema(
            description = "Meta Data associated with the capture process",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected CaptureMetaData metadata;

    @Schema(
            description = "List of station that received the frame",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected List<CaptureNwkStation> nwkStations;


    // -------------------------------------------

    public CaptureDataPivot clone() {
        CaptureDataPivot copy = new CaptureDataPivot();
        copy.rxUuid = this.rxUuid;
        copy.rxTimestampMs = this.rxTimestampMs;
        copy.rxCaptureRef = this.rxCaptureRef;
        copy.payload = this.payload;
        copy.nwkStatus = this.nwkStatus;
        copy.status = this.status;
        copy.ingestOwnerId = this.ingestOwnerId;
        copy.fromIp = this.fromIp;
        copy.coredDump = this.coredDump;

        copy.headers = new ArrayList<>();
        if (this.headers != null) {
            for (CustomField h : this.headers) {
                copy.headers.add(h.clone());
            }
        }

        copy.nwkErrors = new ArrayList<>();
        if (this.nwkErrors != null) {
            for (CaptureError e : this.nwkErrors) {
                copy.nwkErrors.add(e.clone());
            }
        }

        copy.errors = new ArrayList<>();
        if (this.errors != null) {
            for (CaptureError e : this.errors) {
                copy.errors.add(e.clone());
            }
        }

        if (this.metadata != null) {
            copy.metadata = this.metadata.clone();
        } else {
            copy.metadata = null;
        }

        copy.nwkStations = new ArrayList<>();
        if (this.nwkStations != null) {
            for (CaptureNwkStation s : this.nwkStations) {
                copy.nwkStations.add(s.clone());
            }
        }


        return copy;
    }


    // -------------------------------------------


    public UUID getRxUuid() {
        return rxUuid;
    }

    public void setRxUuid(UUID rxUuid) {
        this.rxUuid = rxUuid;
    }

    public long getRxTimestampMs() {
        return rxTimestampMs;
    }

    public void setRxTimestampMs(long rxTimestampMs) {
        this.rxTimestampMs = rxTimestampMs;
    }

    public String getRxCaptureRef() {
        return rxCaptureRef;
    }

    public void setRxCaptureRef(String rxCaptureRef) {
        this.rxCaptureRef = rxCaptureRef;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public NetworkStatus getNwkStatus() {
        return nwkStatus;
    }

    public void setNwkStatus(NetworkStatus nwkStatus) {
        this.nwkStatus = nwkStatus;
    }

    public CaptureStatus getStatus() {
        return status;
    }

    public void setStatus(CaptureStatus status) {
        this.status = status;
    }

    public List<CustomField> getHeaders() {
        return headers;
    }

    public void setHeaders(List<CustomField> headers) {
        this.headers = headers;
    }

    public List<CaptureError> getNwkErrors() {
        return nwkErrors;
    }

    public void setNwkErrors(List<CaptureError> nwkErrors) {
        this.nwkErrors = nwkErrors;
    }

    public List<CaptureError> getErrors() {
        return errors;
    }

    public void setErrors(List<CaptureError> errors) {
        this.errors = errors;
    }

    public CaptureMetaData getMetadata() {
        return metadata;
    }

    public void setMetadata(CaptureMetaData metadata) {
        this.metadata = metadata;
    }

    public List<CaptureNwkStation> getNwkStations() {
        return nwkStations;
    }

    public void setNwkStations(List<CaptureNwkStation> nwkStations) {
        this.nwkStations = nwkStations;
    }

    public String getIngestOwnerId() {
        return ingestOwnerId;
    }

    public void setIngestOwnerId(String ingestOwnerId) {
        this.ingestOwnerId = ingestOwnerId;
    }

    public String getFromIp() {
        return fromIp;
    }

    public void setFromIp(String fromIp) {
        this.fromIp = fromIp;
    }

    public String getCoredDump() {
        return coredDump;
    }

    public void setCoredDump(String coredDump) {
        this.coredDump = coredDump;
    }
}
