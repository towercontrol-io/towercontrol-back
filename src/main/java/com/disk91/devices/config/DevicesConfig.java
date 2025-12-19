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

package com.disk91.devices.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value = {"file:configuration/devices.properties"}, ignoreResourceNotFound = true)
public class DevicesConfig {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    // ----------------------------------------------
    // Common setup
    // ----------------------------------------------

    // Service Id used for multi-instance environment, must be different for every instances
    @Value("${common.service.id:83DZqvwbXzmtllVq}")
    protected String commonServiceId;
    public String getCommonServiceId() {
        return (commonServiceId.isEmpty())?"83DZqvwbXzmtllVq":commonServiceId;
    }

    @Value("${devices.integration.medium:memory}")
    protected String devicesIntegrationMedium;
    public String getDevicesIntegrationMedium() {
        return devicesIntegrationMedium;
    }


    // --------------------------------------------
    // Device Cache
    // --------------------------------------------

    @Value("${devices.cache.max.size:1000}")
    protected int devicesCacheMaxSize;
    public int getDevicesCacheMaxSize() {
        return devicesCacheMaxSize;
    }

    @Value("${devices.cache.expiration_s:0}")
    protected int devicesCacheExpiration;
    public int getDevicesCacheExpiration() {
        return devicesCacheExpiration;
    }

    @Value("${devices.cache.log.period:PT24H}")
    protected String devicesCacheLogPeriod;
    public String getDevicesCacheLogPeriod() {
        return devicesCacheLogPeriod;
    }


    // --------------------------------------------
    // Device NwkId Cache
    // --------------------------------------------

    @Value("${devices.nwkid.cache.max.size:1000}")
    protected int devicesNwkIdCacheMaxSize;
    public int getDevicesNwkIdCacheMaxSize() {
        return devicesNwkIdCacheMaxSize;
    }

    @Value("${devices.nwkid.cache.expiration_s:0}")
    protected int devicesNwkIdCacheExpiration;
    public int getDevicesNwkIdCacheExpiration() {
        return devicesNwkIdCacheExpiration;
    }

    @Value("${devices.nwkid.cache.log.period:PT24H}")
    protected String devicesNwkIdCacheLogPeriod;
    public String getDevicesNwkIdCacheLogPeriod() {
        return devicesNwkIdCacheLogPeriod;
    }

}
