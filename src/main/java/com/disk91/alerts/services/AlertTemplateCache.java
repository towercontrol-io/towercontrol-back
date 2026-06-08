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
package com.disk91.alerts.services;

import com.disk91.alerts.config.AlertsConfig;
import com.disk91.alerts.mdb.entities.AlertTemplate;
import com.disk91.alerts.mdb.repositories.AlertTemplateRepository;
import com.disk91.common.tools.Now;
import com.disk91.common.tools.ObjectCache;
import com.disk91.common.tools.exceptions.ITNotFoundException;
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
import java.util.List;
import java.util.Optional;

@Service
public class AlertTemplateCache {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected AlertsConfig alertsConfig;

    @Autowired
    protected AlertTemplateRepository alertTemplateRepository;

    @Autowired
    protected MeterRegistry meterRegistry;

    // ================================================================================================================
    // ALERT TEMPLATE CACHE SERVICE
    // ================================================================================================================

    // In-memory cache keyed by AlertTemplate id
    private ObjectCache<String, AlertTemplate> templateCache;

    protected boolean serviceEnable = false;

    /**
     * Initialize the alert template cache at application startup.
     * Registers Prometheus gauges for cache monitoring.
     */
    @PostConstruct
    private void initAlertTemplateCache() {
        log.info("[alerts] initAlertTemplateCache");
        if (alertsConfig.getTemplateCacheMaxSize() > 0) {
            this.templateCache = new ObjectCache<>(
                    "AlertTemplateCache",
                    alertsConfig.getTemplateCacheMaxSize(),
                    alertsConfig.getTemplateCacheExpiration() * 1000L
            ) {
                @Override
                synchronized public void onCacheRemoval(String key, AlertTemplate obj, boolean batch, boolean last) {
                    // Read-only cache: no write-back on eviction
                }

                @Override
                public void bulkCacheUpdate(List<AlertTemplate> objects) {
                    // Read-only cache: no bulk update needed
                }
            };

            Gauge.builder("alerts_template_cache_sum_time", this.templateCache.getTotalCacheTime())
                    .description("[Alerts] total time cache execution")
                    .register(meterRegistry);
            Gauge.builder("alerts_template_cache_sum", this.templateCache.getTotalCacheTry())
                    .description("[Alerts] total cache try")
                    .register(meterRegistry);
            Gauge.builder("alerts_template_cache_miss", this.templateCache.getCacheMissStat())
                    .description("[Alerts] total cache miss")
                    .register(meterRegistry);
        }

        this.serviceEnable = true;
    }

    /**
     * Gracefully stop the cache at application shutdown.
     */
    @PreDestroy
    public void destroy() {
        log.info("[alerts] AlertTemplateCache stopping");
        this.serviceEnable = false;
        if (alertsConfig.getTemplateCacheMaxSize() > 0) {
            templateCache.deleteCache();
        }
        log.info("[alerts] AlertTemplateCache stopped");
    }

    /**
     * Periodically log cache statistics according to configured period.
     * Skips logging when period >= 24h or cache is disabled.
     */
    @Scheduled(fixedRateString = "${alerts.template.cache.log.period:PT24H}", initialDelay = 3600_000)
    protected void alertTemplateCacheStatus() {
        try {
            Duration duration = Duration.parse(alertsConfig.getTemplateCacheLogPeriod());
            if (duration.toMillis() >= Now.ONE_FULL_DAY) return;
        } catch (Exception ignored) {}
        if (!this.serviceEnable || alertsConfig.getTemplateCacheMaxSize() == 0) return;
        this.templateCache.log();
    }

    // ================================================================================================================
    // Cache access
    // ================================================================================================================

    /**
     * Retrieve an AlertTemplate by its id, using the cache when enabled.
     * Always returns a clone to prevent callers from mutating the cached instance.
     * @param id - MongoDB document id of the AlertTemplate
     * @return a clone of the matching AlertTemplate
     * @throws ITNotFoundException when no template with that id exists
     */
    public AlertTemplate getTemplate(String id) throws ITNotFoundException {
        if (!this.serviceEnable || alertsConfig.getTemplateCacheMaxSize() == 0) {
            // Direct database access when cache is disabled
            Optional<AlertTemplate> t = alertTemplateRepository.findById(id);
            if (t.isEmpty()) throw new ITNotFoundException("alerts-template-not-found");
            return t.get().clone();
        }

        AlertTemplate t = this.templateCache.get(id);
        if (t == null) {
            // Cache miss: load from database and populate cache
            Optional<AlertTemplate> opt = alertTemplateRepository.findById(id);
            if (opt.isEmpty()) throw new ITNotFoundException("alerts-template-not-found");
            t = opt.get();
            this.templateCache.put(t, t.getId());
        }
        return t.clone();
    }

    /**
     * Evict a stale AlertTemplate entry from the cache (e.g. after an update or deletion).
     * @param id - MongoDB document id of the template to evict
     */
    public void flushTemplate(String id) {
        if (this.serviceEnable && alertsConfig.getTemplateCacheMaxSize() > 0) {
            this.templateCache.remove(id, false);
            log.debug("[alerts] Cache flushed for template {}", id);
        }
    }

    /**
     * Persist an AlertTemplate update and evict the stale entry from the cache.
     * The next read will reload the fresh record from the database.
     * @param template - updated AlertTemplate entity to persist
     */
    public void saveTemplate(AlertTemplate template) {
        alertTemplateRepository.save(template);
        this.flushTemplate(template.getId());
    }

}


