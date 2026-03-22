package com.disk91.capture.api.interfaces.sub;

public enum InsertIDsStatus {
    INSERTED,
    INVALID_PROTOCOL_ID,
    INVALID_ENDPOINT_ID,
    INVALID_TYPE_ID,
    INVALID_HEADER,         // Header name found invalid
    DUPLICATED_HEADER,      // Same headeer found multiple times
    INVALID_STATUS,
    MISSING_DATA,           // Fields or Values missing
    MALFORMED_DATA,         // Format is not correct
    NOT_UNIQUE,             // One of the IDs already exists in DB
    UNKNOWN_ERROR
}
