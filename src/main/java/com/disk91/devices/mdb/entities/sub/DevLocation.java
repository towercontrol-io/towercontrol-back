package com.disk91.devices.mdb.entities.sub;

import com.disk91.common.tools.CloneableObject;
import com.disk91.common.tools.GeolocationTools;
import com.uber.h3core.H3Core;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.io.IOException;


@Tag(name = "Device Location", description = "Device location")
public class DevLocation implements CloneableObject<DevLocation> {

    // Date of the location update
    @Schema(
            description = "date of this location in ms since epoch",
            example = "172546501561",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    protected long locationMs;

    // Latitude
    @Schema(
            description = "latitude of the location",
            example = "45.254",
            requiredMode = Schema.RequiredMode.REQUIRED
    ) protected double latitude;

    // Longitude
    @Schema(
            description = "longitude of the location",
            example = "3.254",
            requiredMode = Schema.RequiredMode.REQUIRED
    ) protected double longitude;


    // Altitude (compared to sea level, in meters)
    @Schema(
            description = "altitude to sea level in meters",
            example = "320",
            requiredMode = Schema.RequiredMode.REQUIRED
    ) protected double altitude;

    // Floor (number or name of the floor, can also be a named location like building name)
    @Schema(
            description = "floor of the location",
            example = "1st floor",
            requiredMode = Schema.RequiredMode.REQUIRED
    ) protected String floor;


    // Accuracy in meters
    @Schema(
            description = "accuracy of the given location in meters",
            example = "625",
            requiredMode = Schema.RequiredMode.REQUIRED
    ) protected int accuracy;

    // h3 hex encoding of the location
    @Schema(
            description = "hex encoding for the location",
            example = "0x1234567890abcdef",
            requiredMode = Schema.RequiredMode.REQUIRED
    ) protected String hexLocation;


    // === CLONE ===

    public DevLocation clone() {
        DevLocation u = new DevLocation();
        u.setLocationMs(locationMs);
        u.setLatitude(latitude);
        u.setLongitude(longitude);
        u.setAltitude(altitude);
        u.setFloor(floor);
        u.setAccuracy(accuracy);
        u.setHexLocation(hexLocation);
        return u;
    }

    // ==== SPECIAL METHODS ====

    public String getHexLocation() {
        if ( hexLocation != null && !hexLocation.isEmpty() ) {
            return hexLocation;
        }
        if (GeolocationTools.isAValidCoordinate(this.getLatitude(), this.getLongitude()) ) {
            try {
                // get the hex corresponding in a resolution of 14 - 3m2
                H3Core h3 = H3Core.newInstance();
                this.setHexLocation(h3.latLngToCellAddress(this.getLatitude(), this.getLongitude(),14));
                return this.hexLocation;
            } catch (IOException ioException) {
                return null;
            }
        }
        return null;
    }

    // === GETTER / SETTER ===


    public long getLocationMs() {
        return locationMs;
    }

    public void setLocationMs(long locationMs) {
        this.locationMs = locationMs;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    public String getFloor() {
        return floor;
    }

    public void setFloor(String floor) {
        this.floor = floor;
    }

    public int getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(int accuracy) {
        this.accuracy = accuracy;
    }

    public void setHexLocation(String hexLocation) {
        this.hexLocation = hexLocation;
    }
}
