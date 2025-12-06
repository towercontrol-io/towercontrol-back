## Data Ingestion Flow

A data item arriving from outside follows a dynamic path before being processed. It reaches an endpoint (for HTTP push 
data), but it can also arrive on a topic or other channel. This entry point is an endpoint created dynamically by a user 
with `ROLE_BACKEND_CAPTURE` and `ROLE_DEVICE_ADMIN` rights. This endpoint references a protocol that contains the 
processing functions associated with the data stream.

The received data, whose format depends on the protocol, will be converted into a pivot format that is the same 
regardless of the data source. In this pivot format, a link is made with the platform's `device` object. 
This link is normally established from the network identifiers present in the structure processed by the protocol, 
for example, a DevEUI in the case of LoRaWan.

The protocol will use identifiers present in the stream to find the associated device; there must be only one entry in 
the device table matching that identifier.

The pivot objects are then processed asynchronously (depending on the `capture.(a)sync.processing` settings) and in 
parallel (depending on the `ccapture.processor.threads.count` parameter) to be transformed via a dynamic function 
allowing specific processing or by invoking a generic, user-programmable function.

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
  "wideOpen": "boolean",                     // If true, the endpoint is accessible to authenticated user not only the owner
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
  "createdBy": "string",                     // Creator identifier (system or user)
  "encrypted": "boolean"                     // When true, the (sensitive) pivot objet data will be encrypted, slower so not a default behavior 
}
```

### Wide Open mode

Depending on the type of integration, security may be restricted to the endpoint owner. This is the case, for example, 
for a LoRaWan endpoint where the LNS is the sole source for the endpoint. In that case we verify that the connecting user 
has the right to connect and upload data and that they are indeed the owner of that connection, to ensure another token 
is not attempting to inject data into the wrong endpoint. This approach also allows accepting incoming devices and 
performing auto-commissioning.

However, for other types of devices such as devices connected via Wi-Fi, the device may have a direct connection to the 
endpoint with its own identity. Authorization is therefore managed based on the device's identity rather than the endpoint's, 
and only later, once the data has been translated into the pivot model. A flag thus indicates the interface mode to handle 
security appropriately.

### Ingestion error handling

We can encounter several error situations during data ingestion. In some cases the error is, for example, a malformed
payload that does not match the protocol and appears to be a submission of an invalid frame.
In this situation, we may abandon the frame by rejecting it with an Exception; this will generate a simple log entry,
while taking care not to produce too many similar entries because a missed protocol change could quickly spam the logs.
It may also happen that we face a data interpretation issue, such as a field with an unexpected but overall valid value.
In that case, if we cannot convert the data, we will keep the traces for analysis and mark the data as an error while
still returning the Pivot structure.
Its error status means it will likely not be processed further, but it will nevertheless be stored in the database
with all received information, both raw and partially decoded.
In the case of an identified attack, processing may also reject the frame without storing it, but with an audit log
entry including information related to the source. Storage will be limited to avoid overloading the database.

