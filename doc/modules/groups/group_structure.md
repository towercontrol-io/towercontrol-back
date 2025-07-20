## Group data structure
The group data structure is defined as follows:

```json
{
  "id": "string",                 // technical uniq identifier
  "version": "number",            // group structure version
  "name": "string",               // group name
  "description": "string",        // group description the user can setup
  "language": "string",           // group language (ISO country), defines the alert message languages, as an exemple
  
  "active": "boolean",            // group active flag, a non active group is like a deleted group but it keeps the references
  "createdBy": "string",          // user login of the creator
  "creationDateMs": "number",     // group creation date in MS since epoch
  "modificationDateMs": "number", // last group modification date in MS since epoch
  
  "virtual": "boolean",           // virtual group flag, this group is a virtual group, it is not stored in the database
  
  "attributes": [                 // list of attributes to be done on the group
    { "type": "string",           // An attribute can be registered to a group for being consumed by a different service
      "params": [ {               // Parameters associated to the attribute
        "key": "string",          // parameter key
        "values": [ "string" ]    // parameter value
        }
      ]
    }
  ],
  
  "referringGroups": [             // list of groups that are referring this group
    { 
      "groupId": "string",        // group id
      "priority" : "number"       // priority of the group, start search from the group with the highest priority, stop on first match
    }
  ]
}
```