## Pivot object definition

A pivot object is used to standardize data received from different sources. This object does not handle business data 
but only metadata, while aiming to be generic with respect to possible integration protocols.

### Pivot object structure

```json
{
  "rxUuid": "string",                        // Unique identifier of received data
  "rxTimestamp": "long",                     // Reception Timestamp of the data reception in milliseconds since epoch
  "rxCaptureId": "string",                   // Source identifier (e.g., capture endpoint ID)
  "payload": "string",                       // Raw data payload as a string (can be encoded)
  "nwkStatus" : "enum",                      // Status of the frame seen by network (e.g., "RECEIVED", "PROCESSED", "ERROR")
  "status" : "enum",                         // status of the frame in the platform (e.g., "NEW", "PENDING", "PROCESSED", "ERROR")
  "headers" : [
    { 
      "key": "string",                       // Header key
      "value": "string"                      // Header value
    }
  ],
  "nwkErrors": [                             // List of errors encountered by network and reported
    {
      "level": "enum",                       // Error level (e.g., "WARNING", "ERROR", "CRITICAL")
      "code": "string",                      // Error code 0 when none
      "message": "string"                    // Error message
    }
  ],
  "errors": [                                // List of errors encountered during platform processing
    {
      "level": "enum",                       // Error level (e.g., "WARNING", "ERROR", "CRITICAL")
      "code": "string",                      // Error code 0 when none
      "message": "string"                    // Error message
    }
  ],
  "metadata": {                              // Additional metadata as key-value pairs
    nwkUuid: "string",                       // Network unique identifier for the device
    nwkTimestamp: "long",                    // Original data timestamp in milliseconds since epoch
    nwkTimeNs: "long",                       // Original data timestamp nanoseconds part (when available)
    nwkDeviceId: "string",                   // Device Id as identified on the network
    deviceId: "string",                      // Device Id as identified in the platform
    frameCounter: "int",                     // Frame counter when available
    framePort: "int",                        // Frame port when available
    confirmReq: "boolean",                   // Whether the frame has confirme request flag
    confirmed: "boolean",                    // Whether the frame was confirmed
    donwlinkReq: "boolean",                  // When the frame requests a downlink
    donwlinkResp: "boolean",                 // When the frame is corresponding to a downlink response
    radioMetadata: {                         // Radio metadata when available
      frequency: "float",                    // Frequency in MHz
      dataRate: "string",                    // Data rate description
      address: "string",                     // Network address, like Ip Address of DevAddr
      customParams: {                        // Protocol dependant Custom parameters as key-value pairs
        key: "string",                       // Custom parameter key
        value: "string"                      // Custom parameter value
      }
    },
    calculatedLocation: {                   // Calculated location when available
      latitude: "double",                    // Latitude in decimal degrees
      longitude: "double",                   // Longitude in decimal degrees
      altitude: "double",                    // Altitude in meters
      accuracy: "float",                     // Accuracy in meters
      hexagonId: "string"                    // Hexagon ID 
    }
  },
  nwkStations: [                            // List of network stations that received the frame
    {
      nkwTimestamp: "long",                  // Network timestamp when the frame was received in milliseconds since epoch
      nkwTimeNs: "long",                     // Network timestamp nanoseconds part (when available)
      stationId: "string",                   // Station identifier
      rssi: "int",                           // RSSI value
      snr: "float",                          // SNR value
      stationLocation: {                     // Station location when available
        latitude: "double",                    // Latitude in decimal degrees
        longitude: "double",                   // Longitude in decimal degrees
        altitude: "double",                    // Altitude in meters
        hexagonId: "string"                    // Hexagon ID
      },
      customParams: [{                        // Protocol dependant Custom parameters as key-value pairs
        key: "string",                       // Custom parameter key
        value: "string"                      // Custom parameter value
      }]
    }
  ]
}
```