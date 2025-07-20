## Service Integration Layer

The goal of the *integration* system is to enable communication between modules that may be distributed across multiple servers,
using a shared medium such as a **database** or an **MQTT broker** or in **memory** in single instance mode. This setup ensures
that when one module needs to perform an update or trigger a resynchronization, other modules can be notified accordingly and take
the appropriate action.

The *integration* service allow to broadcast a communication from one instance of a service to
the other instances of that service.

The implementation of this service can be made with the MongoDB shared database or with a message queue
broker like mosquitto or in memory Queue when the system dioes not have multiple instances. 
In the current implementation only memory is implemented. The selection is indicated by the
following configuration lines in the configuration file:

The service integration layer allows communication between service and 
between instances of services. This is a common interface, integrated to all the 
services and not a specific service. It manages the communications using different 
methods to make the deployment flexible as a monolith or as independent, scalable, services.

Service integration is mostly asynchronous, synchronous call are polling the asynchronous API.

### Available services
A service can configure a list of services to be joined and to join other services.
The configuration is made in the service configuration file.

### Query API
Messages and responses are encapsulated into a common message.

```json
{
    'queryId': String,                    // Uid of the message to link with response
    'serviceNameSource' : QueryService,   // Service name for the source related to package (users)
    'serviceNameDest': QueryService,      // Service name for the destination (users)
    'route': QueryRoute,                  // Route to use for the message, db, memory, mqtt, based on service config
    'type': QueryType,                    // Query type, fire & forget, broadcast, async, sync...
    'action' : number,                    // Query action, value depends on services
    'query': object,                      // Query parameters as an Object depending on oction
    'response': ActionResult,             // Result of the Query, sucess or error like Exception
    'result' : object,                    // Query response as an Object depending on action
    'state' : QueryState,                 // Query state, STATE_PENDING, STATE_DONE, STATE_ERROR
                                          // Query access request parallelism support
    'query_ts' : number,                  // Query start time ref, structure creation in ns
    'response_ts' : number,               // Response time ref, in ns
    'query_ms' : number,                  // Query timestamp in ms for timeout
    'timout_ms': number                   // Timeout (duration) in ms for the query
}
```

Integration is natively asynchronous, API will transform asynchronous call into synchronous call when preferred.
The following mechanism is used to manage the asynchronous call:
- The service sends a query message to the integration service
- The service wait for response (polling the `state` field)
- Integration service complete the query_ts timestamp, it route de message according to the configuration:
  - If the destination service is local, integration directly call the sync query function. 
    - The query is updated with the response, response_ts and state are updated
  - If the destination service is remote, integration send the message to the remote service using the proposed route MQTT or API (async)
    - Integration service stores the pending query.
    - The remote integration service process the response and call the async response handler
    - The async response handler updates the query, response_ts and state 
  
### Integration Service

Integration service stores the pending query in an hashmap based on the messageId.



#### local
The service communicates directly calling the Java Api of the service hosted locally.

