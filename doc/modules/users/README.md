## User Management module
This module is responsible for managing users in the system. It provides the following features:
- User registration request - prefilter user registration with email validation, public API.
- User registration - User creation (slef, authorized by a validation code or admin managed)
- User profile
- User role
- User acl management
- User login 
- User cache management

## Related Documentation
- [User data structure](./user_structure.md)
- 

### User life cycle
A user can be created manually by an administrator or after a self registration. User creation have different steps depending on the selected path:
- self registration, enabled by `users.registration.self` property:
    - User is registering with an email, until the email has been confirmed, the user does not exist in the system but the
      registration request is stored in a pending table. When `users.registration.with.invitecode` is set to true, the user must provide
      an invite code to register. The email address is verified and filtered (see user creation security)
    - when `user.registration.link.byemail` is true, a confirmation email is sent to the user with a secret to validate and create the user. The expiration time for the invitation code
      is defined by `users.registration.link.expiration` parameter in seconds.
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

* User signout updates the `sessionSecret` value for token repudiation.
* API accounts have a long life JWT token, the expiration is set to 1 year by default, but can be configured by the `user.api.token.exp` parameter.
  ROLE_USER_ADMIN can create apiAccount and generate JWT for them with an API endpoint.
* Accounts pending creation are stored in a separate table that is progressively cleaned up—either because the accounts have been created
  or because they did not pass the next stage and have been rejected.

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

#### User login

When a user logs in, their login and password will be verified, and it must be ensured that the account is active 
and has at least the role `ROLE_REGISTERED_USER`. If the user has not logged in for a very long period, determined 
by the `users.data.privacy.expiration.days` parameter, their personal data will no longer be decryptable. However, 
logging in will resolve this situation by renewing the keys necessary for decryption, based on their plaintext 
password, which only they know. The login process is also an opportunity to check if the password needs to be 
renewed if it has expired.

### traceability
Event on user service are logged into an audit table. It includes
- login event
- logout events
- password change
- email sent (not content, just event)
- authorization addition and removal
- group association and removal
- profile modification date
- rekeying event
- dekeying event
- condition validation history
- communication message seen

The event table is stored in the database and can only be accessible with
technical access in a first time. In the future it can be available with a UI.
A purge system is in place to remove old events, the retention period is defined
by the `user.audit.retention.days` parameter.

