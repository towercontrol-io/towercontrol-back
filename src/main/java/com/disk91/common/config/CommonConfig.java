/*
 * Copyright (c) - Paul Pinault (aka disk91) - 2024.
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

package com.disk91.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value = {"file:configuration/common.properties"}, ignoreResourceNotFound = true)
public class CommonConfig {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    // ----------------------------------------------
    // Common setup
    // ----------------------------------------------
    @Value("${common.encryption.key:d5b504d560363cfe33890c4f5343f387}")
    protected String encryptionKey;
    public String getEncryptionKey() {
        return encryptionKey;
    }

    @Value("${common.application.key:a84c2d1f7b9e063d5f1a2e9c3b7d408e}")
    protected String applicationKey;
    public String getApplicationKey() {
        return applicationKey;
    }

    @Value("${common.mail.sender:contact@foo.bar}")
    protected String commonMailSender;
    public String getCommonMailSender() {
        return commonMailSender;
    }

    @Value("${common.service.name:ITC}")
    protected String commonServiceName;
    public String getCommonServiceName() {
        return commonServiceName;
    }

    @Value("${common.service.back.domain:api.foo.bar}")
    protected String commonServiceBackDomain;
    public String getCommonServiceBackDomain() {
        return commonServiceBackDomain;
    }
    @Value("${common.service.front.domain:itc.foo.bar}")
    protected String commonServiceFrontDomain;
    public String getCommonServiceFrontDomain() {
        return commonServiceFrontDomain;
    }


    @Value("${common.service.back.baseurl:https://api.foo.bar}")
    protected String commonServiceBackBaseUrl;
    public String getCommonServiceBackBaseUrl() {
        return commonServiceBackBaseUrl;
    }
    @Value("${common.service.front.baseurl:https://itc.foo.bar}")
    protected String commonServiceFrontBaseUrl;
    public String getCommonServiceFrontBaseUrl() {
        return commonServiceFrontBaseUrl;
    }

    // Port as a num
    @Value("${common.service.back.port:-1}")
    protected int commonServiceBackPort;
    public int getCommonServiceBackPort() {
        return commonServiceBackPort;
    }
    @Value("${common.service.front.port:-1}")
    protected int commonServiceFrontPort;
    public int getCommonServiceFrontPort() {
        return commonServiceFrontPort;
    }

    // Compose and url for a given path
    public String getCommonServiceUrl(String path, boolean isFront) {
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        if ( !isFront ) {
            if ( this.getCommonServiceBackPort() > 0 ) {
                return commonServiceBackBaseUrl + ":" + commonServiceBackPort + "/" + path;
            } else {
                return commonServiceBackBaseUrl + "/" + path;
            }
        } else {
            if ( this.getCommonServiceFrontPort() > 0 ) {
                return commonServiceFrontBaseUrl + ":" + commonServiceFrontPort + "/" + path;
            } else {
                return commonServiceFrontBaseUrl + "/" + path;
            }
        }
    }

    @Value("${common.lang.default:en}")
    protected String commonLangDefault;
    public String getCommonLangDefault() {
        return commonLangDefault;
    }

    // ----------------------------------------------
    // Database setup
    // ----------------------------------------------
    @Value("${common.mongo.sharding.enabled:false}")
    protected boolean mongoShardingEnabled;
    public boolean isMongoShardingEnabled() {
        return mongoShardingEnabled;
    }

    @Value("${common.mongo.database:itc}")
    protected String mongoDatabase;
    public String getMongoDatabase() {
        return mongoDatabase;
    }

    // ----------------------------------------------
    // Google Api
    // ----------------------------------------------
    @Value("${common.google.api.key:}")
    protected String googleApiKey;
    public String getGoogleApiKey() {
        return googleApiKey;
    }

    // ----------------------------------------------
    // WiFi Geolocation
    // ----------------------------------------------
    @Value("${common.wifimac.cache.size:1000}")
    protected int wifiMacCacheSize;
    public int getWifiMacCacheSize() {
        return wifiMacCacheSize;
    }

    @Value("${common.wifimac.cache.ttl:3600}")
    protected int wifiMacCacheTtl;
    public int getWifiMacCacheTtl() {
        return wifiMacCacheTtl;
    }

    // Spring ISO duration (PT24H - 24 hours ; PT30M - 30 minutes)
    @Value("${common.wifimac.cache.logperiod:PT24H}")
    protected String wifiMacCacheLogPeriod;
    public String getWifiMacCacheLogPeriod() {
        return wifiMacCacheLogPeriod;
    }

    // ----------------------------------------------
    // Test activation
    // ----------------------------------------------
    @Value("${common.test.enabled:false}")
    protected boolean commonTestEnabled;
    public boolean isCommonTestEnabled() {
        return commonTestEnabled;
    }

    // ----------------------------------------------
    // Non Community activation (also required the associated classes)
    // ----------------------------------------------

    @Value("${common.nce.enable:false}")
    protected boolean commonNceEnable;
    public boolean isCommonNceEnable() {
        return commonNceEnable;
    }

}
