## User Management module
This module is responsible for managing users in the system. It provides the following features:
- User registration request - prefilter user registration with email validation, public API.
- User registration - User creation (self, authorized by a validation code or admin managed)
- User login - User authentication with password and 2FA
- User profile
- User role
- User acl management
- User cache management

## Related Documentation
- [User data structure](./user_structure.md)
- [User configuration fields](./configuration.md)
- [User roles attribution](./dynrole_structure.md)
- [User specific i18n keys](./i18n.md)

### User life cycle
A user can be created manually by an administrator or after a self registration. User creation have different steps depending on the selected path:
- self registration, enabled by `users.registration.self` property:
    - User is registering with an email, until the email has been confirmed, the user does not exist in the system but the
      registration request is stored in a pending table. When `users.registration.with.invitecode` is set to true, the user must provide
      an invitation code to register. The email address is verified and filtered (see user creation security)
    - when `user.registration.link.byemail` is true, a confirmation email is sent to the user with a secret to validate and create the user. The expiration time for the invitation code
      is defined by `users.registration.link.expiration` parameter in seconds. The email contains a link to the front-end validation page defined by `user.registration.path` parameter, 
      when this parameter is empty, the validation code will be sent directly.
    - Following the confirmation email, the user sets the minimal information (passwords, condition validation eventually), to create the user structure and assign
      the `ROLE_PENDING_USER` role. The user condition is set with the parameter (db) `users.condition.version` when existing and set in body ; parameter `users.creation.need.eula`
      force the user condition (eula) validation for the user creation.
    - User can be redirected to profile configuration or will do it later on its own base on boolean parameter `user.pending.forceprofile`
    - The user can then be validated by an administrator to assign the `ROLE_REGISTERED_USER` role or automatically moved to that status
      depending on the configuration boolean parameter `users.pending.autovalidation`
- Administrator registration:
    - User is created by an administrator with a `ROLE_PENDING_USER` role.
    - On first login, user will have to change its password.
    - User can be redirected to profile configuration or will do it later on its own base on boolean parameter `user.pending.forceprofile`
    - The user can then be validated by an administrator to assign the `ROLE_REGISTERED_USER` role or automatically moved to that status depending on the configuration boolean parameter `user.pending.autovalidation`

User can delete his account, also after a given period of inactivity, the user account will be frozen by removing the `userSecret` value. This value is used for encrypting user information,
as a consequence, without a new login of the user with the right password, the data will stay encrypted and not accessible, even for the platform administrator.  The frozen period is decided by the `user.max.inactivity` parameter.

* User signout updates the `sessionSecret` value for token repudiation ; all running session for that user are canceled.
* User session JWT token have an expiration defined by `users.session.timeout.sec` default 10 hours.
* API accounts have a long life JWT token, the expiration is set to 30 years by default (value 0) defined by `users.session.api.timeout.sec`.
  ROLE_USER_ADMIN can create apiAccount and generate JWT for them with an API endpoint.
* Accounts pending creation are stored in a separate table that is progressively cleaned up—either because the accounts have been created
  or because they did not pass the next stage and have been rejected.

#### User account deletion
Admin can delete a user account but standard user can only delete its account. 
Account deletion is virtual in a first step. The account has its userKey cleared to make sure the 
user personal data won't be accessible anymore. The account will be completely deleted after `users.deletion.purgatory.duration_h`
hours with an external schedule. This allows an admin to restore an account on user request. User login
will be mandatory for restoring the personal data. The user won't be able to recreate an account until 
a full destroy. User login won't be possible after this action until admin restore. 
When the deletion purgatory period (`users.deletion.purgatory.duration_h`) is set to 0, the account is 
immediately destroyed.

#### User email messages
The user service is able to send email messages to users. The email content is defined in the configuration file and can be
modified by the administrator. The email content is defined in the `users.messages_xx.properties` files.

#### User creation security

During the user creation phase, Email verification is performed based on different rules:
- email size, should not exceed `users.registration.email.maxlength` parameter
- email format, should match email standard format
- email structure, should not match any of the `users.registration.email.filters` regex patterns, separated by `,`

#### Password creation rules

The password creation & change must respect the rules defined in the configuration files and set by the following 
variables (or the env var equivalent):
- `users.password.min.size` : (USER_PASSWORD_MIN_SIZE) - minimum password size ; default 8
- `users.password.min.uppercase` : (USER_PASSWORD_MIN_UPPERCASE) - minimum number of uppercase characters
- `users.password.min.lowercase` : (USER_PASSWORD_MIN_LOWERCASE) - minimum number of lowercase characters
- `users.password.min.numbers` : (USER_PASSWORD_MIN_NUMBERS) - minimum number of numbers
- `users.password.min.symbols` : (USER_PASSWORD_MIN_SYMBOLS) - minimum number of symbols

To protect the password against dictionary attacks, the password hash uses a per-user salt, randomly generated at user. To avoid
a database attack exposing the salt and the hash of the password, the password can be altered with a pre-string `users.password.header` 
and a post-string `users.password.footer` to be added to the password before hashing. The pre-string and post-string are stored in configuration
file not be exposed to database attack.

#### User login

When a user logs in, their login and password will be verified, and it must be ensured that the account is active 
and has at least the role `ROLE_REGISTERED_USER`. If the user has not logged in for a very long period, determined 
by the `users.data.privacy.expiration.days` parameter, their personal data will no longer be decryptable. However, 
logging in will resolve this situation by renewing the keys necessary for decryption, based on their plaintext 
password, which only they know. The login process is also an opportunity to check if the password needs to be 
renewed if it has expired.

When the user have 2 factors authentication (2FA) enabled, the login process will be done in 2 steps, after the first step,
user will get `ROLE_LOGIN_1FA` role. It needs to complete the second step for accessing a full login. Once he has made the 
2FA authentication, he will gain the `ROLE_LOGIN_2FA` role and the `ROLE_LOGIN_COMPLETE`. The user have `user.session.2fa.timeout.sec`
to complete the 2FA authentication. If the user does not complete the 2FA authentication in time, the session will be closed.

When the user has 1FA authentication only, it will directly get the `ROLE_LOGIN_COMPLETE` role. In any case, before getting this role,
the user must have a non expired password and signed for the user conditions. If not, the user will only have the `ROLE_LOGIN_1FA` 
or the `ROLE_LOGIN_2FA` role and `ROLE_REGISTERED_USER` role util it has completed the login process and the condition validation.

Once connected, the user obtains 2 JWT tokens. The first is the authentication token, which contains the list of functional roles 
in addition to the dynamic roles linked to the connection. This token will be used for all API calls until it expires. 
It is possible to renew the token using the `upgrade` API. Once the token has expired, it is possible to request a new 
token using the renewal token. The renewal token is valid for `users.session.renewal.extra.sec` additional seconds 
and allows the authentication token to be renewed without having all functional rights.

For being used, the JWT token must be preceded by the `Bearer` keyword. 

#### Brute force protection on user login

When a new login or session upgrade is received, we verify the login attempt is
lower than the limit fixed by `users.session.security.max.login.failed` for the user and
`users.session.security.max.ip.failed` for the IP. The number of trial per IP is higher
due to the possible multiple users behind a NAT in companies or university. This only
counts the failures. When the failure max is reached, the next attempts are blocked
and the 2FA code if exist is reset.
If the hashmap size limit `users.session.security.hashmap.size` has been reached, all the errors make 
the 2fa to be reset. On every 5 minutes, the cache is cleaned to remove the old entries, the one with
the last access time lower than `users.session.security.block.period_s`

#### Email 2FA

When a user has their 2FA enabled with email, during login, a 4-digit hexadecimal code will be randomly 
generated and sent by email. The user must then enter this code within the 2FA session time 
`user.session.2fa.timeout.sec`, even if the session is extended. After this duration, the code will be 
invalidated. The **brute force** protections described above apply to prevent this type of attack.

### Personal data protection

The user personal data are encrypted into the database with AES encryption. The encryption key is composed of 3 parts:
- a Server key from parameter `common.encryption.key` from configuration (common.properties) file, randomly generated
- an Application key from parameter `common.application.key` from Jar (resources/application.properties) file, randomly generated. 
- a User key from the `userSecret` field, generated from user raw password (field `password` is just a sha-256 hash, it's not the source)

The combination of these 3 keys gives the encryption key. Any loss of one of keys will avoid any further data decryption. The purpose is to
avoid the decryption of the data from one single data breach. The second purpose is to allow personal data removal after a given period of time
when a user stopped to use the service. Practically, `users.data.privacy.expiration.days` parameter defines the period of time after which the
user data will be made inaccessible. This is done by removing the userSecret value. The userSecret is used for encrypting user information, so without
this key the data can't be restored anymore. When the user decides to reconnect the system, the `userSecret` is restored from the plain text password and the 
data can be decrypted again. As the password is never stored, the `userSecret` can't be restored without the user action.

### Super Administrator

When the application is launched for the first time, an initial user is created if the `users.superadmin.email` and 
`users.superadmin.password` parameters are provided. This user will be created with the `ROLE_GOD_ADMIN` role and 
will be the super administrator of the application. The password must be changed during the first login.

For security reasons, this creation will only occur during the first launch of the application. Afterwards, it will 
no longer be possible to automatically create the super administrator unless the `users.superadmin.created` parameter 
is deleted from the database.

### traceability
Event on user service are logged into an audit table. It includes
- Self registration request
- User Account creation
- Login event
- Logout event
- Password change
- Dekeying event
- Rekeying event

- email sent (not content, just event)
- authorization addition and removal
- group association and removal
- profile modification date
- condition validation history
- communication message seen

The event table is stored in the database and can only be accessible with
technical access in a first time. In the future it can be available with a UI.
A purge system is in place to remove old events, the retention period is defined
by the `user.audit.retention.days` parameter.

