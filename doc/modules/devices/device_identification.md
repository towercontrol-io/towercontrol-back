## Device identification

A device will use many identifiers depending on the use cases and to account for the various situations it may encounter:

### Logical Identification

- **Device Id**: The device id is a unique identifier for the device in the platform; it is a *randomly* generated ID that
will be unique for each device structure. It is the primary key to use for interacting with the APIs.
- **Public Id**: This is a random key that can be generated to make this object publicly accessible to anyone who knows 
this key. For example, to track a package, the user can know this key (tracking number) which allows them to access the 
data of this specific object.
- **Hardware Id**: This is an identification associated with the object, hardware-related, and can be used to search for
it. It can be a MAC address, a Sigfox ID, or something else. An object can have multiple hardware identifiers.
- **Near Field Id**: This is an identification associated with the object that will be easily manipulated by the user, 
for example, for onboarding, a QR code, an NFC ID, or RFID; it is randomly generated or assigned during the manufacturing process.
- **Customer Id**: This is a business-related identification, assigned by the user, for example, an order number or an 
ID from another system.
- **Data Stream Id**: This identifier links to a data stream, aiming to decouple the data from the physical object. This way, 
it is possible to clean the data of an object without necessarily deleting it, or to link data even if the object has been 
replaced. This is useful in case of failure, for example.
- **Onboarding Id**: This identifier is used to associate the object during onboarding. Unlike the near field id, this 
onboarding ID is considered a secret, a random value that only the person holding the object can know. This secures 
the ownership of the object. Generally, after use, this code is changed and will be required to reassign ownership to another user.
- **Family Id**: This identifier refers to a family of objects, a free field used to identify the type of object and the 
common behaviors associated with it. It can be a string giving the name of the object or an ID corresponding to a 
specific entity with its own attributes, usable in other modules.
- **Communication Ids**: This is a set of elements enabling communication and network management. For example, on Sigfox, 
you will find the devId and the PAC; on LoRaWan, the deveui, appeui, appkey, etc.

### Network Identification

The network identification depends on the communication protocol used by the device. A device can use multiple.

#### LoRaWan
- DevEUI : type to be used is `LoRa_devEui` and the value is a hexadecimal string of 16 characters (8 bytes), lower case


