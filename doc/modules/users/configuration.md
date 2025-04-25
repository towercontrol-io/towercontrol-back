## User Service Configuration file
The user service configuration file is named `users.properties` and is located in the `configuration` 
directory at the root of the java execution.

### list of properties
- `user.audit.retention.days` : retention period for audit events in days
- `users.registration.self` : authorize the user self registration when true
- `users.registration.with.invitecode` : self registration requires an invite code (not yes implemented)
- `users.registration.email.maxlength` : on registration, limits the email size
- `users.registration.email.filters`: list of email patterns to filter on registration (regex separated by comma)
- `users.registration.link.expiration` : confirmation link expiration in seconds
- `user.registration.link.byemail` : confirmation link send by email 
- `user.registration.path` : path to the registration page with confirmation link
- `users.lostpassword.link.expiration_s` : lost password link expiration in seconds

