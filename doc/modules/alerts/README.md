## Alerts module

The alerts module provides an autonomous notification system used by the platform to broadcast information to users when 
a business rule or a technical event matches a trigger.

The module is intentionally independent of the modules that generate the trigger. A source module detects a condition, 
then sends an alert intent to the alerts' module. From that point, the alerts module handles:
- alert persistence
- target group resolution
- user fan-out
- delivery channel selection
- localization and message rendering
- retry / fallback selection
- history and audit traceability

## Purpose

An alert is a message sent in response to an event. It can be issued in different ways, is associated with a triggering event
and sent to an identified group, as a consequence all users belonging to that group with alerts enabled and having `ROLE_DEVICE_ALERTING`

The same alert can be delivered through different channels:
- email
- SMS
- push notification
- ...

The channel list requested by the alert is a preference list, not a strict requirement. If the preferred channel is not 
available for a user, the module must select another usable channel as a fallback.

## Core principles

- Alerts are group-based, not user-based.
- The module must remain independent from the source module that created the trigger.
- The same logical alert can be emitted once, repeatedly, or kept active until a stop condition is met.
- Alerts are persisted in the database so they can be queried, purged, and used to rebuild the operational history.
- Each emission must create an audit entry.

## Alert flow

1. A source module detects a condition.
2. The source module creates or updates an alert record with the group, message key, parameters, desired channels, and lifecycle information. Alert is stored in the database for async processes.
3. A process in the alert module will handle pending alerts asynchronously.
4. The alerts module resolves the target group members.
5. For each user, the module selects the first compatible delivery channel.
6. The message is rendered with the appropriate translation and the best matching parameters.
7. The alert is delivered.
8. The alert emission and its processing outcome are written to the audit trail.

## Delivery model

Delivery is performed per user, even if the alert itself is stored at group level.

For a given user, the delivery engine evaluates the requested channels in priority order. The first usable channel is 
selected according to:
- the channels requested by the alert
- the channels enabled for the user
- the platform availability of the delivery provider

If the preferred channel cannot be used, the module falls back to another available channel. When multiple channels are 
available, the module should keep a deterministic selection order to avoid inconsistent user experience.

## Message localization

An alert message is not stored only as a final text. It is stored as a translatable message key with parameters so that 
the final rendering can be adapted to the delivery medium and to the user language.

The message rendering process must support:
- language selection from translation files
- parameter substitution
- channel-specific message variants when required
- reuse of the same parameters across channels when possible

When a channel-specific template cannot reuse the full parameter list, the module should keep the first parameters that 
match the target template to avoid losing critical information.

## Alert structure

```json
{
  "id" : "string",                 // technical uniq identifier used inside the platform
  "targetedGroups" : ["string"],   // targeted Groups for this alert
  "alertId" : "string",            // stable alert identifier, used to identify the same alert across multiple emissions, see bellow for more details
  "alertDefRef" : "string",        // reference to the alert definition, ex : dev_DEVICEID to refer the device, source of the alert, grp_GROUPID for group alerts
  "alertTemplateId" : "string",    // reference to the alert template, this is used to link the alert to the message key and parameters
  
  "state" : "AlarmState",          // Alarm state: indicates whether it is **active**, **terminated**. 
  "requestMs" : "long",            // Moment the alarm has been raised (event time)
  "fireMs" : "long",               // Moment the alarm has been raised (alert execution time)
  "expirationMs" : "long",         // Moment the alarm expired (rearm the alarm)
  "error" : "string",              // In case of error making alert impossible to process
  
  "sent": [                        // Sent confirmation by medium with ack when available
    {
      "userId" : "string",         // One for each of the targeted user
      "state" : [
        {
          "medium": "AlertMedium",     // what medium has been used
          "sent": "boolean",           // alert has been sent
          "ack": "boolean",            // sending confirmation when possible
          "error": "string"            // in case of error keep a trace
        }
      ]
    }
  ],
  "publicAccessId" : "string"      // 24 char Random secret to access this alert from a public page
}
```

### Alert identifier `alertId`

The alert identifier will be used to retrieve an alert, avoid duplicates, and manage alert resolution. An alert identifier 
is made up of a string containing a replaceable field, which can be a **device** or a **group ID**, so that a re-emission 
by the same device or the same group ID is correctly considered the same alert.

The device identification is used to raise an alert originating from a specific device. The group ID identification is 
used when a set of devices raises the same alert. For example, in an intrusion detection scenario with multiple sensors, 
one of the sensors will raise the alert, and the alert will be raised only once, not every time a detector reacts.

An alert cache will use the delayed reference to manage access to active alerts.

Here are examples of alert identifiers, in a generic and instantiated format.

```
"alert-temperature-high-{deviceId}" -> "alert-temperature-high-123456789"
"alert-intrusion-detected-{groupId}" -> "alert-intrusion-detected-987654321"
```

### Alert templates

Alerts are associated with a template that, among other things, contains the translations of the different messages, 
as well as the integration of the variables expected in the error message and the alert message, in order to generate 
the text according to the different transmission channels.

See [Alert templates](./alert_templates.md) for more details on the alert template structure and management.

## Alert lifecycle

Alerts can have several lifecycle modes.
- PENDING : the alert has been detected and wait for being executed
- PENDING_QUEUE : the alert has been transferred to the waiting queue (technical state) ; en restart the alert in this state is reset to PENDING
- FIRED : the alert has been proceeded 
- RUNNING : when the alert is waiting a end signal to be rearmed
- ENDING : when the alert received the end-event and waiting to be processed
- ENDING_QUEUE : Same principle as PENDING_QUEUE
- ENDED : when the alert is completed and ready for being deleted

### `SILENT` alerts
A silent alert just keep a trace in the audit log, it goes from PENDING to ENDED.

### `FIRE_FORGET` alert
A one-shot alert is emitted once and can be sent again immediately if the source module creates a new alert instance.
This alert will be duplicated if state is PENDING and will directly go to ENDED after that.

### `FIRE_TO_END` alert

Once the alert is triggered, it moves to the `PENDING` state, and if a new alert of the same type is triggered, 
it will not be taken into account.

Once the alert is processed, it moves directly to the `RUNNING` state, waiting for an alert end-event. 
This wait can be an expiration or an explicit request to end the alert.

During this time, the alert cannot be re-armed; any new incoming alert will be rejected. Once the end-event is 
received, the alert moves to `ENDING` then after being reported `ENDED` state.

Expiration will not raise a message.

### `FIRE_UNTIL` alert

In this mode, once the alert has been submitted, it remains in the `PENDIUNG` state until it is processed. It then 
automatically moves to the `RUNNING` state until the alert expires. When the alert expires, it is set to the 
`ENDED` state, and at that point, a new alert can be submitted. 

### `RUNNING` state

An `RUNNING` alert is associated with a condition that remains true over time.

While the condition is active, the module must avoid emitting duplicate alerts every time the source module reports the 
same state. The alert is only emitted again when the stop condition is reached and a new activation cycle starts.

Example:
- temperature rises above 30°C -> alert is activated and emitted
- temperature stays at 31°C or 32°C -> no new emission
- temperature drops below the stop threshold -> alert is closed, potentially with a resolution message
- temperature rises again -> a new alert emission is allowed

An running alert can also define an expiration period. This approach makes it possible to have an alert that is not 
disarmed, but that can expire after a certain amount of time, so that a new alert can be triggered and generate a 
new message.

For example, if a refrigerator door is left open, the alert can be sent immediately with an expiration after 5 minutes.
If the opening is detected again during the next 5 minutes, no additional alert is sent. But after 5 minutes, a 
reminder is issued, and so on every 5 minutes.


### `RUNNING` to `ENDED` state

An alert can be explicitly deactivated by its source module or automatically closed by a stop condition.

When an alert uses an expiration, the module must still respect the configured lifetime and must clear the alert 
when the alert is canceled, expired, or purged.

## Persistent storage

Alerts are stored in the database. The persistent record is used for:
- current state tracking
- delivery retry and fallback handling
- search and operational monitoring
- history reconstruction
- purge processing

The stored alert should keep, at minimum:
- alert identifier
- source module reference
- attached group identifier
- message key
- message parameters
- selected channels
- alert type
- activation state
- creation timestamp
- last emission timestamp
- reminder / expiration timestamp when applicable
- delivery status
- processing status

Including after an alert expires, it is retained in the database for historical purposes and will be purged once 
the `alerts.max.retention.ms` duration is reached.

## Specific medium

### POPUP
In the case of a message sent via a pop-up medium, the alert is displayed when the user logs in. There may 
be a set of historical alerts that the user may have missed, so the pop-up messages are stored in a table in order 
to access this history. The table can be purged once the messages have been read, since the alert history is maintained 
in the alert table, which can be consulted until the purge date is reached.

## Operational history

The alert history is preserved through the audit log and thus the audit module so that operators can understand what was 
emitted, when it was emitted, and to whom it was delivered.

Processed (and terminated) alerts can be deleted from the alerts table after a configurable period so that the alert table 
does not become too large.

## Audit traceability

Every alert emission must generate an audit log entry.

The audit entry should record at least:
- the source of the alert
- the attached group
- the selected delivery channel
- the result of the delivery attempt
- the processing timestamp

This traceability is mandatory even when the final delivery fails, because the alert activity itself is part of the system history.

## Medium selection

For alert communication to the user, the appropriate medium must be selected. The user has chosen possible contact alert 
channels, and the alert template defines the priority order for those channels.

We proceed by selecting the highest-priority medium from the template and checking whether the user authorizes it. 
If so, it is selected. Otherwise, we move to the secondary medium and check whether it is active for the user, and so on.

If the alert template does not define a priority for the mediums, the mediums will be processed in the following order: 
starting with email, then SMS, and then the other systems in the order in which they are found.

If none of the media defined on the template match the user’s available channels, or if no media are defined on the 
template, we use the user’s medium, preferably the one for which a template is available.

If no template is available, we prioritize email, then SMS, and then another solution.

## Alert message composition

The alert template provides the information needed to complete the message. A number of parameters are passed in the
messages and must be replaced. These parameters correspond to predefined fields or custom fields that can be used, 
depending on how the alert was created.

## Translation files

Alert messages should be defined in translation files so the same alert can be displayed in different languages.

The translation keys should be short, stable, and independent from the delivery channel. Channel-specific wording can be 
handled by separate keys when necessary.

Example concept:
```json
{
  "alert-temperature-high": "Temperature is above the configured threshold: {value}°C",
  "alert-temperature-high-sms": "High temperature detected: {value}°C"
}
```


