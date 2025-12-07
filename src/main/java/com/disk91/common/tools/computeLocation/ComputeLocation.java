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

package com.disk91.common.tools.computeLocation;

import com.disk91.common.tools.GeolocationTools;
import com.disk91.common.tools.exceptions.ITNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ComputeLocation {

    private final static Logger log = LoggerFactory.getLogger(ComputeLocation.class);

    // Compute a location from a list of stations
    // Simple approach, just best signal
    public static Location computeLocation(
            List<Location> locations
    ) throws ITNotFoundException {

        double best = -160.0;
        Location bestLocation = null;
        for (Location loc : locations) {
            // search for best station to make it simple
            if ( loc.rssi > best && loc.radius <= 300 && GeolocationTools.isAValidCoordinate(loc.lat,loc.lng)) {
                best = loc.rssi;
                bestLocation = loc;
            }
            if (! GeolocationTools.isAValidCoordinate(loc.lat,loc.lng)) {
                log.debug("[tools] Invalid loc - rssi:{} radius:{} pos({}, {})", loc.rssi, loc.radius, loc.lat, loc.lng);
            }
        }
        if ( best > -160.0 ) {
            return new Location(bestLocation.lat,bestLocation.lng,estimatedDistance(bestLocation.rssi), bestLocation.rssi);
        }
        throw new ITNotFoundException("tools-impossible-to-compute-valid-location");
    }


    /**
     * Base on the RSSI, estimate the distance in meters, this is not very accurate
     * but give a rough idea
     *
     * @param rssi
     * @return
     */
    public static double estimatedDistance(double rssi) {

        if ( rssi < -110 ) return 20_000.0;
        if ( rssi < -103 ) return 10_000.0;
        if ( rssi < -97  ) return  5_000.0;
        if ( rssi < -89  ) return  2_000.0;
        if ( rssi < -83  ) return  1_000.0;
        if ( rssi < -82  ) return    800.0;
        if ( rssi < -80  ) return    500.0;
        if ( rssi < -70  ) return    200.0;
        else return 100.0;

    }

}
