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

package com.disk91.capture.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value = {"file:configuration/capture.properties"}, ignoreResourceNotFound = true)
public class CaptureConfig {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    // ----------------------------------------------
    // Ingestion config
    // ----------------------------------------------
    @Value("${capture.raw.store.sync:true}")
    protected boolean captureRawStoreSync;
    public boolean isCaptureRawStoreSync() {
        return captureRawStoreSync;
    }

    @Value("${capture.raw.store.async:true}")
    protected boolean captureRawStoreAsync;
    public boolean isCaptureRawStoreAsync() {
        return captureRawStoreAsync;
    }

    @Value("${capture.raw.store.async.batch.size:100}")
    protected int captureRawStoreAsyncBatchSize;
    public int getCaptureRawStoreAsyncBatchSize() {
        return captureRawStoreAsyncBatchSize;
    }

    @Value("${capture.raw.store.async.batch.timeout:60000}")
    protected int captureRawStoreAsyncBatchTimeout;
    public int getCaptureRawStoreAsyncBatchTimeout() {
        return captureRawStoreAsyncBatchTimeout;
    }

    @Value("${capture.processor.threads.count:1}")
    protected int captureProcessorThreadsCount;
    public int getCaptureProcessorThreadsCount() {
        return captureProcessorThreadsCount;
    }

    @Value("${capture.async.queue.warning.threshold:2000}")
    protected int captureAsyncQueueWarningThreshold;
    public int getCaptureAsyncQueueWarningThreshold() {
        return captureAsyncQueueWarningThreshold;
    }

    @Value("${capture.async.queue.max.size:5000}")
    protected int captureAsyncQueueMaxSize;
    public int getCaptureAsyncQueueMaxSize() {
        return captureAsyncQueueMaxSize;
    }

    // ----------------------------------------------
    // Common setup
    // ----------------------------------------------

    // Service Id used for multi-instance environment, must be different for every instances
    @Value("${common.service.id:83DZqvwbXzmtllVq}")
    protected String commonServiceId;
    public String getCommonServiceId() {
        return (commonServiceId.isEmpty())?"83DZqvwbXzmtllVq":commonServiceId;
    }

    // Select the medium to be used for the intracom service communication
    @Value("${capture.intracom.medium:db}")
    protected String captureIntracomMedium;
    public String getCaptureIntracomMedium() {
        return captureIntracomMedium;
    }


    // --------------------------------------------
    // Capture Endpoint Cache
    // --------------------------------------------

    @Value("${capture.endpoint.cache.max.size:500}")
    protected int captureEndpointCacheMaxSize;
    public int getCaptureEndpointCacheMaxSize() {
        return captureEndpointCacheMaxSize;
    }

    @Value("${capture.endpoint.cache.expiration_s:0}")
    protected int captureEndpointCacheExpiration;
    public int getCaptureEndpointCacheExpiration() {
        return captureEndpointCacheExpiration;
    }

    @Value("${capture.endpoint.cache.log.period:PT24H}")
    protected String captureEndpointCacheLogPeriod;
    public String getCaptureEndpointCacheLogPeriod() {
        return captureEndpointCacheLogPeriod;
    }

}
