## Dynamic role data structure

Any module can register roles on startup, role can be added to users

```json
{
  id: "string",            // technical uniq identifier
  version: "number",       // role structure version 
  name: "string",          // role name ROLE_EX_MODULENAME_ROLENAME
  description: "string",   // role description for front-end internationalisation role-ex-modulename-rolename-desc
  enDescription: "string", // role description in english for reference
  platform: "string",      // true when the role is declared by platform (not custom, user)
  assignable: "boolean",   // true if the role can be assigned to users, false if the role is assigned dynmically like LOGIN

  "creationBy": "string",   // id of the user who created the role (in particular for custom roles)
  "creationMs": "number"    // creation date in MS since epoch
}
```

## Api for role data structure

- A module on startup requests for ROLE addition (POST) or REMOVAL (DELETE) using the API endpoint. This is not a human 
interaction but a system interaction. Only new Role are added.
- Role UPDATE (PUT) does not allow to modify name. DELETE will cascade on users.
- Dynamic roles are listed with all the standard role through the GET
