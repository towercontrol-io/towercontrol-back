## User data structure
The user data structure is defined as follows:

```json
{
  "id": "string",                 // technical uniq identifier
  "version": "number",            // user structure version
  "login": "string",              // user name for login, this is a hash of the email
  "userSearch": ["string" ],      // Hashes of the 3 first letter of the email + domain for search optimization, can be later extended with other keywords
  "password": "string",           // password hash
  "email": "string",              // user email [Base64(encrypted)]
  "roles": [
     "string"                     // user role collection, role are predefined
  ],
  "acls": [
    { "group": "string", "localName" : "string", "roles" : [ "string"] }   // user acl collection, acl are based on groups and dynamic, name of the groupe can be changed by the user
  ],
  "salt": [ "numbers" ],          // encryption salt 
  "sessionSecret" :  "string",    // session signature salt for token repudiation
  "apiKeys": [                   // List of API session secrets for API accounts, used to sign JWTs and repudiation
    {
      "id": "string",             // API key id, used to identify the right key, 6 hex char, random, unique for a user
      "name": "string",           // API key name, given by user, used to identify the key
      "secret": "string",         // API key secret, used to sign JWTs
      "expiration": "number",     // API key expiration date in MS since epoch
      "roles": ["string"],        // API key groups, list of group names (for quick search later)
      "acls": [                   // API key ACLs, same structure as user acls
        { "group": "string", "localName" : "string", "roles" : [ "string"] }
      ]
    }], 
  "userSecret" : "string",        // secret key computed from password allowing to deactivate user data without removing user
  "lastLogin": "date",            // last login date in MS since epoch
  "countLogin": "number",         // login count
  "registrationDate": "date",     // user creation date in MS since epoch
  "registrationIP": "string",     // IP address of the registration [Base64(encrypted)]

  "modificationDate": "date",     // last user modification date in MS since epoch
  "passwordResetId" : "string",   // randomly generated secret key for password reset
  "passwordResetExp" : "number",  // Expiration for password reset in MS since epoch
  
  "active": "boolean",            // user active flag, means the user is activated, validated by admin or by email confirmation
  "locked": "boolean",            // user locked flag, means we decided after to lock this user, for example after too many bad login
  "expiredPassword": "long",      // password expriration date in MS since epoch
  "apiAccount" : "boolean",       // flag to indicate if user is an API account
  "apiAccountOwner" : "string",   // user login of the owner of the API account
  
  "language": "string",           // user language (ISO country)
  
  "alert preference": {
    "email": "boolean",           // user email alert preference
    "sms": "boolean",             // user sms alert preference
    "push": "boolean"             // user push alert preference 
  },
  
  "profile": {
        "gender": "string",           // user gender to be used [Base64(encrypted)]
        "firstname": "string",        // user first name [Base64(encrypted)]
        "lastname": "string",         // user last name [Base64(encrypted)]
        "phone": "string",            // user phone number e164 format [Base64(encrypted)]
        "address": "string",          // user address [Base64(encrypted)]
        "city": "string",             // user city [Base64(encrypted)]
        "zip": "string",              // user zip code [Base64(encrypted)]
        "country": "string",          // user country ISO code [Base64(encrypted)]
        "customFields": [{            // user custom fields
           "name": "string",          // custom field key [clear]
           "value": "string"          // custom field value [Base64(encrypted)]
        }]
    },
    "billingProfile": {
        "gender": "string",           // user gender to be used [Base64(encrypted)]
        "firstname": "string",        // user first name [Base64(encrypted)]
        "lastname": "string",         // user last name [Base64(encrypted)]
        "companyName": "string",      // user company name [Base64(encrypted)]
        "phone": "string",            // user phone number [Base64(encrypted)]
        "address": "string",          // user address [Base64(encrypted)]
        "city": "string",             // user city [Base64(encrypted)]
        "zip": "string",              // user zip code [Base64(encrypted)]
        "country": "string",          // user country [Base64(encrypted)]
        "countryCode": "string",      // user 2 digit standard country code [Base64(encrypted)]
        "vatNumber": "string",        // user VAT number [Base64(encrypted)]
        "customFields": [{            // user custom fields
          "name": "string",           // custom field key [clear]
          "value": "string"           // custom field value [Base64(encrypted)]
         }]
    },
    "conditionValidation": "boolean",   // user condition validation flag
    "conditionValidationDate": "date",  // user condition validation date
    "conditionValidationVer": "string", // user condition version
    "twoFAType": "enum",                // 2FA type (NONE, EMAIL, SMS, GAUTHENTICATOR)
    "twoFASecret": "string",            // 2FA secret key [Base64(encrypted)]
 
    "lastComMessageSeen": "number",     // last communication message seen
    "customFields": [{                  // user custom fields
        "name": "string",               // custom field key [clear], the one starting with `basic_` are returned in the basic API
        "value": "string"               // custom field value [Base64(encrypted)]
    }]
}
```

### Role definition
Roles are associated to every user and gives user access to particular API endpoints. Roles are stored in the 
session token to avoid verifications on every call. As a consequence, the role refresh requires a session refresh.

- `ROLE_GOD_ADMIN`: this role is the super admin role, it does not give specific permissions but influence the other role to give full access on not owned artifacts
- `ROLE_USER_ADMIN`: this role is the user admin role, it has the right to manage users
- `ROLE_GROUP_ADMIN`: this role manage the group creation, configuration and association of user & devices to groups
- `ROLE_GROUP_LADMIN`: this role allows a user to create and configure an owned group and self assign to it
- `ROLE_DEVICE_ADMIN`: this role allows to register devices
- `ROLE_DEVICE_READ`: this role gives access to owned device data.
- `ROLE_DEVICE_WRITE`: this role gives access to device description metadata modifications.
- `ROLE_DEVICE_CONFIG`: this role allows to modify the device technical configuration
- `ROLE_DEVICE_ALERTING`: this role allows to receive device alerts
- `ROLE_BACKEND_CAPTURE`: this role is dedicated to technical account allowed to report device data to the platform

Following roles are dynamically added to the user profile with the user creation process, they can't be assigned
- `ROLE_PENDING_USER`: this role is assigned to a user not yet registered into the system
- `ROLE_REGISTERED_USER`: this role is assigned to a user registered into the system (confirmed email)

Following roles are dynamically added and should not be stored in the user profile.
- `ROLE_LOGIN_1FA`: this role is assigned on login with 1st factor to allow access 2nd Factor API
- `ROLE_LOGIN_2FA` : this role is assigned on 2nd factor login to indicate user has 2FA. Certain api can expect 2FA and user can apply 2FA as a second step.
- `ROLE_LOGIN_COMPLETED`: this role is assigned when the login process is completed (it should not be stored in role list). It indicates that the user has passed all the needed factors and verifictions
- 
- Role can be added dynamically by some other modules to cover certain authorizations specific for them, see [dynamic role](dynrole_structure.md)

### User Acls

A user has access rights to groups corresponding to the Groups defined elsewhere. A user who does not have a global right may 
still have a specific right on a particular group. By default, a virtual group with the same name as the user exists without 
needing to be explicitly listed; this represents the user's personal group. These personal groups are prefixed with `$user_`, 
which is therefore not allowed for standard group names. This group can be shared via ACLs as well.

### Data encryption
Data are AES encrypted, the encryption key is composed by
- a Server key from parameter `user.server.key` from configuration file, randomly generated
- an Application key from parameter `user.application.key` from Jar file, randomly generated
- a User key from the `userSecret` field, generated from user raw password (field `password` is just a sha-256 hash)

The key is 16 Bytes long (128bits) composed by the 3 keys listed above, all 16 Bytes long and Xored. The userSecret is
generated from the password with PBKDF2 with salt.

### JWT signature
JWT signature depends on
- a Server key from parameter `users.session.key` from configuration file, randomly generated
- a User key from the `sessionSecret` field, randomly generated at user creation and modified after every user signout.

API keys use a specific sessionSecret so as not to be affected by the signout mechanism and to allow individual
repudiation of API keys. For this purpose, an array of `apiSession` is used, and to retrieve them, 
the API key contains a reference key corresponding to the 6 hex characters used as the secret identifier.
When a JWT is received, if it is an API key, the subject allows to find the user and the Id field allows 
to identify the correct entry in `apiSession` to verify the signature with the specific secret for this 
API key.

### Password change
On password change, the data encryption need to be recreated as the `userSecret` will be different.

### 2FA
2FA is based on different systems, it can be email, sms or Google Authenticator application. 
The user can choose to use 2FA or not. If the user chooses to use 2FA, during the login process, the access will be
restricted to the second step FA with password. The `twoFASecret` field is used to store the secret key for the 2FA, 
it can be the Authenticator secret or it can be the temporary code for the SMS or email.

### Custom fields
The structure contains several customizable fields that allow the structure to be extended. These can be 
enriched by the frontend without interpretation by the standard backend, but can also be used by custom 
backends that need extensions without modifying the standard structure.
In the main structure at the root of the user, fields whose name starts with `basic_` are returned in
the basic API. They are intended to store UI configuration information such as a theme color, a choice 
for a dark mode ...