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

    // When the source can be any device directly with dedicated authorization key, we can't match with an ower
    protected boolean wideOpen;

    // When true, the sensitive data will be encrypted like the payload in the pivot objet stored in DB
    protected boolean encrypted;

    // Creation time
    protected long creationMs;

    // Link with the protocol associated to this capture endpoint
    protected String protocolId;

    // Custom Configuration, the protocol defines the mandatory fields to fill in here
    // we can have extra parameters here as well
    protected List<CustomField> customConfig;

    // Processing class name is the full qualified java class name that will process the payload
    // This is not the same level as the protocol processing class. This class is the one used
    // to process the pivot object after protocol processing.
    protected String processingClassName;

    // ----- STATS -------

    // Total frames received on the endpoint
    protected long totalFramesReceived = 0L;
    // Total frames refused because JWT owner is invalid
    protected long totalBadOwnerRefused = 0L;
    // Total frames accepted and passed to pivot transformation
    protected long totalFramesAcceptedToPivot = 0L;
    // Total frames pivoted and passed to processing
    protected long totalFramesAcceptedToProcess = 0L;
    // Total frames rejected due to payload format
    protected long totalBadPayloadFormat = 0L;
    // Total frames rejected due to device right exception
    protected long totalBadDeviceRight = 0L;
    // Total frames in the different raw -> pivot Driver
    protected long totalInDriver = 0L;
    // Total frames queued to be processed
    protected long totalQueuedToProcess = 0L;
    // Total frames refused due to billing restrictions
    protected long totalBillingRefused = 0L;

    // --------------------------------

    public synchronized void resetstats() {
        this.totalFramesReceived = 0L;
        this.totalFramesAcceptedToPivot = 0L;
        this.totalFramesAcceptedToProcess = 0L;
        this.totalBadDeviceRight = 0L;
        this.totalBadPayloadFormat = 0L;
        this.totalBadOwnerRefused = 0L;
        this.totalInDriver = 0L;
        this.totalQueuedToProcess = 0L;
        this.totalBillingRefused = 0L;
    }

    public synchronized void incTotalFramesReceived() {
        this.totalFramesReceived++;
    }

    public synchronized void incTotalBadOwnerRefused() {
        this.totalBadOwnerRefused++;
    }

    public synchronized void incTotalFramesAcceptedToPivot() {
        this.totalFramesAcceptedToPivot++;
    }

    public synchronized void incTotalFramesAcceptedToProcess() {
        this.totalFramesAcceptedToProcess++;
    }

    public synchronized void incTotalBadPayloadFormat() {
        this.totalBadPayloadFormat++;
    }

    public synchronized void incTotalBadDeviceRight() {
        this.totalBadDeviceRight++;
    }

    public synchronized void incTotalInDriver() {
        this.totalInDriver++;
    }

    public synchronized void incTotalQueuedToProcess() {this.totalQueuedToProcess++;}

    public synchronized void incTotalBillingRefused() {this.totalBillingRefused++;}
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
        p.setWideOpen(this.wideOpen);
        p.setEncrypted(this.encrypted);
        p.setProcessingClassName(this.processingClassName);
        p.setCustomConfig(new ArrayList<>());
        if ( this.customConfig != null ) {
            for ( CustomField cf : this.customConfig ) {
                p.getCustomConfig().add( cf.clone() );
            }
        }

        p.setTotalFramesReceived(this.totalFramesReceived);
        p.setTotalFramesAcceptedToPivot(this.totalFramesAcceptedToPivot);
        p.setTotalFramesAcceptedToProcess(this.totalFramesAcceptedToProcess);
        p.setTotalBadOwnerRefused(this.totalBadOwnerRefused);
        p.setTotalBadDeviceRight(this.totalBadDeviceRight);
        p.setTotalBadPayloadFormat(this.totalBadPayloadFormat);
        p.setTotalInDriver(this.totalInDriver);
        p.setTotalQueuedToProcess(this.totalQueuedToProcess);
        p.setTotalBillingRefused(this.totalBillingRefused);
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

    public boolean isWideOpen() {
        return wideOpen;
    }

    public void setWideOpen(boolean wideOpen) {
        this.wideOpen = wideOpen;
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public void setEncrypted(boolean encrypted) {
        this.encrypted = encrypted;
    }

    public String getProcessingClassName() {
        return processingClassName;
    }

    public void setProcessingClassName(String processingClassName) {
        this.processingClassName = processingClassName;
    }

    public long getTotalFramesReceived() {
        return totalFramesReceived;
    }

    synchronized public void setTotalFramesReceived(long totalFramesReceived) {
        this.totalFramesReceived = totalFramesReceived;
    }

    public long getTotalBadOwnerRefused() {
        return totalBadOwnerRefused;
    }

    public void setTotalBadOwnerRefused(long totalBadOwnerRefused) {
        this.totalBadOwnerRefused = totalBadOwnerRefused;
    }

    public long getTotalFramesAcceptedToPivot() {
        return totalFramesAcceptedToPivot;
    }

    public void setTotalFramesAcceptedToPivot(long totalFramesAcceptedToPivot) {
        this.totalFramesAcceptedToPivot = totalFramesAcceptedToPivot;
    }

    public long getTotalFramesAcceptedToProcess() {
        return totalFramesAcceptedToProcess;
    }

    public void setTotalFramesAcceptedToProcess(long totalFramesAcceptedToProcess) {
        this.totalFramesAcceptedToProcess = totalFramesAcceptedToProcess;
    }

    public long getTotalBadPayloadFormat() {
        return totalBadPayloadFormat;
    }

    public void setTotalBadPayloadFormat(long totalBadPayloadFormat) {
        this.totalBadPayloadFormat = totalBadPayloadFormat;
    }

    public long getTotalBadDeviceRight() {
        return totalBadDeviceRight;
    }

    public void setTotalBadDeviceRight(long totalBadDeviceRight) {
        this.totalBadDeviceRight = totalBadDeviceRight;
    }

    public long getTotalInDriver() {
        return totalInDriver;
    }

    public void setTotalInDriver(long totalInDriver) {
        this.totalInDriver = totalInDriver;
    }

    public long getTotalQueuedToProcess() {
        return totalQueuedToProcess;
    }

    public void setTotalQueuedToProcess(long totalQueuedToProcess) {
        this.totalQueuedToProcess = totalQueuedToProcess;
    }

    public long getTotalBillingRefused() {
        return totalBillingRefused;
    }

    public void setTotalBillingRefused(long totalBillingRefused) {
        this.totalBillingRefused = totalBillingRefused;
    }
}
