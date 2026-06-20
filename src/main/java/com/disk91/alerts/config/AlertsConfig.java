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
package com.disk91.alerts.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@PropertySource(value = {"file:configuration/alerts.properties"}, ignoreResourceNotFound = true)
public class AlertsConfig {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    // ----------------------------------------------
    // Alert template cache
    // ----------------------------------------------

    /** Maximum number of AlertTemplate entries kept in memory. 0 disables the cache. */
    @Value("${alerts.template.cache.max.size:200}")
    protected int templateCacheMaxSize;
    public int getTemplateCacheMaxSize() {
        return templateCacheMaxSize;
    }

    /** Time-to-live in seconds for a cached AlertTemplate entry. 0 means entries never expire. */
    @Value("${alerts.template.cache.expiration_s:0}")
    protected long templateCacheExpiration;
    public long getTemplateCacheExpiration() {
        return templateCacheExpiration;
    }

    /** ISO-8601 duration between two cache stat log outputs (e.g. PT1H, PT24H). */
    @Value("${alerts.template.cache.log.period:PT24H}")
    protected String templateCacheLogPeriod;
    public String getTemplateCacheLogPeriod() {
        return templateCacheLogPeriod;
    }

    // ----------------------------------------------
    // Alert lifecycle
    // ----------------------------------------------

    /** Maximum age in milliseconds for ENDED alerts before they are purged. Default: 7 days. */
    @Value("${alerts.max.retention.ms:604800000}")
    protected long alertsMaxRetentionMs;
    public long getAlertsMaxRetentionMs() {
        return alertsMaxRetentionMs;
    }

    /** Maximum number of alert processing tasks running in parallel. Default: 5. */
    @Value("${alerts.max.parallel.processing:5}")
    protected int alertsMaxParallelProcessing;
    public int getAlertsMaxParallelProcessing() {
        return alertsMaxParallelProcessing;
    }

    // ----------------------------------------------
    // Alert worker thread pool
    // ----------------------------------------------

    /**
     * Spring-managed thread pool for alert worker threads.
     * Pool size is fixed to alerts.max.parallel.processing.
     * Waits up to 15 seconds for in-flight tasks on shutdown.
     */
    @Bean(name = "alertTaskExecutor")
    public ThreadPoolTaskExecutor alertTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(alertsMaxParallelProcessing);
        executor.setMaxPoolSize(alertsMaxParallelProcessing);
        executor.setThreadNamePrefix("alert-worker-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(15);
        executor.initialize();
        return executor;
    }

}

