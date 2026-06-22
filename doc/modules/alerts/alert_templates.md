## Alert Templates

The alert template is a structure that can be modified by the user, and into which UI elements can be inserted. 
It is used to define the content of the messages sent for a given alert type. When an alert is created, it is associated 
with an alert template, which makes it possible to determine how to generate the messages.

The alert template manages all languages and can be modified from the user interface. 

Mediums define how the alert can be sent and what form it will take for each alert type. SMS, email, and push notifications 
are sent at the individual level, while webhooks and topics are sent using settings configured at the level of the associated group.
The group can also have an email, push, and SMS configuration for alerts. In this case, the group will have an alert 
attribute that contains the necessary information.

### Structure AlertTemplate

```json
{
  "id": "string",                 // technical uniq identifier used inside the platform
  "shortId" : "string",           // easier Id to be used for identification (unique, 6 a-Z randomly chosen, with non existance verification)
  "version" : "int",              // structure version for later
  "owner" : "string",             // when a template is not global, it can be owned by a specific user, allowing user filtering
  "global" : "boolean",           // true when the template is global and visible for all
  "name": "string",               // template name, this is the name of the template, given by the user
  "description": "string",        // template description, this is a free text description of the template, given by the user
  "parameters" : [                // list of parameters in the right order, see possible list of parameters (first)
    {
      "type" : "AlertParameter",  // category of the parameter
      "param" : "string"          // associated value when required
    }
  ],
  "open" : [                      // messages sent when the alarm is fired (all mode)
    {
      "locale": "string",              // Applicable local like 'fr','en'..
      "mediums" : [
        { 
          "medium" : "AlertMedium",    // target medium for this alarm (sms, email, push, webhook, topic)
          "message" : "string"         // Generic message to be sent, markdown format parameter excaped with {1} for 1st parameter
        }
      ]
    }
  ],
  "close" : [                      // messages sent when the alarm is ending (mode FIRE_TO_END)
    {
      "locale": "string",              // Applicable local like 'fr','en'..
      "mediums" : [
        {
          "medium" : "AlertMedium",    // target medium for this alarm (sms, email, push, webhook, topic)
          "message" : "string"         // Generic message to be sent, markdown format parameter excaped with {1} for 1st parameter
        }
      ]
    }
  ],
  "behavior" : "AlertBehavior",   // Indicate the default behavior for the alert, namely whether it is one-shot, auto-rearming with a delay, or waiting for cancellation. 
  "preferred" : ["AlertMedium"],  // Preferred medium, in execution order when user have multiple medium enabled
  "durationMs" : "long"           // Alarm duration, the alarm is automatically closed after this duration (mode FIRE_TO_END and FIRE_UNTIL)
}
```

#### AlertMedium

```java
public enum AlertMedium {
    EMAIL,          // Sent with email (user level)
    SMS,            // Sent with short message (user level)
    PUSH,           // Sent with a push message on smartphone (user level)
    WHATSAPP,       // Sent over Whatsapp channel (Not Implemented) (user level)
    POPUP,          // Display the alerts in the web application, asynchronous (user level) - always on
    
    WEBHOOK,        // Sent with a webhook (group level)
    TOPIC,          // Sent with a mqtt / amqp topic (group level)
        
    DEFAULT,        // Used when the channel is not specified
}
```


#### AlertBehavior

```java
public enum AlertBehavior {
    SILENT,         // The Alert is not raiser, but an audit log keep a trace
    FIRE_FORGET,    // Raise the alarm and terminate it, the next trigger will raise a new alarm
    FIRE_TO_END,    // Raise the alarm and wait for termination to FIRE a new one, even if a cancel action is expected an expiration can be set
    FIRE_UNTIL,     // Raise the alarm and wait for termination or given expiration duration to FIRE a new one 
    
    UNKNOWN,
}
```

#### AlertParameter

```java
public enum AlertParameter {
    DEVICE_ID,
    DEVICE_NAME,
    GROUP_NAME,
    USER_FIRSTNAME,
    USER_LASTNAME,
    USER_GENDER,
    ALERT_TIME,             // ex 18:45
    ALERT_DATE_TIME,        // ex 2026-06-08 18:45 (UTC)
    CUSTOM_PARAM,           // alert specific : custom parameter, associated value gives the parameter name (lowercase) to search in user, device, group associated
    SERVICE_NAME,           // Name of the service (from configuration file)
    SERVICE_HOME,           // Service home page (from configuration file)
    ALERT_LINK,             // http link, associated value is the link containing {aid} to be replaced by the alert ID {did} to be replaced by device ID {pubid} for public access
    
    UNKNOWN,
}
```
