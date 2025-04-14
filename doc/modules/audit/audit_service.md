## Audit service - trace platform operation for later auditing activities


The audit service aggregates information that will later be used to perform an audit of the platform. These operations, 
such as user login, account creation, and password change requests, allow for tracking, detecting abnormal behaviors, 
and analyzing them.

The audit service makes it possible to persist audit information in a database, depending on the `audit.store.medium` 
parameter, which can be mongodb, postgresql, file, or logs. Audits are always formatted as JSON text lines for better 
readability and searchability. The format is as follows:


```json
{
    "service" : String,                     // Service name where the audit logs has been generated
    "action" : String,                      // Action, depends on the service
    "actionMs" : number,                    // Action start time ref, structure creation in ms
    "owner" : String,                       // Owner related, login hash
    "log": String,                          // Log information
    "params" : [ Strings ]                  // One encrypted parameter if != "" will take place in log between {}
}
```

Logs may contain sensitive information, and these will be displayed encrypted by default (Base64) so that they can be 
handled anonymously. It will also be possible to decrypt them if needed.

Log example :
```json
{ "service" : "users", "action" : "registration", "actionMs" : 1697030400000, "owner" : "xxxxxx", "log": "email {0} from ip {1}", "param" : ["eyJ1c2VyIjoiam9obmRvZSIsInBhc3N3b3JkIjoiMTIzNDU2In0=", "eyJ1c2VyIjoiam9obmRvZSIsInBhc3N3b3JkIjoiMTIzNDU2In0="] }
```

Encrypted print example:
```text
2023-10-11 12:00:00.000 [users] [registration] From xxxxx - email eyJ1c2VyIjoiam9obmRvZSIsInBhc3N3b3JkIjoiMTIzNDU2In0= from ip eyJ1c2VyIjoiam9obmRvZSIsInBhc3N3b3JkIjoiMTIzNDU2In0=
```

Decrypted print example:
```text
2023-10-11 12:00:00.000 [users] [registration] From xxxxx - email john.doe@foo.bar from ip 1.1.1.1
```

