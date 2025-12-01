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
import com.disk91.capture.api.interfaces.CaptureResponseItf;
import com.disk91.capture.interfaces.AbstractProtocol;
import com.disk91.capture.interfaces.CaptureIngestResponse;
import com.disk91.capture.mdb.entities.CaptureEndpoint;
import com.disk91.capture.mdb.entities.Protocols;
import com.disk91.common.config.ModuleCatalog;
import com.disk91.common.tools.Now;
import com.disk91.common.tools.Tools;
import com.disk91.common.tools.exceptions.*;
import com.disk91.capture.config.ActionCatalog;
import com.disk91.users.mdb.entities.User;
import com.disk91.users.services.UserCommon;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

@Service
public class CaptureIngestService {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected CaptureEndpointCache captureEndpointCache;

    @Autowired
    protected CaptureProtocolsCache captureProtocolsCache;

    @Autowired
    protected CaptureAsyncProcessService captureAsyncProcessService;

    @Autowired
    protected UserCommon userCommon;

    @Autowired
    protected AuditIntegration auditIntegration;

    @Autowired(required = false)
    private AutowireCapableBeanFactory beanFactory;

    // Cache the protocols class to avoid recreation each time
    protected final HashMap<String, AbstractProtocol> protocolCache = new HashMap<>();


    /**
     * This generic endpoint allows ingesting data from any source; data are received as a string which in most cases
     * will be JSON and will be interpreted by the module adapted to the source. The parameter captureId indicates
     * which module should be used for the ingestion; at the same time we will verify that the user and the capture
     * module match and belong to the same person. This can be a technical (API) account or a regular user account
     * with an API key.
     *
     * @param req
     * @param body
     * @param captureId
     * @return
     * @throws ITParseException
     * @throws ITTooManyException
     * @throws ITNotFoundException
     * @throws ITRightException
     */
    public CaptureResponseItf ingestData(
            HttpServletRequest req,
            byte [] body,
            String captureId
    ) throws ITParseException, ITTooManyException, ITNotFoundException, ITRightException {

        long startTime = Now.NowUtcMs();
        try {
            // Get the endpoint information
            CaptureEndpoint e = captureEndpointCache.getCaptureEndpoint(captureId);
            // Check the ownership
            User u = userCommon.getUser(req.getUserPrincipal().getName());
            if ( !e.isWideOpen() && e.getOwner().compareTo(u.getLogin()) != 0) {
                log.debug("[capture] Ingest data failed, right error for captureId {} and user {}", captureId, u.getLogin());
                throw new ITRightException("capture-ingest-right-error");
            }

            // Right match, we can proceed
            try {
                Protocols p = captureProtocolsCache.getProtocol(e.getProtocolId());
                // Invoke the protocol ingestion
                AbstractProtocol ap = protocolCache.get(p.getProcessingClassName());
                if ( ap == null ) {
                    synchronized (protocolCache) {
                        // Manage async call, block on cache and when released make sure another thread did not create it in the meantime
                        ap = protocolCache.get(p.getProcessingClassName());
                        if ( ap == null ) {
                            try {
                                Class<?> clazz = Class.forName(p.getProcessingClassName());
                                ap = (AbstractProtocol) beanFactory.createBean(clazz);
                                protocolCache.put(p.getProcessingClassName(), ap);
                                incrementIngestProtocols();
                            } catch (Exception ex) {
                                log.debug("[capture] Ingest data failed, protocol class instantiation error for protocolId {}", e.getProtocolId());
                                throw new ITNotFoundException("capture-ingest-protocol-class-error");
                            }
                        }
                    }
                }

                try {
                    Object result = ap.getClass()
                            .getMethod("toPivot",
                                    User.class,
                                    CaptureEndpoint.class,
                                    Protocols.class,
                                    byte[].class,
                                    HttpServletRequest.class
                            )
                            .invoke(ap, u, e, p, body, req);
                    CaptureIngestResponse pivot = (CaptureIngestResponse) result;

                    // Input processed, the underlying level will manage the database saving
                    switch ( pivot.getPivot().getStatus() ) {
                        case CAP_STATUS_PARTIAL:
                        case CAP_STATUS_SUCCESS:
                            // Enqueue for processing
                            try {
                                captureAsyncProcessService.enqueueRawData(pivot.getPivot());
                                incrementIngestSuccess();
                            } catch (ITOverQuotaException x) {
                                try {
                                    // the system is shutting down or overloaded, this frame is rejected
                                    Object fallback = ap.getClass()
                                            .getMethod("fallbackResponse",
                                                    CaptureIngestResponse.class
                                            )
                                            .invoke(ap, pivot);
                                    CaptureResponseItf resp = (CaptureResponseItf) fallback;
                                    incrementIngestFailed();
                                    return resp;
                                } catch ( InvocationTargetException exp ) {
                                    // fallback
                                    Throwable _expect = x.getCause();
                                    if (_expect instanceof ITNotFoundException) {
                                        // protocol prefer to repost and error
                                        throw new ITTooManyException("capture-ingest-closing-or-overloaded");
                                    } else {
                                        incrementIngestFailed();
                                        if (canLog()) {
                                            log.error("[capture] Ingest fallback failed, we have an unexpected exception or for captureId {} protocolId {} from {}", captureId, e.getProtocolId(), Tools.getRemoteIp(req));
                                        }
                                        throw new ITParseException("capture-ingest-parse-error");
                                    }
                                }
                            }
                            break;
                        case CAP_STATUS_FAILURE:
                            incrementIngestFailed();
                            break;
                    }
                    return pivot.getResponse();

                } catch (NoSuchMethodException | IllegalAccessException x) {
                    incrementIngestFailed();
                    log.error("[capture] Ingest data failed, protocol class toPivot method missing for protocolId {}", e.getProtocolId());
                    throw new ITNotFoundException("capture-ingest-protocol-missing-method");
                } catch ( InvocationTargetException x ) {

                    Throwable _expect = x.getCause();

                    // Re-throw known business exceptions
                    if (_expect instanceof ITParseException) {
                        // We have identified a parsing issue, and we don't want to store this frame, it is malformed and
                        // rejected.
                        incrementIngestFailed();
                        if (canLog()) {
                            log.warn("[capture] Ingest data failed, parsing error for captureId {} protocolId {} from {}", captureId, e.getProtocolId(), Tools.getRemoteIp(req));
                        }
                        throw new ITParseException("capture-ingest-parse-error");
                    } else if (_expect instanceof ITHackerException) {
                        incrementIngestFailed();
                        incrementIngestHackingException();
                        if (canLog()) {
                            log.error("[capture] Ingest data failed, hacking detection error for captureId {} protocolId {} from {}", captureId, e.getProtocolId(), Tools.getRemoteIp(req));
                            auditIntegration.auditLog(
                                    ModuleCatalog.Modules.CAPTURE,
                                    ActionCatalog.getActionName(ActionCatalog.Actions.HACKING_DETECTED),
                                    u.getLogin(),
                                    "Hacking alert during ingest by {0} from {1} on captureId {2} protocolId {3}",
                                    new String[]{u.getLogin(), Tools.getRemoteIp(req), captureId, e.getProtocolId()}
                            );
                        }
                        throw new ITRightException("capture-ingest-right-error");
                    } else if (_expect instanceof ITRightException) {
                        // no logs (use debug for this)
                        incrementIngestFailed();
                        log.debug("[capture] Ingest data failed, right error for captureId {} protocolId {} from {}", captureId, e.getProtocolId(), Tools.getRemoteIp(req));
                        throw new ITRightException("capture-ingest-right-error");
                    } else {
                        // Unknown exception, log it
                        incrementIngestFailed();
                        if (canLog()) {
                            log.error("[capture] Ingest data failed, we have an unexpected exception or for captureId {} protocolId {} from {}", captureId, e.getProtocolId(), Tools.getRemoteIp(req));
                        }
                        throw new ITParseException("capture-ingest-parse-error");
                    }
                }

            } catch ( ITNotFoundException x ) {
                incrementIngestFailed();
                log.debug("[capture] Ingest data failed, unknown protocolId {}", e.getProtocolId());
                throw new ITNotFoundException("capture-ingest-protocol-unknown");
            }

        } catch (ITNotFoundException e) {
            incrementIngestFailed();
            log.debug("[capture] Ingest data failed, unknown captureId {}", captureId);
            throw new ITNotFoundException("capture-ingest-endpoint-unknown");
        } finally {
            // Metrics
            incrementIngestRequests();
            addIngestDuration(Now.NowUtcMs() - startTime);
        }
    }

    // ================================================================================================
    // Log Rate limiting (max 1 per 10 seconds / max batch 30 in 30 seconds)

    private AtomicInteger logLimit = new AtomicInteger(30);

    @Scheduled(fixedRate = 10_000)
    private void resetLogLimit() {
       logLimit.updateAndGet(v -> v < 30 ? v + 1 : v);
    }

    protected boolean canLog() {
        return ( logLimit.updateAndGet(v -> v > 0 ? v - 1 : v)  > 0 );
    }


    // ================================================================================================
    // Metrics
    //
    // - Total ingest requests
    // - Total successful ingests
    // - Total failed ingests
    // - Total inject time (track average time)
    // - Total protocol loaded
    // - Total HackingException detected

    @Autowired
    protected MeterRegistry meterRegistry;

    @PostConstruct
    private void initCaptureIngestService() {
        log.info("[capture] CaptureIngestService initialized");

        Gauge.builder("capture_ingest_service_tot_requests", this.getIngestRequest())
                .description("[capture] Number of ingest requests from start")
                .register(meterRegistry);

        Gauge.builder("capture_ingest_service_tot_success", this.getIngestSuccess())
                .description("[capture] Number of successful ingest from start")
                .register(meterRegistry);

        Gauge.builder("capture_ingest_service_tot_failed", this.getIngestFailed())
                .description("[capture] Number of failed ingest from start")
                .register(meterRegistry);

        Gauge.builder("capture_ingest_service_tot_duration_ms", this.getIngestDuration())
                .description("[capture] Total duration of ingest processing in ms from start")
                .register(meterRegistry);

        Gauge.builder("capture_ingest_service_tot_protocols", this.getIngestProtocols())
                .description("[capture] Number of different protocols ingested from start")
                .register(meterRegistry);

        Gauge.builder("capture_ingest_service_tot_hacking_exception", this.getIngestHackingException())
                .description("[capture] Number of hacking exception detected from start")
                .register(meterRegistry);

    }

    protected AtomicLong ingestRequests = new AtomicLong(0);
    public Supplier<Number> getIngestRequest() {
        return ()->ingestRequests.get();
    }
    public void incrementIngestRequests() {
        ingestRequests.incrementAndGet();
    }

    protected AtomicLong ingestSuccess = new AtomicLong(0);
    public Supplier<Number> getIngestSuccess() {
        return ()->ingestSuccess.get();
    }
    public void incrementIngestSuccess() {
        ingestSuccess.incrementAndGet();
    }

    protected AtomicLong ingestFailed = new AtomicLong(0);
    public Supplier<Number> getIngestFailed() {
        return ()->ingestFailed.get();
    }
    public void incrementIngestFailed() {
        ingestFailed.incrementAndGet();
    }

    protected AtomicLong ingestDuration = new AtomicLong(0);
    public Supplier<Number> getIngestDuration() {
        return ()->ingestDuration.get();
    }
    public void addIngestDuration(long durationMs) {
        ingestDuration.addAndGet(durationMs);
    }

    protected AtomicLong ingestProtocols = new AtomicLong(0);
    public Supplier<Number> getIngestProtocols() {
        return ()->ingestProtocols.get();
    }
    public void incrementIngestProtocols() {
        ingestProtocols.incrementAndGet();
    }

    protected AtomicLong ingestHackingException = new AtomicLong(0);
    public Supplier<Number> getIngestHackingException() {
        return () -> ingestHackingException.get();
    }
    public void incrementIngestHackingException() {
        ingestHackingException.incrementAndGet();
    }



}
