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
package com.disk91.files.services;

import com.disk91.common.tools.Now;
import com.disk91.common.tools.ObjectCache;
import com.disk91.common.tools.exceptions.ITNotFoundException;
import com.disk91.files.config.FilesConfig;
import com.disk91.files.pdb.entities.FileStored;
import com.disk91.files.pdb.repositories.FileStoredRepository;
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
public class FileCache {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected FilesConfig filesConfig;

    @Autowired
    protected FileStoredRepository fileStoredRepository;

    @Autowired
    protected MeterRegistry meterRegistry;

    // ================================================================================================================
    // FILE CACHE SERVICE
    // ================================================================================================================

    // In-memory cache keyed by file id (UUID String)
    private ObjectCache<String, FileStored> fileCache;

    protected boolean serviceEnable = false;

    /**
     * Initialize the file cache at application startup.
     * Registers Prometheus gauges for cache monitoring.
     */
    @PostConstruct
    private void initFileCache() {
        log.info("[files] initFileCache");
        if (filesConfig.getFileCacheMaxSize() > 0) {
            this.fileCache = new ObjectCache<String, FileStored>(
                    "FileROCache",
                    filesConfig.getFileCacheMaxSize(),
                    filesConfig.getFileCacheExpiration() * 1000L
            ) {
                @Override
                synchronized public void onCacheRemoval(String key, FileStored obj, boolean batch, boolean last) {
                    // Read-only cache: no write-back on eviction
                }
                @Override
                public void bulkCacheUpdate(List<FileStored> objects) {
                    // Read-only cache: no bulk update needed
                }
            };
        }

        this.serviceEnable = true;

        Gauge.builder("files_service_cache_sum_time", this.fileCache.getTotalCacheTime())
                .description("[Files] total time cache execution")
                .register(meterRegistry);
        Gauge.builder("files_service_cache_sum", this.fileCache.getTotalCacheTry())
                .description("[Files] total cache try")
                .register(meterRegistry);
        Gauge.builder("files_service_cache_miss", this.fileCache.getCacheMissStat())
                .description("[Files] total cache miss")
                .register(meterRegistry);
    }

    /**
     * Gracefully stop the cache at application shutdown.
     */
    @PreDestroy
    public void destroy() {
        log.info("[files] FileCache stopping");
        this.serviceEnable = false;
        if (filesConfig.getFileCacheMaxSize() > 0) {
            fileCache.deleteCache();
        }
        log.info("[files] FileCache stopped");
    }

    /**
     * Periodically log cache statistics according to configured period.
     * Skips logging when period >= 24 h or cache is disabled.
     */
    @Scheduled(fixedRateString = "${files.cache.log.period:PT24H}", initialDelay = 3600_000)
    protected void fileCacheStatus() {
        try {
            Duration duration = Duration.parse(filesConfig.getFileCacheLogPeriod());
            if (duration.toMillis() >= Now.ONE_FULL_DAY) return;
        } catch (Exception ignored) {}
        if (!this.serviceEnable || filesConfig.getFileCacheMaxSize() == 0) return;
        this.fileCache.log();
    }

    // ================================================================================================================
    // Cache access
    // ================================================================================================================

    /**
     * Retrieve a FileStored by its uniqueName, using the cache when enabled.
     * Always returns a clone to prevent callers from mutating the cached instance.
     * The in-memory lastSignatureCheck field is preserved in the cached copy.
     * @param uniqueName - physical unique filename of the file
     * @return a clone of the matching FileStored
     * @throws ITNotFoundException when no file with that uniqueName exists
     */
    public FileStored getFile(String uniqueName) throws ITNotFoundException {
        if (!this.serviceEnable || filesConfig.getFileCacheMaxSize() == 0) {
            // Direct database access when cache is disabled
            Optional<FileStored> f = fileStoredRepository.findByUniqueName(uniqueName);
            if (f.isEmpty()) throw new ITNotFoundException("files-file-not-found");
            return f.get().clone();
        }

        FileStored f = this.fileCache.get(uniqueName);
        if (f == null) {
            // Cache miss: load from database and populate cache
            Optional<FileStored> opt = fileStoredRepository.findByUniqueName(uniqueName);
            if (opt.isEmpty()) throw new ITNotFoundException("files-file-not-found");
            f = opt.get();
            this.fileCache.put(f, f.getUniqueName());
        }
        return f.clone();
    }

    /**
     * Remove a FileStored entry from the local cache (e.g. after an update or deletion).
     * @param uniqueName - physical unique filename of the file to evict
     */
    public void flushFile(String uniqueName) {
        if (this.serviceEnable && filesConfig.getFileCacheMaxSize() > 0) {
            this.fileCache.remove(uniqueName, false);
            log.debug("[files] Cache flushed for file {}", uniqueName);
        }
    }

    /**
     * Persist a FileStored update and evict the stale entry from the cache.
     * The next read will reload the fresh record from the database.
     * @param file - updated FileStored entity to persist
     */
    public void saveFile(FileStored file) {
        fileStoredRepository.save(file);
        this.flushFile(file.getUniqueName());
    }

    /**
     * Update the in-memory lastSignatureCheck timestamp for a cached file without
     * triggering a database write. This avoids a DB round-trip on every download
     * when the signature interval has not elapsed.
     * If the file is not currently in cache, the update is silently ignored.
     * @param uniqueName - physical unique filename of the file
     */
    public void updateLastSignatureCheck(String uniqueName) {
        if (!this.serviceEnable || filesConfig.getFileCacheMaxSize() == 0) return;
        FileStored cached = this.fileCache.get(uniqueName);
        if (cached != null) {
            // Mutate the cached instance directly (in-memory only, not persisted)
            cached.setLastSignatureCheck(Now.NowUtcMs());
        }
    }

}

