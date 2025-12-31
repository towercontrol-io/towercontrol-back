## Device data structure
The device data structure is defined as follows:

```json
{
  "id": "string",                 // technical uniq identifier usined inside the platform
  "version": "number",            // group structure version
  
  "instanceOfId" : "string",      // When this device is an instance of a given device, this makes the link with the upper
                                  // layer device reference. else null.
  
  "publicId" : "string",          // public id, is used when we want to publically share device information, this id is a 
                                  // secret the user must know to read access the data of this device.

  "hardwareIds" : [               // hardware id, this is the id used to identify the hardware device in the platform
    { "type" : "string", "id" : "string" } // is can be a MAC address, a sigfox ID or whatever...
  ],        
                                  
  "nearFieldId" : "string",       // near field id, this is the id used to identify the device like an RFID/NFC/QR code tag
                                  // null or empty if not used

  "customerId": "string",         // customer id, this is the id used to identify the customer in the platform
                                  // it can be a business identifier 


  "dataStreamId": "string",       // data stream id, this is the id used to identify the data stream in the platform
                                  // multiple physical devices can be associated to the same data stream, like when the device is swapped
                                  // also, when the device is re-affected to another customer, the data stream id is preserved for letting 
                                  // the user access it's previous data
  
  "onboardingId": "string",       // onboarding id, this is the id used to identify the onboarding process in the platform
                                  // it can be a PAC number, a QR code or whatever... This is a secret [encrypted]

  "familyId": "string",           // family id, this is the id used to identify the family of the device in the platform
                                  // family have the same beahvior, as an exemple, same decoders.

  "name": "string",               // device name, this is the name of the device, given by device owner or manager
  "description": "string",        // device description, this is a free text description of the device, given by device owner or manager
  "icon" : "string",              // device icon, is a icon name to be used in front-end to display the device.
  
  "deviceState" : "devState",     // Current device state, see the device life cycle management section for more information
  "deviceStateMs": "number",      // Current device state date in MS since epoch, this is the date of the last state change
  
  "lastSeenMs": "number",          // last seen date in MS since epoch, this is the last time the device was seen by the platform
  "lastRestartMs": "number",       // last restart date in MS since epoch, this is the last time the device was restarted
  
  "firmwareVersion": "string",    // firmware version, this is the version of the firmware running on the device
  "hardwareVersion": "string",    // hardware version, this is the version of the hardware running on the device

  "tags" : ["string"],            // List of associated tags. Tags are used for device common actions, like firmware update. 
                                  // Allowing to have different behavior / version per tag, like having a Beta group ... 
  "fuotaProtocolId" : "string",   // Driver Id for the FUOTA, this will decide the device rules for operating the firmware updates
  
  "batteryType" : "batType",      // type of battery used in the device, this allows some autonomy calculation
  "batteryCapacity" : "number",   // capacity of the battery used in the device, this allows some autonomy calculation, in mAh
  "batteryLowLevel" : "number",   // low level of the battery used in the device, when 0, a default base on type applies, in mV
  "batteryLevel" : "number",      // level of the battery used in the device, in mV
  
  "createdBy": "string",          // user login of the creator
  "createdOnMs": "number",        // device creation date in MS since epoch
  "updatedBy": "string",          // user login of the updater
  "updatedOnMs": "number",        // device update date in MS since epoch
  
  "billingPeriodStartMs": "number", // billing period start date in MS since epoch
  "billingPeriodEndMs": "number",   // billing period end date in MS since epoch
  "billingGroupId": "string",       // billing group id, this is the id used to identify the billing group. This is a Group
                                    // where the biling information are attached
  
  "dynamicLocation": "boolean",   // dynamic location flag, this is the flag used to identify if the device is a dynamic location device
                                    // when true, the device position can be updated based on network information dynamically.
  "location": {                   // Position of the device
    "referential" : "string",       // location referential, this is the referential used to define the location (global when null or empty or custom map...)
    "locationMs" : "number",       // location date in MS since epoch, this is the date of the last location change
    "latitude" :  "number",         // device known latitude
    "longitude" : "number",         // device known longitude
    "altitude" :  "number",         // device known altitude from sea level (m)
    "floor" : "string",             // device floor when applicable
    "accuracyM" :  "number",        // location accuracy in meters
    "hexLocation" : "string"        // hex location, this is the hex representation of the location (uber h3)
  },
  
  "dataEncrypted": "boolean",     // device data is encrypted flag, this is the flag used to identify if the device data is encrypted
                                    // The encryption information are stored as part of the attributes.
  
  "communicationIds": [            // list of communication ids, this is the list of communication ids used by the device
    { "type": "string",           // An attribute can be registered to a group for being consumed by a different service
      "params": [ {               // Parameters associated to the attribute
        "key": "string",          // parameter key
        "values": [ "string" ]    // parameter value
      }
      ]
    }
  ],
  
  "attributes": [                 // list of attributes to be associated with the device
    { "type": "string",           // An attribute can be registered to a device for being consumed by a different service
      "params": [ {               // Parameters associated to the attribute
        "key": "string",          // parameter key
        "values": [ "string" ]    // parameter value
        }
      ]
    }
  ],
  
  "associatedGroups": [            // list of groups the device is associated with
    { 
      "groupId": "string"         // group id
    }
  ]
}
```