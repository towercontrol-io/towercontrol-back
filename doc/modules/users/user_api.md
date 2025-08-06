## User API (see the swagger for more details)

 - `POST` /users/1.0/registration/register : **public**, user registration request, need email confirmation
 - `POST` /users/1.0/creation/create : **public**, user creation request, allowed with email confirmation code
 - `POST` /users/1.0/session/signin : **public**, user login
 - `GET` /users/1.0/session/signout : user logout
 - `GET` /users/1.0/session/upgrade : param (secondFactor) user session upgrade with 2FA code, eula...
 - `GET` /users/1.0/session/2fa : param (secondFactor) to verify the 2FA code before enabling it

