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
- `user.lostpassword.path` : path to the lost password page
- `users.pending.autovalidation` : automatically validate the user after registration
- `users.creation.need.eula` : force the user to accept the EULA during registration
- `users.password.expiration.days` : password expiration in days, 0 means no expiration
- `users.password.min.size` : password minimum size
- `users.password.min.uppercase` : password minimum number of uppercase characters
- `users.password.min.lowercase` : password minimum number of lowercase characters
- `users.password.min.numbers` : password minimum number of digits
- `users.password.min.symbols` : password minimum number of special characters
- `users.password.header` : string to be added to the password to be used for encryption
- `users.password.footer` : string to be added to the password to be used for encryption
- `users.data.privacy.expiration.days` : user data expiration in days, after this decryption will not be possible until relogin, 0 means no expiration
- `users.lostpassword.link.expiration_s` : lost password link expiration in seconds
- `users.cache.max.size` : maximum size of the user cache
- `user.cache.expiration.sec` : user cache entry expiration in seconds, after they are removed
- `user.session.2fa.timeout.sec` : 1FA to 2FA session timeout in seconds
- `users.cache.log.period` : period for cache log as a standard cron string
- `users.session.timeout.sec` : session timeout in seconds, 0 means no expiration
- `users.session.renewal.extra.sec` : session renewal additional time in second
- `users.session.api.timeout.sec` : session timeout for API accounts in seconds, 0 means no expiration
- `user.session.2fa.timeout.sec` : temporary session timeout for 2FA in seconds
- `user.session.key` : common key used for JWT signature, change this value for security or global session invalidation
- `users.session.security.hashmap.size` : size of the hashmap used for risky session storage
- `users.session.security.block.period_s` : if a risky session is identified by not had an attempt during this period, we can clean the entry, in seconds
- `users.session.security.max.ip.failed` : number of accepted failures on a given IP before locking IP
- `users.session.security.max.login.failed`: number of accepted failures on a given LOGIN before locking it.
- `users.superadmin.email`: email of the super admin, first user created in the system automatically when set.
- `users.superadmin.password`: password of the super admin, will be asked for change on first login.
- `users.default.roles` : list of ROLE_ affected by default on user creation, used to fill the default template on non-community version.
- `users.apikey.authorized.roles` ; list of ROLE_ that can be affected to an API key in a global standpoint.