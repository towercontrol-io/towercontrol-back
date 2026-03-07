## Capture module

This module is responsible for receiving data from external sensors via different protocols. It can operate in push or 
pull mode with scheduled data collection. It therefore consists of different services that will adapt to the needs of 
each type of interface.

Its role is not to interpret the data which, in general, are converted to a pivot format that can be processed at a 
higher level.

Data are stored in raw form so they can be replayed later and for each data item a unique object will be propagated to 
allow updating the entire chain.

Currently supported integrations:

#### Push

- HTTPS endpoint (REST API) with JSON, authentication using a JWT token derived from an API key. Text format (JSON)

Planned for the future

#### Push

- MQTT(s) with JSON and Protobuf formatting, authentication by client certificate or username/password

#### Pull

- HTTP(s) with JSON and Protobuf formatting, authentication via header (api_key or JWT token)

### Natively supported integrations
- `Helium / Chirpstack` via HTTP(s) with JSON formatting, authentication via header (api_key or JWT token)
- `Sigfox` via HTTP(s) callback, authentication via header (api_key or JWT token)

See (protocols)[protocols.md] documentation for more details on protocols interface and how to implement new ones.

#### How to setup Helium / Chirpstack integration (case of single tenant)
1) Create an API key with the roles `ROLE_GLOBAL_CAPTURE` and `ROLE_BACKEND_CAPTURE` and `ROLE_DEVICE_WRITE`
2) Generate the JWT token for this API key (valid for 10 years for example)
3) Create an endpoint with corresponding protocol, this endpoint is owned by the same user as the JWT owner.
4) On Chirpstack, create an HTTP integration with the following parameters:
   - Endpoint URL: `https://<your_domain>/capture/1.0/ingest/<your_endpoint_id>/`
   - Headers: `Authorization` and value `Bearer <your_jwt_token>`
   - Uplink Data Template: default (you can customize it later if needed)


### Communication ID commissioning

The management of communication identifiers is based on several principles to remain as generic as possible. 
Depending on the object’s lifecycle, communication identifiers will be assigned in different ways.

For example, communication identifiers can be initialized *before* any device is created, completely independently 
of the device itself.

This can be the case with **Sigfox** where, if we are **device maker**, we obtain a list of identifiers even before the 
device is created or flashed. There is therefore a generic storage of available identifiers that can later be used when 
creating a device.

Then, as we move further in the lifecycle, when the object is produced, an assignment takes place. This assignment can 
be done in the manufacturing facility, but it can also be done by using a service that provides the required identifiers.

It can also happen that, at manufacturing time, identification is obtained at that moment because the device includes a 
module that already has its own communication certificates. In that case, the certificates are retrieved from the device.

Subscription can also be activated dynamically later, for example when a device is activated. In that case, it can be 
obtained by querying the platform, or it can be retrieved from the list of active subscriptions.  A subscription can be 
revoked at any time. If it is revoked, it can be reactivated using several approaches: 
- some may choose never to reactivate a past subscription
- others may reactivate the most recently revoked subscriptions
- others may instead prefer the oldest one to be reused.

You can also distinguish between IDs and subscriptions. Telecom IDs are generally associated with subscriptions that
have lifecycles. It may therefore be preferable to use IDs only when they have non-expired subscriptions—for example, 
to perform rotations while avoiding creating too many subscriptions.

This means there is quite a bit of complexity behind the notion of a subscription. 

#### ID Pool
An **ID pool** is a set of identifiers that the platform can draw from when there is a request to assign identifiers. 
This mainly applies when we manufacture devices and subscriptions are either created directly from the platform. This 
can be the case in an ecosystem, or in cases like a **device maker (Sigfox)** where IDs are pre-assigned even before 
manufacturing and before the devices exist.

An ID pool is therefore a set of identifiers that can be used, and the characteristics of these IDs vary depending on 
the protocols. There is a protocol-specific definition of the fields required in the ID pool. The pool also has a 
state: it may have been used and be active; it may be returned; or it may have never been used.

We will keep the dates when IDs are assigned and the dates when they were created. We can also store information about 
the start date of the active subscription for a given ID. These dates make it possible to decide which ID to assign—for 
example, selecting an ID that is available but whose subscription is still active, in order to assign it to another 
device. 

The ID pool aggregates, in a generic way, all ID information coming from all protocols. However, each protocol can 
automatically update the ID pool. The ID pool can be loaded from the front-end, either via single uploads or via CSV-type 
documents enabling bulk uploads. 

##### ID Pool 

```json
{
   "id": "string",                     // technical uniq identifier used inside the platform
   "protocolId": "string",             // protocol associated with this ID pool, must be defined in the system
   "captureId": "string",              // capture id associated with this ID pool, must be defined in the system
   "configTypeId": "string",           // configuration type associated with this ID, multiple config types are possible for a given protocol
   "customConfig": [{                 // Custom configuration specific to the protocol and type
      "key" : "string",               // Protocol-specific configuration parameters
      "value" : "string"
   }],                                // List of mandatory fields required to configure this protocol
   "state": "enum",                    // state of the ID in the pool, [NOT_ASSIGNED, ASSIGNED, IN_USE, RETURNED, EXPIRED_RETURNED, EXPIRED_IN_USE, REMOVED]
   "creationMs": "long",               // Creation timestamp in milliseconds since epoch
   "updateMs": "long",                 // Update timestamp in milliseconds since epoch - for batch processing, not update too often
   "assignedMs": "long",               // Assignment timestamp in milliseconds since epoch
   "releasedMs": "long",               // Release timestamp in milliseconds since epoch (when the ID is returned to the pool)
   "subscriptionStartMs" : "long",     // Subscription start timestamp in milliseconds since epoch (when the subscription associated with this ID becomes active)
   "subscriptionEndMs" : "long",       // Subscription end timestamp in milliseconds since epoch (when the subscription associated with this ID expires)
   "removalMs" : "long",               // Removal timestamp in milliseconds since epoch (when the ID is removed from the pool, for example because it is expired or dead)
   "creationBy" : "string"             // Creator identifier 
}
```

The possible states are as follows:

- `NOT_ASSIGNED` indicates that the ID is not currently linked to any *device*.
- `ASSIGNED` indicates that the ID is assigned to a *device*, but potentially not associated with an active telecom subscription.
- `IN_USE` indicates that the ID is assigned and used in an active subscription.
- `RETURNED` indicates that the ID is returned to the pool and can therefore be reassigned to another *device*; however, the subscription may still be active.
- `EXPIRED_RETURNED` indicates that the subscription is no longer active for a returned ID.
- `EXPIRED_IN_USE` indicates that the subscription is no longer active for an ID that is still assigned to a device, which may indicate a problem with the device or the subscription.
- `REMOVED` indicates that the ID must no longer be used, but can be kept for historical reasons. 

These states are maintained by the software when an ID is assigned or returned. They can also be maintained asynchronously 
by the protocol to update state. Updating these states does not necessarily affect the operation of devices, which have 
their own internal logic and check their subscription status on their own.

##### ID Pool ID definition
To automatically process the parameters required for each protocol, the expected formats for IDs are described in the 
protocol definition as follows: 
