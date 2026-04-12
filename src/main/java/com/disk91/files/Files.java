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
package com.disk91.files;

import com.disk91.common.config.CommonConfig;
import com.disk91.common.pdb.repositories.ParamRepository;
import com.disk91.files.pdb.repositories.FileStoredRepository;
import com.disk91.integration.services.IntegrationService;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class Files {

    /**
     * This class init the Files specific components
     *
     */

    protected Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected ParamRepository paramRepository;

    @Autowired
    protected CommonConfig commonConfig;

    @Autowired
    protected IntegrationService integrationService;

    @Autowired
    protected FileStoredRepository fileStoredRepository;

    @PostConstruct
    void initFilesModule() {
        // Init the Capture Module
        log.info("[files] Files is starting");
    }

    // ================================================================================================================
    // Metrics
    // ================================================================================================================

    @Autowired
    protected MeterRegistry meterRegistry;

    // Stats periodically refreshed from the database by the scheduled task
    private volatile long statTotalFiles = 0;
    private volatile long statTotalSize = 0;
    private volatile long statTotalAccessCount = 0;

    // Real-time signature error counter - incremented on each signature verification failure
    private static long signatureErrors = 0;

    /**
     * Register Prometheus gauges for file service metrics at startup.
     */
    @PostConstruct
    private void initFileServiceMetrics() {
        log.info("[files] File service metrics initialized");
        Gauge.builder("files_service_total_files", () -> statTotalFiles)
                .description("Total number of files stored in the system")
                .register(meterRegistry);
        Gauge.builder("files_service_total_size_bytes", () -> statTotalSize)
                .description("Total size in bytes of all stored files")
                .register(meterRegistry);
        Gauge.builder("files_service_total_access_count", () -> statTotalAccessCount)
                .description("Total number of file download accesses across all files")
                .register(meterRegistry);
        Gauge.builder("files_service_signature_errors", () -> signatureErrors)
                .description("Number of file or thumbnail signature verification errors detected")
                .register(meterRegistry);
    }

    /**
     * Scheduled task refreshing file statistics from the database every 5 minutes.
     * Updates total file count, total size and total access count.
     */
    @Scheduled(fixedRate = 300_000, initialDelay = 60_000)
    protected void refreshFileStats() {
        try {
            statTotalFiles = fileStoredRepository.count();
            statTotalSize = fileStoredRepository.sumAllSizes();
            statTotalAccessCount = fileStoredRepository.sumAllAccessCount();
            log.debug("[files] File stats refreshed: totalFiles={} totalSize={} totalAccessCount={}",
                    statTotalFiles, statTotalSize, statTotalAccessCount);
        } catch (Exception e) {
            log.error("[files] Failed to refresh file statistics", e);
        }
    }

    /**
     * Increment the signature error counter in a thread-safe manner.
     * Called whenever a file or thumbnail integrity check fails.
     */
    public static synchronized void incSignatureErrors() {
        signatureErrors++;
    }

}
