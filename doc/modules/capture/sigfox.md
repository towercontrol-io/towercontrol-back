# Sigfox specific integration

To connect and integrate with the Sigfox network, it is important, from Sigfox’s point of view, that callbacks are 
implemented and that they match the expected format. The callbacks are sent via `POST`, and the body content must be 
exactly as follows.

A JWT token must be created to integrate the data into the API. The same JWT token can be reused for all callbacks.

## Service > Advanced

This callback is the one that will trigger data processing because it is the most complete callback, the one that arrives 
last. It is therefore the callback that is definitely required, but it will not be sufficient if we want to handle
`downlinks`, which will require a bidir callback used only for data handling.

### Headers
The JWT token required for authentication can be created via the `API Key` menu with the `ROLE_GLOBAL_CAPTURE` and 
`ROLE_BACKEND_CAPTURE` roles. In case you want to manage security at group level, the user does not have the global 
capture role, they must have the `ROLE_DEVICE_WRITE` role on the group to which this device belongs plus the
`ROLE_BACKEND_CAPTURE`. If your need to create new device dynamically, role `ROLE_DEVICE_ADMIN` is required.

- Authorization: `JWT`
- Content-Type: `application/json`

### Body content
```json
{ 
  "type" : "srv_advanced", 
  "device" : "{device}", 
  "time" : {time}, 
  "data" : "{data}", 
  "seq" : {seqNumber},
  "lqi" : "{lqi}",
  "operatorName" : "{operatorName}", 
  "countryCode" : {countryCode}, 
  "computedLocation" : {computedLocation}, 
  "duplicates" : {duplicates}, 
  "version" : 1
}
```

## Data >> BiDir

The BDIR callback will be implemented when we want to handle downlinks. In that case, when a BDIR callback is received, 
the driver will check whether there is a pending downlink action, and that is the only operation that will be performed. 
Is the data processed through the `advanced` callback.

### Headers

The JWT token required for authentication can be created via the `API Key` menu  with the `ROLE_BACKEND_CAPTURE` role.

- Authorization: `JWT`
- Content-Type: `application/json`

### Body content
```json
{
  "type" : "data", 
  "device" : "{device}",
  "time" : {time},
  "seq" : {seqNumber},
  "ack" : {ack},
  "duplicate" : false,
  "data" : "{data}", 
  "version" : 1
}
```

## Service >> Status

The status callback provides information about events associated with devices. It is optional and is currently not handled.

### Headers

The JWT token required for authentication can be created via the `API Key` menu  with the `ROLE_BACKEND_CAPTURE` role.

- Authorization: `JWT`
- Content-Type: `application/json`

### Body content
```json
{
  "type" : "srv_status", 
  "device" : "{device}", 
  "station" : "{station}",
  "seq" : {seqNumber},
  "duplicate" : false,
  "temp" : {temp}, 
  "batt" : {batt}, 
  "time" : {time},
  "version" : 1
}
```
