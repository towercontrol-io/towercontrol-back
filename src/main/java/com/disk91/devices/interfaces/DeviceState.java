package com.disk91.devices.interfaces;

public enum DeviceState {
    IDENTIFIED,                 // The device IDs have been created into the platform.
    ASSOCIATED,                 // The device is associated to a physical device, the physical device exists
    STORED,                     // The device is stored into an inventory
    COMMISSIONED,               // The device has been declared on the telco networks, telco IDs are associated.
    AFFECTABLE,                 // The device can be affected to a user or a group
    AFFECTED,                   // The device is affected to a user or a group
    ACTIVATED,                  // The device is activated on the communication networks and able to send data
    OPEN,                       // The device data can flow into the platform
    ACTION_PENDING,             // The device can be upgraded to a new version or get a new configuration
    ACTION_DONE,                // The pending action has been executed. This is an acknowledgeable state
    OUT_OF_SUBSCRIPTION,        // The device subscription (user or network) is expired, no data can be sent from the device
    DEFECTIVE,                  // The device is defective and need to be replaced.
    LOST,                       // The device is lost.
    RETURNABLE,                 // The device can be returned to the manufacturer or the solution provider.
    RETURNED,                   // The device has been returned to the manufacturer or the solution provider.
    TRASHED,                    // The device is trashed and can be removed from the platform.
    RECYCLED,                   // The device is recycled (destroyed, not for reuse) and can be removed from the platform.
}
