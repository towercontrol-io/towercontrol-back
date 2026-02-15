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
package com.disk91.integration.services;

import com.disk91.common.api.interfaces.ActionResult;
import com.disk91.common.config.CommonConfig;
import com.disk91.common.config.ModuleCatalog;
import com.disk91.common.tools.Now;
import com.disk91.common.tools.exceptions.ITOverQuotaException;
import com.disk91.common.tools.exceptions.ITParseException;
import com.disk91.common.tools.exceptions.ITTooManyException;
import com.disk91.integration.api.interfaces.IntegrationCallback;
import com.disk91.integration.api.interfaces.IntegrationQuery;
import com.disk91.integration.config.IntegrationConfig;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

@Service
public class IntegrationService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /*
     * There are several integration solutions between modules depending on the architecture; it can be
     * done via in-memory structures or a database. In the latter case we use an event store with an expiration
     * so messages are not kept for too long.
     * Each consumer will have a progress indicator for reading this structure. Each processed message
     * will have a processing counter allowing, for most messages, to determine whether they can be removed,
     * and a garbage-collector process will perform the cleanup.
     * The structure is a concurrent linked list handling concurrency or a table in a database.
     * In the case of using an MQTT/AMQP broker, the message will be published on an appropriate topic and the
     * broker will handle distribution.
     */
    private ConcurrentSkipListMap<Long, IntegrationQuery> eventStore;
    private AtomicLong eventId;
    private AtomicLong eventsInQueue;
    protected final AtomicBoolean running = new AtomicBoolean(false);

    private static final Object lock = new Object();

    // Autowired
    protected IntegrationConfig integrationConfig;
    protected CommonConfig commonConfig;

    @Autowired
    public IntegrationService(
        IntegrationConfig _integrationConfig,
        CommonConfig _commonConfig
    ) {
        log.info("[integration] Init");
        this.integrationConfig = _integrationConfig;
        this.commonConfig = _commonConfig;
        this.eventStore = new ConcurrentSkipListMap<>();
        // @TODO : load the previous data from database
        this.eventId = new AtomicLong(0);
        this.eventsInQueue = new AtomicLong(0);
    }

    @PostConstruct
    public void startRunners() {
        if ( this.integrationConfig.isIntegrationRouteMemoryEnabled() ) {
            log.info("[integration] Start inMemory Runners");
            this.startInMemoryWorkers(Math.max(1, this.integrationConfig.getIntegrationWorkersMaxCount()));
        }
    }


    /**
     * Stop workers and drain resources
     * Inputs: none
     * Outputs: none
     */
    @PreDestroy
    public void shutdown() {
        log.info("[integration] Integration workers stopping");
        if ( this.integrationConfig.isIntegrationRouteMemoryEnabled() ) {
            // @TODO : persist the event store to database for later processing
            stopInMemoryWorkers();
        }
        log.info("[integration] Integration workers stopped");
    }




    /**
     * Process the query depending on the mode and the route, not sure about the right
     * way to make it currently.... let see later, fire& forget is easy, it is just async
     * without any later consideration.
     * The routing depends on the available routing and the module preferences
     *
     * This function must run in an async context
     * @param query
     * @throws ITOverQuotaException - when the service is closed or the queue is full
     * @return
     */
    public IntegrationQuery processQuery(IntegrationQuery query) throws ITOverQuotaException {
        log.debug("[integration] Process query {}", query.getQueryId());

        if ( !this.running.get() ) query.setForLaterProcessing(true);
        // Check the getRoute requested vs available routes
        if (
                ( query.getRoute() == IntegrationQuery.QueryRoute.ROUTE_MEMORY && !this.integrationConfig.isIntegrationRouteMemoryEnabled() ) ||
                ( query.getRoute() == IntegrationQuery.QueryRoute.ROUTE_MQTT && !this.integrationConfig.isIntegrationRouteMqttEnabled() ) ||
                ( query.getRoute() == IntegrationQuery.QueryRoute.ROUTE_DB && !this.integrationConfig.isIntegrationRouteDbEnabled() )
        ) {
            // the requested route is not available, find a different one, use as favorite the one working in spread architecture
            if ( this.integrationConfig.isIntegrationRouteMqttEnabled() ) {
                query.setRoute(IntegrationQuery.QueryRoute.ROUTE_MQTT);
            } else if ( this.integrationConfig.isIntegrationRouteDbEnabled() ) {
                query.setRoute(IntegrationQuery.QueryRoute.ROUTE_DB);
            } else if ( this.integrationConfig.isIntegrationRouteMemoryEnabled() ) {
                query.setRoute(IntegrationQuery.QueryRoute.ROUTE_MEMORY);
            } else {
                // No solution
                log.error("[integration] No route available (all closed) for query {}", query.getQueryId());
                return query;
            }
        }

        // process depending route
        switch (query.getRoute()) {
            case ROUTE_MEMORY -> {
                switch (query.getType()) {
                    case TYPE_FIRE_AND_FORGET, TYPE_BROADCAST -> {
                        synchronized (lock) {
                            this.eventStore.put(this.eventId.incrementAndGet(), query);
                            this.eventsInQueue.incrementAndGet();
                        }
                        this.incrementInQueueRequests();
                        this.incrementIntegrationRequests();
                    }
                    case TYPE_ASYNC, TYPE_SYNC -> {
                        log.error("[integration] Query Type not yet implemented");
                        this.incrementIntegrationRequests();
                        this.incrementFailedRequests();
                    }

                }
            }
            case ROUTE_DB, ROUTE_MQTT -> {
                        log.error("[integration] Unknown route {} : not yet implemented", query.getRoute());
                        this.incrementIntegrationRequests();
                        this.incrementFailedRequests();
            }
        }
        return query;
    }

    // ===============================================================================
    //  CALL BACK REGISTRATIONS
    // ===============================================================================


    protected static class IntegrationSetup {
        public ModuleCatalog.Modules module;
        public IntegrationCallback callback;
        public long index;
    }

    protected HashMap<ModuleCatalog.Modules, IntegrationSetup> _callbacks = new HashMap<>();


    /**
     * Register a callback function to be called when a new query is available for the given service
     * @param service
     */
    public void registerCallback(ModuleCatalog.Modules service, IntegrationCallback callback) throws ITParseException, ITTooManyException {
        if ( callback == null ) throw new ITParseException("integration-callback-is-null");
        if ( this._callbacks.get(service) != null ) throw new ITTooManyException("integration-callback-already-registered");
        IntegrationSetup c = new IntegrationSetup();
        c.module = service;
        c.callback = callback;
        c.index = 0;
        this._callbacks.put(service, c);
    }

    // ================================================================================================
    // IN-MEMORY PENDING QUERIES MANAGEMENT
    // ================================================================================================
    protected final AtomicReference<ExecutorService> executorRef = new AtomicReference<>();

    /**
     * Start a fixed number of worker threads pulling from the queue
     * @param count - Number of workers
     */
    protected void startInMemoryWorkers(int count) {
        running.set(true);

        ExecutorService exec = Executors.newFixedThreadPool(
                count,
                r -> {
                    Thread t = new Thread(r);
                    t.setName("integration-worker-" + t.threadId());
                    t.setDaemon(true);
                    return t;
                }
        );
        executorRef.set(exec);

        for (int i = 0; i < count; i++) {
            exec.submit(this::inMemoryWorkerLoop);
        }
    }


    /**
     * Stop worker threads and shutdown executor
     */
    protected void stopInMemoryWorkers() {
        running.set(false);

        ExecutorService exec = executorRef.getAndSet(null);
        if (exec == null) return;

        // Interrupt workers blocked on queue.take()
        exec.shutdownNow();
        try {
            // Best-effort graceful stop
            exec.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ================================================================================================
    // WORKER LOOP FOR IN-MEMORY QUEUE
    // ================================================================================================

    private AtomicLong currentEventId = new AtomicLong(0);

    private synchronized IntegrationQuery getNextEvent() {
        if ( currentEventId.get() == 0 ) {
            try {
                currentEventId.set(eventStore.firstKey());
            } catch (NoSuchElementException x) {
                // empty store
                return null;
            }
        }
        IntegrationQuery evt = eventStore.get(currentEventId.get());
        if ( evt != null ) {
            currentEventId.getAndIncrement();
        }
        return evt;
    }

    /**
     * Worker loop: takes events and dispatches them to all callbacks
     */
    protected void inMemoryWorkerLoop() {
        long lastEventId = 0;
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                boolean pending = false;
                if ( integrationConfig.isIntegrationRouteMemoryEnabled() ) {
                    IntegrationQuery query = getNextEvent();
                    if ( query != null ) {
                        boolean get = false;
                        synchronized (lock) {
                            // let see if we can take it
                            if (query.getProcessAttempts() == 0 && !query.isForLaterProcessing() ) {
                                // In memory mode (local) broadcast is just 1 instance
                                query.setProcessAttempts(query.getProcessAttempts() + 1);
                                get = true;
                            }
                        }
                        if ( get ) {
                            // not yet took by another worker, process it.
                            if (    query.getServiceNameSource() == query.getServiceNameDest()
                                 && query.getSourceInstanceId().compareTo(commonConfig.getInstanceId()) ==0 ) {
                                // skip processing for self messages from local instance
                                query.setStateDone();
                                this.incrementProcessingCount();
                                this.incrementSkipRequests();
                            } else {
                                try {
                                    this._callbacks.get(query.getServiceNameDest()).callback.onIntegrationEvent(query);
                                    query.setStateDone();
                                    this.addProcessingTime(Now.NowUtcMs()-query.getQuery_ms());
                                    this.incrementProcessingCount();
                                    this.incrementSuccessRequests();
                                } catch (Exception x) {
                                    this.incrementFailedRequests();
                                    query.setStateError();
                                    query.setResponse(ActionResult.UNKNOWN(x.getMessage()));
                                }
                            }
                            pending = true; // not sure but probably more to process
                        }
                    }
                }
                // Now process the DB pending events

                // @TODO : DB Integration processing

                // Calm down when not busy
                if ( !pending ) Now.sleep(20);

            } catch (Exception e) {
                log.error("[integration] Worker loop failure", e);
            }
        }
    }


    /**
     * For debugging purpose, we get the ability to trace the queue content...
     */
    public void traceQueue() {
        if ( eventStore == null ) return;
        eventStore.forEach((key,element) -> {
            log.info("[integration] Elt ({}) from {} to {} with type {} and action {} having state {}",
                    key,
                    element.getServiceNameSource().name(),
                    element.getServiceNameDest().name(),
                    element.getType(),
                    element.getAction(),
                    element.getState()
            );
        });


    }

    // ================================================================================================
    // Garbage collector for in-memory pending queries
    //

    private long lastInMemoryGarbage = Now.NowUtcMs();
    private boolean forceInMemoryGarbageNext = false;

    /**
     * Ability to force garbage to run faster (mostly for tests)
     */
    public void forceInMemoryGarbage() {
        forceInMemoryGarbageNext = true;
    }

    /**
     * Clean the outdated pending queries every second
     */
    @Scheduled(fixedDelay = 1_000, initialDelay = 2_000)
    void inMemoryGarbage() {
        if ( eventStore == null ) return;
        if ( (Now.NowUtcMs() - lastInMemoryGarbage < 30_000) && !forceInMemoryGarbageNext ) return;
        lastInMemoryGarbage = Now.NowUtcMs();
        forceInMemoryGarbageNext = false;
        try {
            // remove all the queries that are too old and done with ID lower than the worker indexes
            long now = Now.NowUtcMs();
            long eventId = eventStore.firstKey();
            long _currentEventId = this.currentEventId.get();
            while (eventId < _currentEventId) {
                IntegrationQuery evt = eventStore.get(eventId);
                if (evt != null) {
                    if ((evt.getState() == IntegrationQuery.QueryState.STATE_DONE || evt.getState() == IntegrationQuery.QueryState.STATE_ERROR)
                            && !evt.isForLaterProcessing()  // don't clear these one we want to store in database on exit
                    ) {
                        // remove it
                        eventStore.remove(eventId);
                        this.eventsInQueue.decrementAndGet();
                        this.decrementInQueueRequests();
                    }
                }
                eventId++;
            }
        } catch (NoSuchElementException x) {
            // empty store, nothing to do.
        } catch (Exception e) {
            log.error("[integration] In-memory garbage collector failure ({})", e.getMessage());
        }
        // @TODO : not sure what to do with the timeout...  so let see later,
    }


    // ================================================================================================
    // Metrics
    //
    // - Total integration requests
    // - Total in queue requests
    // - Total success request
    // - Total skipped request ( local loop )
    // - Total failed request
    // - Total processing time
    // - Total processing count

    @Autowired
    protected MeterRegistry meterRegistry;


    @PostConstruct
    private void initIntegrationMetrics() {
        log.info("[integration] IntegrationService Metrics initialization");
        Gauge.builder("capture_integration_service_total_requests", this.getIntegrationRequest())
                .description("[capture] Number of integration requests from start")
                .register(meterRegistry);
        Gauge.builder("capture_integration_service_in_queue_requests", this.getInQueueRequests())
                .description("[capture] Number of requests currently waiting in queue")
                .register(meterRegistry);
        Gauge.builder("capture_integration_service_success_requests", this.getSuccessRequests())
                .description("[capture] Number of successfully processed requests")
                .register(meterRegistry);
        Gauge.builder("capture_integration_service_skip_requests", this.getSkipRequests())
                .description("[capture] Number of skipped requests (local loop)")
                .register(meterRegistry);
        Gauge.builder("capture_integration_service_failed_requests", this.getFailedRequests())
                .description("[capture] Number of failed requests")
                .register(meterRegistry);
        Gauge.builder("capture_integration_service_total_processing_time_ms", this.getTotalProcessingTimeMs())
                .description("[capture] Total processing time in milliseconds")
                .register(meterRegistry);
        Gauge.builder("capture_integration_service_processing_count", this.getProcessingCount())
                .description("[capture] Total number of processed requests")
                .register(meterRegistry);
    }


    protected AtomicLong integrationRequests = new AtomicLong(0);
    public Supplier<Number> getIntegrationRequest() {
        return ()->integrationRequests.get();
    }
    public void incrementIntegrationRequests() {
        integrationRequests.incrementAndGet();
    }

    protected AtomicLong inQueueRequests = new AtomicLong(0);
    public Supplier<Number> getInQueueRequests() {
        return () -> inQueueRequests.get();
    }
    public void incrementInQueueRequests() {
        inQueueRequests.incrementAndGet();
    }
    public void decrementInQueueRequests() {
        inQueueRequests.decrementAndGet();
    }

    protected AtomicLong successRequests = new AtomicLong(0);
    public Supplier<Number> getSuccessRequests() {
        return () -> successRequests.get();
    }
    public void incrementSuccessRequests() {
        successRequests.incrementAndGet();
    }

    protected AtomicLong skipRequests = new AtomicLong(0);
    public Supplier<Number> getSkipRequests() {
        return () -> skipRequests.get();
    }
    public void incrementSkipRequests() {
        skipRequests.incrementAndGet();
    }


    protected AtomicLong failedRequests = new AtomicLong(0);
    public Supplier<Number> getFailedRequests() {
        return () -> failedRequests.get();
    }
    public void incrementFailedRequests() {
        failedRequests.incrementAndGet();
    }

    protected AtomicLong totalProcessingTimeMs = new AtomicLong(0);
    public Supplier<Number> getTotalProcessingTimeMs() {
        return () -> totalProcessingTimeMs.get();
    }
    public void addProcessingTime(long durationMs) {
        totalProcessingTimeMs.addAndGet(durationMs);
    }

    protected AtomicLong processingCount = new AtomicLong(0);
    public Supplier<Number> getProcessingCount() {
        return () -> processingCount.get();
    }
    public void incrementProcessingCount() {
        processingCount.incrementAndGet();
    }


}
