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
import com.disk91.capture.mdb.entities.CaptureEndpoint;
import com.disk91.capture.mdb.repositories.CaptureEndpointRepository;
import com.disk91.common.config.CommonConfig;
import com.disk91.common.config.ModuleCatalog;
import com.disk91.common.tools.Now;
import com.disk91.common.tools.ObjectCache;
import com.disk91.common.tools.exceptions.ITNotFoundException;
import com.disk91.common.tools.exceptions.ITOverQuotaException;
import com.disk91.integration.api.interfaces.IntegrationQuery;
import com.disk91.integration.services.IntegrationService;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.disk91.capture.integration.CaptureActions.CAPTURE_ACTION_FLUSH_CACHE_ENDPOINT;
import static com.disk91.users.integration.UsersActions.USERS_ACTION_FLUSH_CACHE_USERS;

@Service
public class CaptureEndpointCache {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * CaptureEndpoint Cache Service is caching the Group Information. It may be instantiated in all the instances
     * and multiple cache should collaborate in a cluster
     */

    @Autowired
    protected CaptureConfig config;

    @Autowired
    protected CaptureEndpointRepository repository;

    @Autowired
    protected MeterRegistry meterRegistry;

    @Autowired
    protected CommonConfig commonConfig;

    @Autowired
    protected IntegrationService integrationService;

    // ================================================================================================================
    // CACHE SERVICE
    // ================================================================================================================

    private ObjectCache<String, CaptureEndpoint> cache;

    protected boolean serviceEnable = false;

    @PostConstruct
    private void initCaptureEndpointCache() {
        log.info("[capture] initCaptureEndpointCache");
        if ( config.getCaptureEndpointCacheMaxSize() > 0 ) {
            this.cache = new ObjectCache<String, CaptureEndpoint>(
                    "CaptureEndpointRWCache",
                    config.getCaptureEndpointCacheMaxSize(),
                    config.getCaptureEndpointCacheExpiration()*1000L
            ) {
                @Override
                synchronized public void onCacheRemoval(String key, CaptureEndpoint obj, boolean batch, boolean last) {
                    // Save the stats update
                    if ( obj != null ) {
                        repository.save(obj);
                    }
                }
                @Override
                public void bulkCacheUpdate(List<CaptureEndpoint> objects) {
                    // save the stats update in bulk
                    repository.saveAll(objects);
                }
            };
        }

        this.serviceEnable = true;

        Gauge.builder("capture_endpoint_service_cache_sum_time", this.cache.getTotalCacheTime())
                .description("[capture] total time cache execution")
                .register(meterRegistry);
        Gauge.builder("capture_endpoint_service_cache_sum", this.cache.getTotalCacheTry())
                .description("[capture] total cache try")
                .register(meterRegistry);
        Gauge.builder("capture_endpoint_service_cache_miss", this.cache.getCacheMissStat())
                .description("[capture] total cache miss")
                .register(meterRegistry);
    }

    @PreDestroy
    public void destroy() {
        log.info("[capture] CaptureEndpointCache stopping");
        this.serviceEnable = false;
        if ( config.getCaptureEndpointCacheMaxSize() > 0 ) {
            cache.deleteCache();
        }
        log.info("[capture] CaptureEndpointCache stopped");
    }

    @Scheduled(fixedRateString = "${capture.endpoint.cache.log.period:PT24H}", initialDelay = 3600_000)
    protected void cacheStatus() {
        try {
            Duration duration = Duration.parse(config.getCaptureEndpointCacheLogPeriod());
            if (duration.toMillis() >= Now.ONE_FULL_DAY ) return;
        } catch (Exception ignored) {}
        if ( ! this.serviceEnable || config.getCaptureEndpointCacheMaxSize() == 0 ) return;
        this.cache.log();
    }

    // ================================================================================================================
    // Cache access
    // ================================================================================================================

    /**
     * Get the capture endpoint from the cache or from the database if not in cache
     * No Object clone as we want to manage counters globally
     * @param id - id to be retrieved
     * @return the object
     * @throws ITNotFoundException if not found
     */
    public CaptureEndpoint getCaptureEndpoint(String id) throws ITNotFoundException {
        if (!this.serviceEnable || config.getCaptureEndpointCacheMaxSize() == 0) {
            // direct access from database
            CaptureEndpoint o = repository.findOneByRef(id);
            if (o == null) throw new ITNotFoundException("capture-endpoint-not-found");
            return o;
        } else {
            CaptureEndpoint o = this.cache.get(id);
            if (o == null) {
                // not in cache, get it from the database
                o = repository.findOneByRef(id);
                if (o == null) throw new ITNotFoundException("capture-endpoint-not-found");
                this.cache.put(o, o.getRef());
            }
            return o;
        }
    }

    /**
     * Remove a capture endpoint from the local cache if exists (this is when the object has been updated somewhere else)
     * @param id - capture endpoint to be removed
     * @return
     */
    public void flushCaptureEndpoint(String id) {
        if ( this.serviceEnable && config.getCaptureEndpointCacheMaxSize() > 0 ) {
            this.cache.remove(id,false);
        }

        // Broadcast other instances to flush their cache for this device
        IntegrationQuery iq = new IntegrationQuery(ModuleCatalog.Modules.CAPTURE, commonConfig.getInstanceId());
        iq.setServiceNameDest(ModuleCatalog.Modules.CAPTURE);
        iq.setType(IntegrationQuery.QueryType.TYPE_BROADCAST);
        iq.setAction(CAPTURE_ACTION_FLUSH_CACHE_ENDPOINT.ordinal());
        iq.setQuery(id);
        iq.setRoute(IntegrationQuery.getRoutefromRouteString(config.getCaptureIntracomMedium()));
        try {
            integrationService.processQuery(iq);
        } catch (ITOverQuotaException ignored) {}

    }

    /**
     * On regular basis (5 minutes), save the stats of all cached endpoints
     * Print the stats on logs at least until the front-end have the feature to display them
     * later pass it to debug level
     */
    @Scheduled(fixedRate = 300000, initialDelay = 5000)
    protected void printStatsAndSave() {
        if ( ! this.serviceEnable || config.getCaptureEndpointCacheMaxSize() == 0 ) return;
        for ( String key : Collections.list(this.cache.list()) ) {
            CaptureEndpoint ep = this.cache.get(key);
            if ( ep != null ) {
                log.info("[capture][endpoint-cache] Endpoint {} | RX {} | +PV {} | +DR {} | +PR {} | +QP {} | -BO {} | -BP {} | -BR {}",
                        ep.getRef(),
                        ep.getTotalFramesReceived(),
                        ep.getTotalFramesAcceptedToPivot(),
                        ep.getTotalInDriver(),
                        ep.getTotalFramesAcceptedToProcess(),
                        ep.getTotalQueuedToProcess(),
                        ep.getTotalBadOwnerRefused(),
                        ep.getTotalBadPayloadFormat(),
                        ep.getTotalBadDeviceRight()
                );
                repository.save(ep);
            }
        }
    }

}
