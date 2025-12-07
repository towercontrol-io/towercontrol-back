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
package com.disk91.devices.services;

import com.disk91.common.tools.CloneableObject;
import com.disk91.common.tools.Now;
import com.disk91.common.tools.ObjectCache;
import com.disk91.common.tools.exceptions.ITNotFoundException;
import com.disk91.devices.config.DevicesConfig;
import com.disk91.devices.interfaces.DeviceState;
import com.disk91.devices.mdb.entities.Device;
import com.disk91.devices.mdb.repositories.DevicesRepository;
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
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class DevicesNwkCache {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * Device Cache Using the networkIds as a key for the Search
     * Must be read only !
     */

    @Autowired
    protected DevicesConfig deviceConfig;

    @Autowired
    protected DevicesRepository devicesRepository;

    @Autowired
    protected MeterRegistry meterRegistry;

    @Autowired
    protected DeviceCache deviceCache;


    // ================================================================================================================
    // CACHE SERVICE
    // ================================================================================================================

    private static class DeviceNwkCacheEntry implements CloneableObject<DeviceNwkCacheEntry> {
        public String deviceId;
        public DeviceNwkCacheEntry clone() {
            DeviceNwkCacheEntry copy = new DeviceNwkCacheEntry();
            copy.deviceId = this.deviceId;
            return copy;
        }
    }

    private ObjectCache<String, DeviceNwkCacheEntry> devicesCache;

    protected boolean serviceEnable = false;
    protected final ArrayList<DeviceState> validStates = new ArrayList<>();

    @PostConstruct
    private void initDevicesNwkCache() {
        log.info("[devices] initDevicesNwkCache");
        if ( deviceConfig.getDevicesNwkIdCacheMaxSize() > 0 ) {
            this.devicesCache = new ObjectCache<String, DeviceNwkCacheEntry>(
                    "DevicesNwkROCache",
                    deviceConfig.getDevicesNwkIdCacheMaxSize(),
                    deviceConfig.getDevicesNwkIdCacheExpiration()*1000L
            ) {
                @Override
                synchronized public void onCacheRemoval(String key, DeviceNwkCacheEntry obj, boolean batch, boolean last) {
                    // read only cache, do nothing
                }
                @Override
                public void bulkCacheUpdate(List<DeviceNwkCacheEntry> objects) {
                    // read only cache, do nothing
                }
            };
        }

        this.serviceEnable = true;

        Gauge.builder("devices_nwkid_cache_sum_time", this.devicesCache.getTotalCacheTime())
                .description("[Devices] total time cache execution")
                .register(meterRegistry);
        Gauge.builder("devices_nwkid_cache_sum", this.devicesCache.getTotalCacheTry())
                .description("[Devices] total cache try")
                .register(meterRegistry);
        Gauge.builder("devices_nwkid_cache_miss", this.devicesCache.getCacheMissStat())
                .description("[Devices] total cache miss")
                .register(meterRegistry);

        // Setup the state we consider for the cache as active devices (the other will be rejected)
        this.validStates.add(DeviceState.ACTIVATED);
        this.validStates.add(DeviceState.OPEN);
        this.validStates.add(DeviceState.ACTION_PENDING);
        this.validStates.add(DeviceState.ACTION_DONE);
    }

    @PreDestroy
    public void destroy() {
        log.info("[devices] DevicesNwkCache stopping");
        this.serviceEnable = false;
        if ( deviceConfig.getDevicesNwkIdCacheMaxSize() > 0 ) {
            devicesCache.deleteCache();
        }
        log.info("[devices] DevicesNwkCache stopped");
    }

    @Scheduled(fixedRateString = "${devices.cache.log.period:PT24H}", initialDelay = 3600_000)
    protected void devicesNwkCacheStatus() {
        try {
            Duration duration = Duration.parse(deviceConfig.getDevicesNwkIdCacheLogPeriod());
            if (duration.toMillis() >= Now.ONE_FULL_DAY ) return;
        } catch (Exception ignored) {}
        if ( ! this.serviceEnable || deviceConfig.getDevicesNwkIdCacheMaxSize() == 0 ) return;
        this.devicesCache.log();
    }

    // ================================================================================================================
    // Cache access
    // ================================================================================================================

    /**
     * Get the device from the cache or from the database if not in cache
     * @param type - type of networkId ( ex LoRa )
     * @param key - param of the search ( ex deveui)
     * @param value - param of the search ( ex value for deveui)
     * @return the device object
     * @throws ITNotFoundException if not found
     */
    public Device getDevice(String type, String key, String value) throws ITNotFoundException {
        if (!this.serviceEnable || deviceConfig.getDevicesNwkIdCacheMaxSize() == 0) {
            // direct access from database
            List<Device> u = devicesRepository.findDevicesByCommunicationIdTypeAndParamAndStates(type, key, value, this.validStates);
            if (u == null || u.isEmpty()) throw new ITNotFoundException("device-not-found");
            if ( u.size() > 1 ) {
                log.warn("[devices] getDevice multiple devices found for {}:{}/{}", type, key,value);
            }
            deviceCache.addDevicesToCache(u.getFirst());
            return u.getFirst().clone();
        } else {
            String searchKey = type + ":" + key + ":" + value;
            DeviceNwkCacheEntry u = this.devicesCache.get(searchKey);
            if (u == null) {
                // not in cache, get it from the database
                List<Device> _u = devicesRepository.findDevicesByCommunicationIdTypeAndParamAndStates(type, key, value, this.validStates);
                if (_u == null || _u.isEmpty()) throw new ITNotFoundException("device-not-found");
                if ( _u.size() > 1 ) {
                    log.warn("[devices] getDevice multiple devices found for {}:{}/{}", type, key,value);
                }
                Device d = _u.getFirst();
                deviceCache.addDevicesToCache(d);
                u = new DeviceNwkCacheEntry();
                u.deviceId = d.getId();
                this.devicesCache.put(u, searchKey);
                return d;
            }
            return deviceCache.getDevice(u.deviceId);
        }
    }

    /**
     * Remove a device from the local cache if exists (this is when the user has been updated somewhere else
     * @param deviceId - deviceId to be removed
     * @return
     */
    public void flushDevice(String deviceId) {
        if ( this.serviceEnable && deviceConfig.getDevicesNwkIdCacheMaxSize() > 0 ) {
            // This is a bit complex because we need to list all devices in the cache and remove those matching the deviceId
            // there is no key to directly find the deviceId
            // So as we use the cache for just the link nwkId -> DeviceId we may just remove entry when this information
            // has changed.
            ConcurrentLinkedQueue<String> kToRemove = new ConcurrentLinkedQueue<>();
            List<String> keys = Collections.list(this.devicesCache.list());
            keys.parallelStream().forEach(k -> {
                DeviceNwkCacheEntry e = this.devicesCache.get(k);
                if (e != null && e.deviceId != null && e.deviceId.equals(deviceId)) {
                    kToRemove.add(k);
                }
            });

            String k = kToRemove.poll();
            if ( k == null ) return;
            do {
                this.devicesCache.remove(k, false);
                k = kToRemove.poll();
            } while ( k != null );
        }
    }

    // @TODO - manage the broadcast request for user flush and scan for flush trigger on each of the instances

}
