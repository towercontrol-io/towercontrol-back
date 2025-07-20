## Intracom Service (obsolete; see inttegration service ... to be reviewed)

The goal of the *intracom* system is to enable communication between modules that may be distributed across multiple servers, 
using a shared medium such as a **database** or an **MQTT broker** or in **memory** in single instance mode. This setup ensures 
that when one module needs to perform an update or trigger a resynchronization, other modules can be notified accordingly and take 
the appropriate action.

The *intracom* service allow to broadcast a communication from one instance of a service to
the other instances of that service. 

The implementation of this service can be made with the MongoDB shared database or with a message queue
broker like mosquitto. In the current implementation only database is implemented. The selection is indicated by the 
following configuration lines in the configuration file:

```properties
# select the intracom medium, currently, only memory is supported, values are db, mqtt, memory
common.intracom.medium=memory
# timeout before deleting the intracom message history from the database ms
common.intracom.timeout_ms=300000

## The following elements are indicative, as they are pre-configured in the code and can be overridden in this way. 
## However, the main goal is to establish a common mechanism for custom services, which can then be described using 
## configuration and naming conventions. These fields allow to list the custom services on top of the common list preset
## in code

## => Pas clair, sur la bonne façon de faire ...

# list the custom services
common.intracom.senders.list=custom1,custom2
# List of actions per services
common.intracom.actions.custom1=load,clear
```
Custom services must declare their own database, sender list, and action list, and call the Intracom initialization function.

The *intracom* service operates without acknowledgements. It functions as a bus onto which messages are broadcast, 
potentially targeting specific recipients, but generally made available to all. Each recipient is responsible for 
checking incoming messages and deciding whether or not to process them. Messages are automatically deleted after a 
certain period.

Each consumer tracks their own message processing progress by comparing the timestamp of the last message they handled 
to the timestamps of newer incoming messages. For this mechanism to work reliably, timestamp synchronization is crucial.
To avoid issues stemming from discrepancies between server clocks, the system uses timestamps provided by the database.

```json
{
    'serviceNameSource' : String,         // Service name for the source related to package (users)
    'serviceNameDest': QueryService,      // Service name for the destination (users)
    'action' : number,                    // Query action, value depends on services
    'query': object,                      // Query parameters as an Object depending on oction
    'query_ts' : number                   // Query start time ref, structure creation in ns
}
```



