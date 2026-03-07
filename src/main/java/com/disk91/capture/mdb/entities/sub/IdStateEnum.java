package com.disk91.capture.mdb.entities.sub;

public enum IdStateEnum {
    UNKNOWN,          // The ID state is unknown and has not been set yet
    NOT_ASSIGNED,     // The ID has been created but not yet assigned to any device
    ASSIGNED,         // The ID as assigned to an existing device but no subscription has been assigned yet
    IN_USE,           // The ID is assigned and a subscription has been activated. The subscription is valid.
    RETURNED,         // The ID has been returned (unassigned) from a device and relocated into the pool, current subscription can be reused
    EXPIRED_RETURNED, // The subscription has expired for a returned ID, reassigning the ID will require a new subscription
    EXPIRED_IN_USE,   // The subscription has expired for an ID in use, the ID is still assigned to the device but a new subscription is required to reactivate it
    REMOVED           // The ID has been removed from the pool, it should not be used anymore, even if we keep track for future.
}
