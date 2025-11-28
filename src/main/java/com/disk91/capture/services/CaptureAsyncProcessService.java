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
package com.disk91.capture.services;

import com.disk91.capture.config.CaptureConfig;
import com.disk91.capture.interfaces.AbstractProtocol;
import com.disk91.capture.interfaces.CaptureDataPivot;
import com.disk91.common.tools.exceptions.ITOverQuotaException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class CaptureAsyncProcessService {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected CaptureConfig captureConfig;

    @Autowired
    protected CaptureEndpointCache captureEndpointCache;

    @Autowired
    protected CaptureProtocolsCache captureProtocolsCache;

    @Autowired(required = false)
    private AutowireCapableBeanFactory beanFactory;

    // Cache the protocols class to avoid recreation each time
    protected HashMap<String, AbstractProtocol> protocolCache = new HashMap<>();

    protected ConcurrentLinkedQueue<CaptureDataPivot> rawQueue = new ConcurrentLinkedQueue<>();
    protected AtomicInteger rawQueueEstimatedSize = new AtomicInteger(0);

    /**
     * Async enqueuing of raw data for a later processing, we monitor the queue size and raise alarm when becoming too high
     * even if we can't really solve this out of dropping after a certain threshold. See the capture configuration for tuning
     *
     * We will also handle the saving of raw data in this section, either synchronously or asynchronously
     * depending on the configuration.
     *
     * @param pivot - pivot object to enqueue / process
     * @throws ITOverQuotaException - when the system is overloaded or closing, this is used to have a dedicated response
     *                               to the sender
     */
    public void enqueueRawData(CaptureDataPivot pivot) throws ITOverQuotaException {
        if ( captureConfig.getCaptureProcessorThreadsCount() > 0 ) {
            if ( running.get() ) {
                this.rawQueue.add(pivot);
                int estimatedSize = this.rawQueueEstimatedSize.incrementAndGet();
            } else {
                // service is closing report
                throw new ITOverQuotaException("capture-async-service-closing");
            }
        } else {
            // process directly (sync)
            processPivot(pivot);
        }
    }

    /**
     * Process a single pivot (parallelizable), this function is called by the async workers
     *
     * @param pivot - pivot object to process
     */
    private void processPivot(CaptureDataPivot pivot) {
        // Minimal action : log
        log.debug("Processing pivot: {}", pivot);
    }


    // ================================================================================
    // Manage async workers
    // ================================================================================

    /**
     * Async worker
     */
    private void workerLoop() {
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            CaptureDataPivot pivot = rawQueue.poll();
            if (pivot == null) {
                try {
                    Thread.sleep(50); // backoff when queue empty
                } catch (InterruptedException ignored) {}
                continue;
            }
            rawQueueEstimatedSize.decrementAndGet();
            try {
                processPivot(pivot);
            } catch (Exception ex) {
                // bypass the errors
                log.debug("[capture] Error processing pivot {}", ex.getMessage());
            }
        }
    }


    /**
     * The processing of frames will be done asynchronously and can be parallelized across multiple
     * threads because this processing may take a long time.
     */

    private ExecutorService workers;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger workerNameCounter = new AtomicInteger(0);

    @PostConstruct
    private void startWorkers() {
        if (captureConfig.getCaptureProcessorThreadsCount() > 0) {
            running.set(true);
            workers = Executors.newFixedThreadPool(captureConfig.getCaptureProcessorThreadsCount(), r -> {
                Thread t = new Thread(r);
                t.setName("capture-async-worker-" + workerNameCounter.incrementAndGet());
                t.setDaemon(true);
                return t;
            });
            for (int i = 0; i < captureConfig.getCaptureProcessorThreadsCount(); i++) {
                workers.submit(this::workerLoop);
            }
            log.info("[capture] Started {} capture async workers", captureConfig.getCaptureProcessorThreadsCount());
        }
    }

    @PreDestroy
    private void stopWorkers() {
        running.set(false);
        if (workers != null) {
            workers.shutdown();
            try {
                if (!workers.awaitTermination(30, TimeUnit.SECONDS)) {
                    workers.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                workers.shutdownNow();
            }
        }
        log.info("[capture] Stopped capture async workers");
    }

}
