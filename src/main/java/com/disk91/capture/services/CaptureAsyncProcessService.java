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

import com.disk91.audit.integration.AuditIntegration;
import com.disk91.capture.config.ActionCatalog;
import com.disk91.capture.config.CaptureConfig;
import com.disk91.capture.interfaces.AbstractProcessor;
import com.disk91.capture.interfaces.CaptureDataPivot;
import com.disk91.capture.mdb.entities.CapturePivotRaw;
import com.disk91.capture.mdb.repositories.CapturePivotRawRepository;
import com.disk91.common.config.ModuleCatalog;
import com.disk91.common.tools.Now;
import com.disk91.common.tools.exceptions.ITOverQuotaException;
import com.mongodb.WriteConcern;
import com.mongodb.bulk.BulkWriteResult;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

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

    @Autowired
    protected AuditIntegration auditIntegration;

    protected class EnQueuedDataPivot {
        public CaptureDataPivot pivot;
        public long enqueueTime;

        public EnQueuedDataPivot(CaptureDataPivot pivot) {
            this.pivot = pivot;
            this.enqueueTime = Now.NowUtcMs();
        }
    }


    protected ConcurrentLinkedQueue<EnQueuedDataPivot> rawQueue = new ConcurrentLinkedQueue<>();
    protected AtomicInteger rawQueueEstimatedSize = new AtomicInteger(0);
    protected AtomicInteger rawLostDataCount = new AtomicInteger(0);

    protected long lastLog = 0;

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
        this.incrementEnqueueRequests();
        if ( captureConfig.getCaptureProcessorThreadsCount() > 0 ) {
            if ( running.get() ) {
                // Warn in case of high queue size
                if ( this.rawQueueEstimatedSize.get() >= captureConfig.getCaptureAsyncQueueWarningThreshold() ) {
                    if ( (Now.NowUtcMs() - lastLog) > 30_000 ) {
                        log.warn("[capture] Async processing queue size is high: {}", this.rawQueueEstimatedSize.get());
                        lastLog = Now.NowUtcMs();
                    }
                }
                // Stop in case of full queue
                if ( this.rawQueueEstimatedSize.get() >= captureConfig.getCaptureAsyncQueueMaxSize() ) {
                    rawLostDataCount.incrementAndGet();
                    if ( (Now.NowUtcMs() - lastLog) > 30_000 ) {
                        log.error("[capture] Async processing queue size exceeded maximum limit: {}", this.rawQueueEstimatedSize.get());
                        auditIntegration.auditLog(
                                ModuleCatalog.Modules.CAPTURE,
                                ActionCatalog.getActionName(ActionCatalog.Actions.INGEST_QUEUE_FULL),
                                pivot.getIngestOwnerId(),
                                "Loss of data due to overloaded async processing queue size {1}",
                                new String[]{""+rawLostDataCount.get()}
                        );
                        lastLog = Now.NowUtcMs();
                    }
                    this.incrementEnqueueRefused();
                    throw new ITOverQuotaException("capture-async-service-overloaded");
                } else {
                    // Normal case, enqueue
                    // Before we check if we have to reset the lost data counter with a 10% threshold
                    if ( rawLostDataCount.get() > 0 && this.rawQueueEstimatedSize.get() < (90*captureConfig.getCaptureAsyncQueueMaxSize())/100 ) {
                        // reset counter
                        auditIntegration.auditLog(
                                ModuleCatalog.Modules.CAPTURE,
                                ActionCatalog.getActionName(ActionCatalog.Actions.INGEST_SERVICE_RESTORED),
                                "system",
                                "Data ingestion restored after loss of {1} data items due to overloaded async processing queue",
                                new String[]{""+rawLostDataCount.get()}
                        );
                        rawLostDataCount.set(0);
                    }
                    EnQueuedDataPivot e = new EnQueuedDataPivot(pivot);
                    this.rawQueue.add(e);
                    this.rawQueueEstimatedSize.incrementAndGet();
                    this.incrementEnqueueSuccess();
                }
            } else {
                // service is closing report
                this.incrementEnqueueRejected();
                throw new ITOverQuotaException("capture-async-service-closing");
            }
        } else {
            // process directly (sync)
            this.incrementEnqueueSuccess();
            processPivot(pivot);
        }
    }

    // ================================================================================================
    // Process a single pivot
    // ================================================================================================

    // Cache the protocols class to avoid recreation each time
    protected final HashMap<String, AbstractProcessor> processorCache = new HashMap<>();

    /**
     * Process a single pivot (parallelizable), this function is called by the async workers
     *
     * @param pivot - pivot object to process
     */
    private void processPivot(CaptureDataPivot pivot) {
        // Store the raw data (sync or async)
        storeRawPivot(pivot);

        // Minimal action : log
        long start = Now.NowUtcMs();
        try {
            AbstractProcessor ap = processorCache.get(pivot.getProcessingChainClass());
            if (ap == null) {
                synchronized (processorCache) {
                    // Manage async call, block on cache and when released make sure another thread did not create it in the meantime
                    ap = processorCache.get(pivot.getProcessingChainClass());
                    if (ap == null) {
                        try {
                            Class<?> clazz = Class.forName(pivot.getProcessingChainClass());
                            ap = (AbstractProcessor) beanFactory.createBean(clazz);
                            processorCache.put(pivot.getProcessingChainClass(), ap);
                        } catch (Exception ex) {
                            log.error("[capture] Process data failed, processor class {} instantiation error for pivot Id {}", pivot.getProcessingChainClass(), pivot.getRxUuid());
                            return;
                        }
                    }
                }
            }
            ap.getClass()
                        .getMethod("process", CaptureDataPivot.class)
                        .invoke(ap, pivot);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException x) {
            log.debug("[capture] Process data failed, processor class {} reason {}", pivot.getProcessingChainClass(), x.getMessage());
        } finally {
            this.addProcessTime(Now.NowUtcMs() - start);
        }
    }


    // ================================================================================
    // Manage async workers
    // ================================================================================

    /**
     * Async worker
     */
    private void workerLoop() {
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            EnQueuedDataPivot pivot = rawQueue.poll();
            if (pivot == null) {
                Now.sleep(50);
                continue;
            }
            rawQueueEstimatedSize.decrementAndGet();
            try {
                this.addQueueLatency(Now.NowUtcMs() - pivot.enqueueTime);
                processPivot(pivot.pivot);
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
        log.info("[capture] Capture workers stopping");
        running.set(false);      // stop enqueuing new data
        int maxWaitSeconds = 60;
        while ( rawQueueEstimatedSize.get() > 0 && maxWaitSeconds > 0 ) {
            Now.sleep(1_000);
            maxWaitSeconds--;
        }
        if ( maxWaitSeconds == 0 ) log.warn("[capture] Capture workers time out (1)");
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
        // Run the final flush of the store queue
        forceFlushStore = true;
        maxWaitSeconds = 60;
        while ( !flushedStore && maxWaitSeconds > 0 ) {
            Now.sleep(1_000);
            maxWaitSeconds--;
        }
        if ( maxWaitSeconds == 0 ) log.warn("[capture] Capture workers time out (2)");
        log.info("[capture] Stopped capture async workers");
    }

    // ================================================================================================
    // Manage the Raw data storage in database
    //
    // Raw storage can be performed synchronously or asynchronously (but not both — that doesn't make sense).
    // If both are enabled, synchronous mode takes precedence.
    // In synchronous mode raw data is written on each submission, whereas asynchronous mode
    // queues data to be stored and then processes writes in batches,
    // triggered by a maximum batch size or a timeout so writes are done in batch rather than individually
    // for better performance.
    // On shutdown, ensure the queue is flushed; otherwise raw data may be lost.

    @Autowired
    protected CapturePivotRawRepository capturePivotRawRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    protected ConcurrentLinkedQueue<CaptureDataPivot> storeQueue = new ConcurrentLinkedQueue<>();
    protected AtomicInteger storeQueueEstimatedSize = new AtomicInteger(0);

    protected void storeRawPivot(CaptureDataPivot pivot) {
        if ( captureConfig.isCaptureRawStoreSync() ) {
            // Synchronous storage
            long start = Now.NowUtcMs();
            try {
                CapturePivotRaw r = new CapturePivotRaw();
                org.springframework.beans.BeanUtils.copyProperties(pivot,r);
                capturePivotRawRepository.save(r);
            } catch (Exception e) {
                log.error("[capture] Error during synchronous raw store: {}", e.getMessage());
            } finally {
                this.addStorageTime(Now.NowUtcMs() - start);
                this.incrementStorageCount();
            }
        } else if ( captureConfig.isCaptureRawStoreAsync() ) {
            // Asynchronous storage
            this.storeQueue.add(pivot);
            this.storeQueueEstimatedSize.incrementAndGet();
        }
    }

    protected long lastProcessStore = Now.NowUtcMs();
    protected boolean forceFlushStore = false;
    protected boolean flushedStore = false;

    @Scheduled(fixedDelay = 100)
    protected void processStore() {
        if ( captureConfig.isCaptureRawStoreAsync() ) {
            if (    this.storeQueueEstimatedSize.get() > captureConfig.getCaptureRawStoreAsyncBatchSize()
                 || (Now.NowUtcMs() - lastProcessStore) > captureConfig.getCaptureRawStoreAsyncBatchTimeout()
                 || forceFlushStore
            ) {
                try {
                    // trigger for processing, we push all pending
                    long start = Now.NowUtcMs();
                    mongoTemplate.setWriteConcern(WriteConcern.W1.withJournal(false));
                    BulkOperations bulkInsert = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, CapturePivotRaw.class);
                    CaptureDataPivot p = storeQueue.poll();
                    while (p != null) {
                        storeQueueEstimatedSize.decrementAndGet();
                        bulkInsert.insert(p);
                        p = storeQueue.poll();
                    }
                    BulkWriteResult bulkWriteResult = bulkInsert.execute();
                    long duration = Now.NowUtcMs() - start;
                    log.debug("[capture] Asynchronous raw store batch completed: inserted={}, duration {}", bulkWriteResult.getInsertedCount(), duration );
                    this.incrementStorageCount();
                    this.addStorageTime(duration);
                    if ( forceFlushStore ) flushedStore = true;
                } catch (Exception e) {
                    log.error("[capture] Error during asynchronous raw store batch: {}", e.getMessage());
                }
            }
        }
    }

    // ================================================================================================
    // Metrics
    //
    // - Total ingest requests
    // - Total enqueue successful
    // - Total enqueue rejected
    // - Total enqueue refused (overloaded)
    // - Total queue latency (to be divided by enqueue successful to have average)
    // - Total storage actions
    // - Total raw store time
    // - Total data process time

    @Autowired
    protected MeterRegistry meterRegistry;

    @PostConstruct
    private void initAsyncProcessService() {
        log.info("[capture] initAsyncProcessService initialized");

        Gauge.builder("capture_async_process_service_tot_enqueue", this.getEnqueueRequest())
                .description("[capture] Number of ingest requests to be enqueued from start")
                .register(meterRegistry);

        Gauge.builder("capture_async_process_tot_enqueue_success", this.getEnqueueSuccess())
                .description("[capture] Number of successful enqueued inject from start")
                .register(meterRegistry);

        Gauge.builder("capture_async_process_tot_enqueue_rejected", this.getEnqueueRejected())
                .description("[capture] Number of failed enqueuing from start")
                .register(meterRegistry);

        Gauge.builder("capture_async_process_tot_enqueue_refused", this.getEnqueueRefused())
                .description("[capture] Number of refused enqueuing due to overload from start")
                .register(meterRegistry);

        Gauge.builder("capture_async_process_tot_queue_latency", this.getQueueLatency())
                .description("[capture] Total time spent in queue until processing from start")
                .register(meterRegistry);

        Gauge.builder("capture_async_process_tot_raw_storage", this.getStorageCount())
                .description("[capture] Total storing raw data action from start")
                .register(meterRegistry);

        Gauge.builder("capture_async_process_tot_raw_storage_dur", this.getStorageTime())
                .description("[capture] Total time spent in storing raw data from start")
                .register(meterRegistry);

        Gauge.builder("capture_async_process_tot_process_dur", this.getProcessTime())
                .description("[capture] Total time spent in processing data from start")
                .register(meterRegistry);

    }

    protected AtomicLong enqueueRequests = new AtomicLong(0);
    public Supplier<Number> getEnqueueRequest() {
        return ()-> enqueueRequests.get();
    }
    public void incrementEnqueueRequests() {
        enqueueRequests.incrementAndGet();
    }

    protected AtomicLong enqueueSuccess = new AtomicLong(0);
    public Supplier<Number> getEnqueueSuccess() {
        return ()-> enqueueSuccess.get();
    }
    public void incrementEnqueueSuccess() {
        enqueueSuccess.incrementAndGet();
    }

    protected AtomicLong enqueueRejected = new AtomicLong(0);
    public Supplier<Number> getEnqueueRejected() {
        return ()-> enqueueRejected.get();
    }
    public void incrementEnqueueRejected() {
        enqueueRejected.incrementAndGet();
    }

    protected AtomicLong enqueueRefused = new AtomicLong(0);
    public Supplier<Number> getEnqueueRefused() {
        return ()-> enqueueRefused.get();
    }
    public void incrementEnqueueRefused() {
        enqueueRefused.incrementAndGet();
    }

    protected AtomicLong queueLatency = new AtomicLong(0);
    public Supplier<Number> getQueueLatency() {
        return ()->queueLatency.get();
    }
    public void addQueueLatency(long durationMs) {
        queueLatency.addAndGet(durationMs);
    }

    protected AtomicLong storageCount = new AtomicLong(0);
    public Supplier<Number> getStorageCount() {
        return ()-> storageCount.get();
    }
    public void incrementStorageCount() {
        storageCount.incrementAndGet();
    }

    protected AtomicLong storageTime = new AtomicLong(0);
    public Supplier<Number> getStorageTime() {
        return ()->storageTime.get();
    }
    public void addStorageTime(long durationMs) {
        storageTime.addAndGet(durationMs);
    }

    protected AtomicLong processTime = new AtomicLong(0);
    public Supplier<Number> getProcessTime() {
        return ()->processTime.get();
    }
    public void addProcessTime(long durationMs) {
        processTime.addAndGet(durationMs);
    }

}
