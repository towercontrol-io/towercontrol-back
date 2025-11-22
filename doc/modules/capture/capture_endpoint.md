## Capture endpoint definition


A capture endpoint is a dynamically created interface that must be able to scale across multiple nodes to handle the load.
It is defined by the following structure, which allows it to be created by an administrator or by a user with the 
`ROLE_DEVICE_ADMIN` role.

### Capture endpoint structure

```json
{
  "name": "string",                          // Name of the capture endpoint
  "ref": "string",                           // Unique reference of the capture endpoint, used in the endpoint URL or topic
  "owner" : "string",                        // Owner identifier apiUser or apikey owner (it must match the authentication)
  "description": "string",                   // Description of the capture endpoint
  "protocolId": "string",                    // Identifier of the protocol driver used to handle this capture endpoint
  "customConfig": [{                         // Custom configuration specific to the protocol and type
    "key" : "string",                        // Protocol-specific configuration parameters
    "value" : "string"
  }],
  "creationMs": "long"                       // Creation timestamp in milliseconds since epoch
}
```

### Capture endpoint types

The system relies on a definition of possible types, protocols, and versions so the frontend can present the appropriate 
options. The goal is to extend capabilities without heavily impacting frontend development. Additionally, it must be 
possible to dynamically specify `customConfig` options required by the protocol. These options will always be strings 
and will be evaluated as needed.

### Supported protocols

The field `protocolId` represents an identifier that refers to a protocol definition which itself is associated with a 
driver responsible for handling that protocol. This driver provides functions related to data reception, but also 
implements the full lifecycle features — for example device commissioning, management of bidirectional communications, etc. 
It is therefore a fairly rich component that must implement a common interface.

This link is associated with several types of structures such as capture links or device families. The goal is for this 
to be extensible without impacting the main processing logic. To describe a protocol, the following structure is used 
which, like roles, has a static in-memory definition for very fast access but can be supplemented by additional database 
entries.

```json
{
  "id": "string",                            // Unique identifier of the protocol
  "protocolFamily": "string",                // Protocol family (e.g., Sigfox, LoRaWan...)
  "protocolType": "string",                  // Protocol type (e.g., lorawan-helium)
  "protocolVersion" : "string",              // Protocol version (e.g., lorawan-helium-chirpstack-v4)
  "description": "string",                   // Description of the protocol (slug for i18n)
  "enDescription": "string",                 // English description of the protocol
  "processingClassName": "string",           // Fully qualified class name of the driver implementing this protocol
  "creationMs": "long",                      // Creation timestamp in milliseconds since epoch
  "createdBy": "string"                      // Creator identifier (system or user)
}
```

