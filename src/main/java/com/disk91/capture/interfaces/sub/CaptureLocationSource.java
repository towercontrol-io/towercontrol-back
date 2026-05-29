package com.disk91.capture.interfaces.sub;

public enum CaptureLocationSource {
    NETWORK_RSSI, // Radio Triangulation
    NETWORK_TDOA,
    NETWORK_DOPLER,
    GPS,
    GPS_RTK,
    WIFI_RSSI,
    BLE_RSSI,
    BLE_CHSOUNDING,
    LE_UWB,

    DECLARED,
    UNKNOWN
}
