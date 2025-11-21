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
  "type": "enum",                            // Type of the capture endpoint (e.g., "HTTP_PUSH", "MQTT", "HTTP_PULL", ...)
  "protocol": "enum",                        // Protocol used by the capture endpoint (e.g., "Helium", "Sigfox")
  "version": "enum",                         // Protocol version used, can be a version "v1", "v2", or "json", "protobuf", or combination...
  "customConfig": [{                         // Custom configuration specific to the protocol and type
    "key" : "string",                        // Protocol-specific configuration parameters
    "value" : "string"
  }],
}
```

### Capture endpoint types

The system relies on a definition of possible types, protocols, and versions so the frontend can present the appropriate 
options. The goal is to extend capabilities without heavily impacting frontend development. Additionally, it must be 
possible to dynamically specify `customConfig` options required by the protocol. These options will always be strings 
and will be evaluated as needed.


