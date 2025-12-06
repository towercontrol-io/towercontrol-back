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

import com.disk91.common.tools.Now;
import com.disk91.common.tools.ObjectCache;
import com.disk91.common.tools.exceptions.ITNotFoundException;
import com.disk91.devices.config.DevicesConfig;
import com.disk91.devices.mdb.entities.Device;
import com.disk91.devices.mdb.entities.DeviceHistory;
import com.disk91.devices.mdb.entities.sub.DeviceHistoryReason;
import com.disk91.devices.mdb.repositories.DevicesHistoryRepository;
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
import java.util.HashMap;
import java.util.List;

@Service
public class DeviceCache {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * Device Cache Service is caching the Group Information. It may be instantiated in all the instances
     * and multiple cache should collaborate in a cluster
     */

    @Autowired
    protected DevicesConfig deviceConfig;

    @Autowired
    protected DevicesRepository devicesRepository;

    @Autowired
    protected DevicesHistoryRepository devicesHistoryRepository;

    @Autowired
    protected MeterRegistry meterRegistry;


    // ================================================================================================================
    // CACHE SERVICE
    // ================================================================================================================

    private ObjectCache<String, Device> devicesCache;

    protected boolean serviceEnable = false;

    @PostConstruct
    private void initDevicesCache() {
        log.info("[devices] initDevicesCache");
        if ( deviceConfig.getDevicesCacheMaxSize() > 0 ) {
            this.devicesCache = new ObjectCache<String, Device>(
                    "DevicesROCache",
                    deviceConfig.getDevicesCacheMaxSize(),
                    deviceConfig.getDevicesCacheExpiration()*1000
            ) {
                @Override
                synchronized public void onCacheRemoval(String key, Device obj, boolean batch, boolean last) {
                    // read only cache, do nothing
                }
                @Override
                public void bulkCacheUpdate(List<Device> objects) {
                    // read only cache, do nothing
                }
            };
        }

        this.serviceEnable = true;

        Gauge.builder("devices_service_cache_sum_time", this.devicesCache.getTotalCacheTime())
                .description("[Devices] total time cache execution")
                .register(meterRegistry);
        Gauge.builder("devices_service_cache_sum", this.devicesCache.getTotalCacheTry())
                .description("[Devices] total cache try")
                .register(meterRegistry);
        Gauge.builder("devices_service_cache_miss", this.devicesCache.getCacheMissStat())
                .description("[Devices] total cache miss")
                .register(meterRegistry);
    }

    @PreDestroy
    public void destroy() {
        log.info("[devices] DevicesCache stopping");
        this.serviceEnable = false;
        if ( deviceConfig.getDevicesCacheMaxSize() > 0 ) {
            devicesCache.deleteCache();
        }
        log.info("[devices] DevicesCache stopped");
    }

    @Scheduled(fixedRateString = "${devices.cache.log.period:PT24H}", initialDelay = 3600_000)
    protected void devicesCacheStatus() {
        try {
            Duration duration = Duration.parse(deviceConfig.getDevicesCacheLogPeriod());
            if (duration.toMillis() >= Now.ONE_FULL_DAY ) return;
        } catch (Exception ignored) {}
        if ( ! this.serviceEnable || deviceConfig.getDevicesCacheMaxSize() == 0 ) return;
        this.devicesCache.log();
    }

    // ================================================================================================================
    // Cache access
    // ================================================================================================================

    /**
     * Get the device from the cache or from the database if not in cache
     * @param deviceId - devicesId to be retrieved
     * @return the device object
     * @throws ITNotFoundException if not found
     */
    public Device getDevice(String deviceId) throws ITNotFoundException {
        if (!this.serviceEnable || deviceConfig.getDevicesCacheMaxSize() == 0) {
            // direct access from database
            Device u = devicesRepository.findOneDeviceById(deviceId);
            if (u == null) throw new ITNotFoundException("device-not-found");
            return u.clone();
        } else {
            Device u = this.devicesCache.get(deviceId);
            if (u == null) {
                // not in cache, get it from the database
                u = devicesRepository.findOneDeviceById(deviceId);
                if (u == null) throw new ITNotFoundException("device-not-found");
                this.devicesCache.put(u, u.getId());
            }
            return u.clone();
        }
    }

    /**
     * Add a device to the local cache when not already in this is to avoid database read
     * when the device has been extracted from another source
     * @param device device to be added
     */
    public void addDevicesToCache(Device device) {
        if ( this.serviceEnable && deviceConfig.getDevicesCacheMaxSize() > 0 ) {
           Device u = this.devicesCache.get(device.getId());
           if (u == null) {
              this.devicesCache.put(device, device.getId());
           }
        }
    }

    /**
     * Search a device with the data stream Id and store in the cache if not already
     * This will query the database on every call as the cache is based on the Id
     * @param streamId
     * @return
     * @throws ITNotFoundException
     */
    public List<Device> getDevicesByDataStream(String streamId) throws ITNotFoundException {
        List<Device> devices = devicesRepository.findDevicesByDataStreamId(streamId);
        if (devices == null || devices.isEmpty()) throw new ITNotFoundException("device-not-found");
        if (!this.serviceEnable || deviceConfig.getDevicesCacheMaxSize() == 0) {
            for ( Device device : devices ) {
                Device u = this.devicesCache.get(device.getId());
                if (u != null) {
                    this.devicesCache.put(u, u.getId());
                }
            }
        }
        return devices;
    }

    /**
     * Remove a device from the local cache if exists (this is when the user has been updated somewhere else
     * @param d - deviceId to be removed
     * @return
     */
    public void flushDevice(String d) {
        if ( this.serviceEnable && deviceConfig.getDevicesCacheMaxSize() > 0 ) {
            this.devicesCache.remove(d,false);
        }
    }

    /**
     * Save the Device structure after an update. The cache is flushed for this device
     * when the reason is given, the device is saved in History
     * This is not protected against concurrent access on multiple cache service instance
     * @param u
     */
    public void saveDevice(Device u, DeviceHistoryReason r) {
        if ( r != DeviceHistoryReason.NO_REASON ) {
            // make a copy from the original device
            DeviceHistory dh = DeviceHistory.getDeviceHistory(u, r);
            devicesHistoryRepository.save(dh);
        }
        devicesRepository.save(u);
        this.flushDevice(u.getId());
    }

    /**
     * Delete a device from the database and flush the cache, if a reason is given, the device is saved in History
     * @param u - device to be deleted
     * @param r - reason for deletion
     */
    public void deleteDevice(Device u, DeviceHistoryReason r) {
        if ( r != DeviceHistoryReason.NO_REASON ) {
            // make a copy from the original device
            DeviceHistory dh = DeviceHistory.getDeviceHistory(u, r);
            devicesHistoryRepository.save(dh);
        }
        devicesRepository.delete(u);
        this.flushDevice(u.getId());
    }

    // @TODO - manage the broadcast request for user flush and scan for flush trigger on each of the instances

}
