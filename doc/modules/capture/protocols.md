# Communication Protocols
The solution natively supports communication protocols and allows adding additional ones via drivers that can be added. 
Protocols are associated with a hierarchy and a declaration with specific parameters. When creating a new capture chain, 
the protocol must be selected and its parameters must be provided.

## Protocol Descriptor Format
Protocols are defined in the `CaptureProtocolsCache` class and have the following fields. They can also be inserted 
into the database for dynamic creation without modifying this code class and can be associated with a specific driver 
class. The goal is to be able to add drivers without impacting the open-source code and thus allow easy updates:

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
  "defaultWideOpen" : "boolean",             // Default value for wideOpen flag indicating each device will directly reach the endpoint
  "mandatoryFields": [{
      "name": "string",                        // Name of the mandatory field
      "valueType": "string",                   // Value type of the mandatory field (string, boolean, number)
      "description": "string",                 // Description of the mandatory field (I18N key formated slug)
      "enDescription": "string"                // English description of the mandatory field
    }],                                       // List of mandatory fields required to configure this protocol
  "protocolIds" : [{
     "name" : "string",                        // Name of the protocol identifier type (static, dynamic, otaa, abp...)
     "description": "string",                  // Description of the protocol identifier type (I18N key formated slug)
     "enDescription": "string",                // English description of the protocol identifier type
     "mandatoryFields": [{
          "name": "string",                    // Name of the mandatory field
          "valueType": "string",               // Value type of the mandatory field (string, boolean, number)
          "description": "string",             // Description of the mandatory field (I18N key formated slug)
          "enDescription": "string"            // English description of the mandatory field
     }]                                       // List of mandatory fields for a single subscription identifier
  }]
}
```

## Communication Protocol Driver
Drivers allow interfacing with connectivity providers and performing standard operations. Depending on the provider 
type, operations may differ and may not necessarily require implementation for certain parts.

A driver's interface is defined by the `AbstractProtocol` class, which implements the following functions:

### getRxUUID
This function returns a unique identifier for the same received data. If the protocol provides an identifier, it is 
worth using it to ensure traceability between the connectivity provider and the platform. Otherwise, a unique 
identifier must be generated to identify duplicates and differentiate newly received data from previous and future data.

```java
protected UUID getRxUUID() {
        return UUID.randomUUID();
}
```

### Data ingestion functions

#### keepHeader
Indicates which headers should be kept in the pivot object; others will be removed. For example, you may want to remove 
the `X-Forwarded-For` header to avoid storing personal data in the pivot object data. Below is an implementation that 
rejects a set of common headers that contain sensitive data and are useless for processing the received data.

```java
protected boolean keepHeader(String headerName) {
    // List of headers to refuse (no interest to keep them)
    String[] headersReject = new String[]{
            "Authorization",
            "X-Forwarded-For",
            "X-Real-IP",
            "Content-Length",
            "Host",
            "Connection",
            "Accept",
            "User-Agent"
    };
    for (String h : headersReject) {
        if (h.equalsIgnoreCase(headerName)) return false;
    }
    return true;
}
```

### toPivot
This function converts the received data into the pivot format. It processes the incoming data stream in its native 
format linked to the connectivity provider and converts it into a common format that the platform can then process. 
At this stage, there is no data conversion per se; data is processed later. It is primarily the metadata that is 
aligned to a standardized representation.

```java 
 /**
     * Convert a data ingested from any endpoint into a pivot objet we can manipulate in a generic way later.
     * This method depends on the protocol (tuple of ingestion method, source, protocol, etc) and must be
     * instantiated in the corresponding class.
     * When an Exception is raised, the frame will not be stored. When you want to store a frame as an error frame,
     * do not report an exception, but use the status field and coreDump fields to store the raw frame in error when
     * expected.
     *
     * @param user User calling the endpoint
     * @param endpoint Corresponding endpoint
     * @param protocol Corresponding protocol
     * @param rawData Raw data received (byte array, data + metadata from the source)
     * @param request HTTP Request info
     * @return
     * @throws ITParseException When the data received does not match the expected format, frame won't be stored
     * @throws ITRightException When a right issue is detected, frame won't be stored
     * @throws ITHackerException When a hacking attempt is detected, frame won't be stored
     */
    public abstract CaptureIngestResponse toPivot(
            String jwtUser,                     // User in the Jwt token (may be an api key)
            User user,                          // User calling the endpoint
            CaptureEndpoint endpoint,           // Corresponding endpoint
            Protocols protocol,                 // Corresponding protocol
            byte[] rawData,                     // Raw data received
            HttpServletRequest request          // HTTP Request info
    ) throws
            ITParseException,                   // Will generate a trace
            ITRightException,                   // No trace
            ITHackerException;                  // Will generated a trace in audit (with caching to not overload)
```

### fallbackResponse
This function handles special cases where it is not possible for the platform to process the received data, for example 
because it is shutting down. In this case, a fallback processing can be developed to avoid losing the received data, 
for example by storing them in a backup storage or sending them to a queue. By default, the data is simply lost.

```java
/**
 * When an injection cannot be handled because it cannot be executed, for example because
 * the service is shutting down, this method provides a generic response. It may also trigger
 * an interruption to fall back to a standard error format for the endpoint.
 *
 * @param ingestResponse
 * @return
 */
public abstract CaptureResponseItf fallbackResponse(
            CaptureIngestResponse ingestResponse
    ) throws
            ITNotFoundException;
```

### Device provisioning functions
