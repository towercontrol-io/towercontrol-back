package com.disk91.capture.interfaces.sub;

public enum CaptureLocationSource {
    NONE,           // There is no location
    NETWORK_RSSI,   // Radio Triangulation
    NETWORK_TDOA,   // Time Difference on arrival
    NETWORK_DOPLER, // Dopler effet like for sat
    GPS,            // GPS coordinate
    GPS_RTK,        // GPS + RTK
    WIFI_RSSI,      // WiFi Triangulation
    BLE_RSSI,       // BLE Triangulation
    BLE_CHSOUNDING, // BLE Channel Sounding
    UWB,            // UWB Triangulation

    DECLARED,       // Statically declared location
    UNKNOWN
}
