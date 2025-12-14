## Device life cycle management
The way a device evolved all along the time is a key element of an IoT platform.
The lifecycle of an object can begin well before its manufacturing, and the order of its states can vary from one 
implementation to another. 

There is business process management behind lifecycle handling, which makes it both complex 
and central to an IoT platform. The concept of lifecycle and states is not necessarily a sequence of states but can 
also be an accumulation of states. Each state change requires traceability because the state is important for actions 
performed by the business process. 

In the case of a replay, it is crucial to ensure the correct state is identified.

### Device states
- **Identified**: The device IDs have been created into the platform.
- **Associated**: The device is associated to a physical device, the physical device exists
- **Stored**: The device is stored in a warehouse deactivated
- **Commissioned**: The device has been declared on the telco networks, telco IDs are associated.
- **Affectable**: The device can be affected to a user or a group
- **Affected**: The device is affected to a user or a group, it can be related to an order
- **Activated**: The device is activated on the communication networks and able to send data
- **Open**: The device data can flow into the platform

- The following states are cumulative, so we use state **action pending** and state **action done** and the application
will manage the different situations.
  - **Upgradable**: The device can be upgraded to a new version
  - **Upgraded**: The device has been upgraded to a new version - this is an acknowledgeable state
  - **Configurable**: The device have a new configuration pending
  - **Configured**: The device configuration has been applied - this is an acknowledgeable state

- The state **Out of subscription** is decomposed into 2 states to better manage the data flow:
  - **Out of network Subscription** : The device won't receive data from network until subscription is renewed
  - **Out of User Subscription** : The device won't send data to platform until subscription is renewed

- **Defective**: The device is defective and need to be replaced.
- **Lost**: The device is lost.
- **Returnable**: The device can be returned to the manufacturer or the solution provider.
- **Returned**: The device has been returned to the manufacturer or the solution provider.
- **Trashed**: The device is trashed.
- **Recycled**: The device is recycled (destroyed not reused).

### Device transitions
We can match theses cumulative states with a standard device lifecycle but the purpose is to have it more open. Here is an
example of device lide-cycle story:
- The solution provider wants to produce 1000 devices to the factory/manufacturer. 1000 devices are created and are in the 
state `Identified:true`. IDs are generated and the manufacturer can produce them.
- Once the manufacturer has produced one of the device, the device add the state `Associated:true`. The device hardware ID 
is added to the device profile.
- The devices are tested on the production line, they can eventually be `Defective:true` or they can be `Affectable:true` 
once in stock are ready to be affected to a user or a group.
- Later a customer order 1 of these devices, it will get the `Affected:true` state and `Affectable:false`. These state are 
mutually exclusive ... or not.
- The device needs to have network connectivity, it will be `Activated:true`. 
- Until the end of the customer subscription, the device will now have `Open:true` state to allow the data routing.
- If the device is defective, it will be `Defective:true` and `Open:false` a replacement will be done. The device data ID will be 
transferred to the new device. It can be `Returnable:true` if the manufacturer wants to get it back and `Returned:true` if it has been
returned.
- If the device is remove from one customer to be later affected to another one it gets the `Affected:false` state 
and `Affectable:true` with `Open:false` state. The device data ID is preserved for the previous user.
- A `Lost:true` device is a non communicating device which can be later restored, it may be replaced but not returned.
- At the end of the lifecycle, the device can be `Trashed:true` and/or `Recycled:true`.
