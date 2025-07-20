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
package com.disk91.common.services;

import com.disk91.common.config.CommonConfig;
import com.disk91.common.mdb.entities.WiFiMacLocation;
import com.disk91.common.mdb.repositories.WiFiMacLocationRepository;
import com.disk91.common.tools.GeolocationTools;
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
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class WiFiMacGeolocationService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    // ================================================================================================================
    // CACHE SERVICE
    // ================================================================================================================

    @Autowired
    protected CommonConfig commonConfig;

    @Autowired
    protected WiFiMacLocationRepository wiFiMacLocationRepository;

    private ObjectCache<String, WiFiMacLocation> wifiMacLocationCache;

    private final MeterRegistry registry;
    public WiFiMacGeolocationService(MeterRegistry registry) {
        this.registry = registry;
    }

    protected boolean serviceEnable = false;

    @PostConstruct
    private void initWifiMacLocationCache() {
        log.info("[common] initWifiMacLocationCache");
        if ( commonConfig.getWifiMacCacheSize() > 0 ) {
            this.wifiMacLocationCache = new ObjectCache<String, WiFiMacLocation>(
                    "WiFiMacLocationRWCache",
                    commonConfig.getWifiMacCacheSize(),
                    commonConfig.getWifiMacCacheTtl()*1000
            ) {
                private ArrayList<WiFiMacLocation> _toSave = new ArrayList<>();

                @Override
                synchronized public void onCacheRemoval(String key, WiFiMacLocation obj, boolean batch, boolean last) {
                    if (batch) {
                        if (obj != null) _toSave.add(obj);
                        if (_toSave.size() > 5000 || last) {
                            _toSave.parallelStream().forEach((d) -> {
                                if ((d != null)) wiFiMacLocationRepository.save(d);
                            });
                            _toSave.clear();
                        }
                    } else {
                        wiFiMacLocationRepository.save(obj);
                    }
                }
                @Override
                public void bulkCacheUpdate(List<WiFiMacLocation> objects) {
                    wiFiMacLocationRepository.saveAll(objects);
                }
            };
        }

        this.serviceEnable = true;

        Gauge.builder("common.service.wifimacgeo.cache_total_time", this.wifiMacLocationCache.getTotalCacheTime())
                .description("[WifiMacGeo] total time cache execution")
                .register(registry);
        Gauge.builder("common.service.wifimacgeo.cache_total", this.wifiMacLocationCache.getTotalCacheTry())
                .description("[WifiMacGeo] total cache try")
                .register(registry);
        Gauge.builder("common.service.wifimacgeo.cache_miss", this.wifiMacLocationCache.getCacheMissStat())
                .description("[WifiMacGeo] total cache miss")
                .register(registry);
    }

    @PreDestroy
    public void destroy() {
        log.info("[common] WiFiMacGeolocationService stopping");
        this.serviceEnable = false;
        if ( commonConfig.getWifiMacCacheSize() > 0 ) {
            wifiMacLocationCache.deleteCache();
        }
        log.info("[common] WiFiMacGeolocationService stopped");
    }

    @Scheduled(fixedRateString = "${common.wifimac.cache.logperiod}", initialDelay = 3600_000)
    protected void wifiMacLocationCacheStatus() {
        try {
            Duration duration = Duration.parse(commonConfig.getWifiMacCacheLogPeriod());
            if (duration.toMillis() >= Now.ONE_FULL_DAY ) return;
        } catch (Exception ignored) {}
        if ( ! this.serviceEnable || commonConfig.getWifiMacCacheSize() == 0 ) return;
        this.wifiMacLocationCache.log();
    }

    // ================================================================================================================
    // Cache access
    // ================================================================================================================

    /**
     * Get A Mac Location from cache or Database, when mac found, it is then stored in cache
     * for a faster access next time ; when cache size > 0 or we just use db access
     *
     * @param macAddress
     * @return
     * @throws ITNotFoundException
     */
    public WiFiMacLocation getWiFiMacLocation(String macAddress)
    throws ITNotFoundException {
        if ( ! this.serviceEnable ) throw new ITNotFoundException("WiFiMacLocation service is disabled");
        macAddress = macAddress.toUpperCase();

        WiFiMacLocation ret = this.wifiMacLocationCache.get(macAddress);
        if ( ret == null && commonConfig.getWifiMacCacheSize() > 0 ) {
            ret = wiFiMacLocationRepository.findByMacAddress(macAddress);
            if ( ret != null ) {
                this.wifiMacLocationCache.put(ret, ret.getMacAddress());
            }
        }
        if ( ret == null ) {
            throw new ITNotFoundException("WiFiMacLocation not found for macAddress : "+macAddress);
        }
        return ret;
    }


    public void upsertWiFiMacLocation(String macAddress, double lat, double lon) {
        upsertWiFiMacLocation(macAddress, lat, lon, 0, -200, false);
    }

    /**
     * Insert a WiFiMacLocation in the database or update the existing information if we already have it
     * @param macAddress
     * @param lat
     * @param lon
     * @param attenuation
     * @param rssi
     * @param certified
     */
    public void upsertWiFiMacLocation(String macAddress, double lat, double lon, double attenuation, double rssi, boolean certified) {
        if ( ! this.serviceEnable ) return;
        macAddress = macAddress.toUpperCase();

        try {
            WiFiMacLocation existing = this.getWiFiMacLocation(macAddress);
            // Existing, we need to verify the information
            existing.setLastSeenDateMs(Now.NowUtcMs());
            if ( certified ) {
                // here we will force the update of the location
                existing.setState(WiFiMacLocation.MacLocationState.CERTIFIED);
                existing.setMacLocation(new GeoJsonPoint(lon, lat));
                existing.setRssi(rssi);
                existing.setAttenuation(attenuation);
            } else {
                switch (existing.getState() ) {
                    case INVALIDATED:
                        // The location has been invalidated, we don't want to use this mac anymore
                        return;
                    case UNKKNOWN:
                        // The location is unknown, but we may have a new proposal
                        if (GeolocationTools.isAValidCoordinate(lat, lon)) {
                            existing.setState(WiFiMacLocation.MacLocationState.VALID);
                            existing.setRssi(rssi);
                            existing.setAttenuation(attenuation);
                            existing.setMacLocation(new GeoJsonPoint(lon, lat));
                        } else {
                            // nothing more to do, the location is still unknown
                            return;
                        }
                        break;
                    case VALID:
                        // The location is valid but not certified, we verify the distance to see if it
                        // is to invalidate
                        if ( GeolocationTools.distanceBetween(existing.getMacLocation().getY(), lat, existing.getMacLocation().getX(), lon, 0 , 0 ) > 1000 ) {
                            existing.setState(WiFiMacLocation.MacLocationState.INVALIDATED);
                            existing.setMacLocation(new GeoJsonPoint(0, 0));
                            existing.setRssi(-200);
                        } else {
                            // When the signal is better, we can update the coordinates
                            if ( existing.getRssi() < rssi ) {
                                existing.setMacLocation(new GeoJsonPoint(lon, lat));
                                existing.setRssi(rssi);
                            }
                        }
                        break;
                    case CERTIFIED:
                        // The location is certified, we don't want to update it
                        return;
                    default:
                        log.error("[common] Unknown state for macAddress : "+macAddress);
                        break;
                }

                // Save the modification in cache, il will later be flushed to the database
                if (  commonConfig.getWifiMacCacheSize() > 0 ) {
                    this.wifiMacLocationCache.put(existing, macAddress);
                } else {
                    wiFiMacLocationRepository.save(existing);
                }
            }

        } catch (ITNotFoundException e) {
            // New Mac Address
            WiFiMacLocation macLocation = new WiFiMacLocation();
            macLocation.setMacAddress(macAddress);
            macLocation.setMacLocation(new GeoJsonPoint(lon, lat));
            macLocation.setAttenuation(attenuation);
            macLocation.setLastSeenDateMs(Now.NowUtcMs());
            macLocation.setRegistrationDateMs(Now.NowUtcMs());
            if ( certified ) {
                macLocation.setState(WiFiMacLocation.MacLocationState.CERTIFIED);
            } else {
                if (GeolocationTools.isAValidCoordinate(lat, lon)) {
                    macLocation.setState(WiFiMacLocation.MacLocationState.VALID);
                } else {
                    macLocation.setState(WiFiMacLocation.MacLocationState.UNKKNOWN);
                }
            }
            macLocation = wiFiMacLocationRepository.save(macLocation);
            if (  commonConfig.getWifiMacCacheSize() > 0 ) {
                this.wifiMacLocationCache.put(macLocation, macAddress);
            }
        }
    }


    public record MacWithRssi(String mac, double rssi) {}

    /**
     * Obtain a GeoLocation base on an array of positions
     * -> upsert the related information when upsert it true. The discovered Macs are updated in the database
     * -> computedLocation based on RSSI when computedLoc is true / return average location
     * -> don't compute location if the number of valid macs is less than minLoc (anti-tracking security)
     */
    public GeolocationTools.Coordinate getGeoLocation(List<MacWithRssi> macs, int minLoc, boolean computedLoc, boolean upsert)
    throws ITNotFoundException
    {

        ArrayList<WiFiMacLocation> valids = new ArrayList<>();
        ArrayList<MacWithRssi> newMacs = new ArrayList<>();
        ArrayList<WiFiMacLocation> toInvalidate = new ArrayList<>();

        int certified = 0;
        for ( MacWithRssi mac : macs ) {
            try {
                WiFiMacLocation loc = this.getWiFiMacLocation(mac.mac());
                switch ( loc.getState() ) {
                    case VALID:
                        valids.add(loc);
                        break;
                    case CERTIFIED:
                        certified++;
                        valids.add(loc);
                        break;
                    case UNKKNOWN:
                        newMacs.add(mac);
                }
            } catch (ITNotFoundException e) {
                // new mac
                newMacs.add(mac);
            }
        }

        if ( valids.size() > 0 && valids.size() >= minLoc ) {

            GeolocationTools.Coordinate center = new GeolocationTools.Coordinate(0,0);
            // Simple case, one single known Mac
            if ( valids.size() == 1 ) {
                center = new GeolocationTools.Coordinate(valids.getFirst().getMacLocation().getY(), valids.getFirst().getMacLocation().getX());
            } else {
                // are the macs position we have good to find a location ?
                // To determine the position and eliminate aberrant data, we will calculate the average coordinates and compute
                // the maximum distance from this center. Gradually, we will remove any coordinates that are more than 300m
                // away from this center, ensuring that all MACs fall within this perimeter. This allows us to find the true
                // position by considering the majority. If more than two MACs remain, then we have a valid position and can
                // update the other unknowns while discarding those that are too distant. In the case where we have a certified
                // MAC, we can directly consider it as the correct center and filter based on that.
                if ( certified > 0) {
                    double slat = 0;
                    double slon = 0;
                    for ( WiFiMacLocation loc : valids ) {
                        if ( loc.getState() == WiFiMacLocation.MacLocationState.CERTIFIED ) {
                            slat += loc.getMacLocation().getY();
                            slon += loc.getMacLocation().getX();
                        }
                    }
                    center = new GeolocationTools.Coordinate(slat / certified, slon / certified);
                } else {
                    int maxDistance;
                    do {
                        double slat = 0;
                        double slon = 0;
                        for (WiFiMacLocation loc : valids) {
                            slat += loc.getMacLocation().getY();
                            slon += loc.getMacLocation().getX();
                        }
                        center = new GeolocationTools.Coordinate(slat / valids.size(), slon / valids.size());

                        maxDistance = 0;
                        for (WiFiMacLocation loc : valids) {
                            double distance = GeolocationTools.distanceBetween(center.lat(), loc.getMacLocation().getY(), center.lng(), loc.getMacLocation().getX(), 0, 0);
                            if (distance > maxDistance) {
                                maxDistance = (int) distance;
                            }
                        }

                        // eliminate the max one if over 300m
                        if (maxDistance > 300) {
                            WiFiMacLocation maxLoc = null;
                            for (WiFiMacLocation loc : valids) {
                                double distance = GeolocationTools.distanceBetween(center.lat(), loc.getMacLocation().getY(), center.lng(), loc.getMacLocation().getX(), 0, 0);
                                if ((int) distance == maxDistance) {
                                    maxLoc = loc;
                                    break;
                                }
                            }
                            if (maxLoc != null) {
                                valids.remove(maxLoc);
                                toInvalidate.add(maxLoc);
                            }
                        }

                    } while (maxDistance > 300 && valids.size() > 1);
                    if ( maxDistance > 300 ) {
                        // Impossible to determine the right location, the macs we have are too far from each other
                        // Impossible to determine what mac is wrong
                        throw new ITNotFoundException("Mac position too far from each other");
                    }
                }
            }
            // Here we have a center position and a list of valid macs around this center position
            if ( computedLoc ) {
                // @TODO : Compute the location based on RSSI to change the center position to become the target position

            }

            // Here the center position is the target position to return
            // Before returning the position, we need to update the macs we have discovered
            if ( upsert ) {
                // Create the new one
                for (MacWithRssi mac : newMacs) {
                    this.upsertWiFiMacLocation(mac.mac(), center.lat(), center.lng(), 0, mac.rssi(), false);
                }
                // Remove the wrong macs
                for ( WiFiMacLocation mac : toInvalidate ) {
                    mac.setState(WiFiMacLocation.MacLocationState.INVALIDATED);
                    mac.setMacLocation(new GeoJsonPoint(0,0));
                    mac.setRssi(-200);
                    // Save the modification in cache, il will later be flushed to the database
                    if (  commonConfig.getWifiMacCacheSize() > 0 ) {
                        this.wifiMacLocationCache.put(mac, mac.getMacAddress());
                    } else {
                        wiFiMacLocationRepository.save(mac);
                    }
                }
                // Update the valid macs when RSSI is better
                for ( WiFiMacLocation valid : valids ) {
                    // find the rssi
                    int rssi = -200;
                    for ( MacWithRssi mac : macs ) {
                        if ( mac.mac().compareToIgnoreCase(valid.getMacAddress()) == 0 ) {
                            rssi = (int) mac.rssi();
                            break;
                        }
                    }
                    this.upsertWiFiMacLocation(valid.getMacAddress(), center.lat(), center.lng(), valid.getAttenuation(), rssi, false);
                }
            }
            return center;

        } else {
            // Not enough valid macs
            throw new ITNotFoundException("Mac position Unknown or lack of data");
        }

    }


}
