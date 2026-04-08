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
package com.disk91.files.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value = {"file:configuration/files.properties"}, ignoreResourceNotFound = true)
public class FilesConfig {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    // ----------------------------------------------
    // Storage setup
    // ----------------------------------------------

    /** Root directory on the local filesystem where uploaded files are stored */
    @Value("${files.storage.path:/var/iotower/files}")
    protected String storageRootPath;
    public String getStorageRootPath() {
        return storageRootPath;
    }

    // ----------------------------------------------
    // Image processing
    // ----------------------------------------------

    /** Maximum allowed dimension (width or height, whichever is larger) in pixels for uploaded images.
     *  Images exceeding this value are proportionally resized. 0 means no limit. */
    @Value("${files.image.max.pixels:1280}")
    protected int imageMaxPixels;
    public int getImageMaxPixels() {
        return imageMaxPixels;
    }

    /** Maximum dimension (width or height) of the generated thumbnail in pixels */
    @Value("${files.image.thumbnail.pixels:256}")
    protected int imageThumbnailPixels;
    public int getImageThumbnailPixels() {
        return imageThumbnailPixels;
    }

    // ----------------------------------------------
    // Quota management
    // ----------------------------------------------

    /** Maximum number of files a single user may own simultaneously. 0 means no limit. */
    @Value("${files.quota.max.files.per.user:1000}")
    protected int quotaMaxFilesPerUser;
    public int getQuotaMaxFilesPerUser() {
        return quotaMaxFilesPerUser;
    }

    /** Maximum cumulative storage size in bytes for all files owned by a single user. 0 means no limit. */
    @Value("${files.quota.max.total.bytes.per.user:209715200}")
    protected long quotaMaxTotalBytesPerUser;
    public long getQuotaMaxTotalBytesPerUser() {
        return quotaMaxTotalBytesPerUser;
    }

    /** Maximum size in bytes allowed for a single uploaded file. 0 means no limit. */
    @Value("${files.quota.max.file.bytes:6291456}")
    protected long quotaMaxFileBytes;
    public long getQuotaMaxFileBytes() {
        return quotaMaxFileBytes;
    }

    // ----------------------------------------------
    // Integrity signature
    // ----------------------------------------------

    /** Secret key used to compute file SHA-256 integrity signatures. Should be a long random string. */
    @Value("${files.signature.secret:4b43f30f6a457e7a1db638d02a69a4c49360b81022dac51843fef9b8f4273379}")
    protected String signatureSecret;
    public String getSignatureSecret() {
        return signatureSecret;
    }

    /** Minimum time in milliseconds between two consecutive signature verifications for the same file.
     *  0 enforces a verification on every download. */
    @Value("${files.signature.check.interval.ms:60000}") // default 1 hour
    protected long signatureCheckIntervalMs;
    public long getSignatureCheckIntervalMs() {
        return signatureCheckIntervalMs;
    }

    // ----------------------------------------------
    // Integration
    // ----------------------------------------------

    @Value("${files.integration.medium:memory}")
    protected String integrationMedium;
    public String getIntegrationMedium() {
        return integrationMedium;
    }

    @Value("${files.integration.timeout.ms:10000}")
    protected long integrationTimeoutMs;
    public long getIntegrationTimeoutMs() {
        return integrationTimeoutMs;
    }

    // ----------------------------------------------
    // File cache
    // ----------------------------------------------

    /** Maximum number of FileStored entries kept in memory. 0 disables the cache. */
    @Value("${files.cache.max.size:500}")
    protected int fileCacheMaxSize;
    public int getFileCacheMaxSize() {
        return fileCacheMaxSize;
    }

    /** Time-to-live in seconds for a cached FileStored entry. */
    @Value("${files.cache.expiration_s:0}")
    protected long fileCacheExpiration;
    public long getFileCacheExpiration() {
        return fileCacheExpiration;
    }

    /** ISO-8601 duration between two cache stat log outputs (e.g. PT1H, PT24H). */
    @Value("${files.cache.log.period:PT24H}")
    protected String fileCacheLogPeriod;
    public String getFileCacheLogPeriod() {
        return fileCacheLogPeriod;
    }

}
