package com.disk91.common.mdb.entities;

import com.disk91.common.tools.CloneableObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Sharded;
import org.springframework.data.mongodb.core.mapping.ShardingStrategy;

@Document(collection = "common_wifimac_location")
@CompoundIndexes({
        @CompoundIndex(name = "comWifiMacLocation_macAddress_Idx", def = "{'macAddress' : 'hashed' }"),
})
@Sharded(shardKey = { "macAddress", "id" }, shardingStrategy = ShardingStrategy.RANGE)
public class WiFiMacLocation implements CloneableObject<WiFiMacLocation> {

    public enum MacLocationState {
        CERTIFIED,  // The mac address has a valid localisation certified by the user
        VALID,      // The mac address has a valid localisation returned / calculated by the system
        INVALIDATED, // the mac address has been invalidated ( multiple location, smartphone...)
        UNKKNOWN   // The mac address has no location yet
    }

    @Transient
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Id
    protected String id = null;

    // Mac address - upper case
    protected String macAddress;

    // Mac State
    protected MacLocationState state;

    // Date of the last time this mac address has been seen
    protected long lastSeenDateMs;

    // Date of the first registration of the mac address
    protected long registrationDateMs;

    // Geolocation of the mac address
    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    protected GeoJsonPoint macLocation;

    // Add an attenuation in dBm for better triangulation later
    protected double attenuation;

    // Best RSSI for location precision eventually
    protected double rssi;

    // ================================================================================================================
    // Clonable implementation

    public WiFiMacLocation clone() {
        WiFiMacLocation clone = new WiFiMacLocation();
        clone.id = this.id;
        clone.macAddress = this.macAddress;
        clone.lastSeenDateMs = this.lastSeenDateMs;
        clone.registrationDateMs = this.registrationDateMs;
        clone.macLocation = new GeoJsonPoint(this.macLocation.getX(), this.macLocation.getY());
        clone.attenuation = this.attenuation;
        clone.state = this.state;
        clone.rssi = this.rssi;
        return clone;
    }

    // ================================================================================================================
    // Getters / Setters


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public long getLastSeenDateMs() {
        return lastSeenDateMs;
    }

    public void setLastSeenDateMs(long lastSeenDateMs) {
        this.lastSeenDateMs = lastSeenDateMs;
    }

    public long getRegistrationDateMs() {
        return registrationDateMs;
    }

    public void setRegistrationDateMs(long registrationDateMs) {
        this.registrationDateMs = registrationDateMs;
    }

    public GeoJsonPoint getMacLocation() {
        return macLocation;
    }

    public void setMacLocation(GeoJsonPoint macLocation) {
        this.macLocation = macLocation;
    }

    public double getAttenuation() {
        return attenuation;
    }

    public void setAttenuation(double attenuation) {
        this.attenuation = attenuation;
    }

    public MacLocationState getState() {
        return state;
    }

    public void setState(MacLocationState state) {
        this.state = state;
    }

    public double getRssi() {
        return rssi;
    }

    public void setRssi(double rssi) {
        this.rssi = rssi;
    }
}

