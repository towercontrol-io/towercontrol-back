## User API (see the swagger for more details)

 - `POST` /users/1.0/registration/register : **public**, user registration request, need email confirmation
 - `POST` /users/1.0/creation/create : **public**, user creation request, allowed with email confirmation code
 - `POST` /users/1.0/session/signin : **public**, user login
 - `GET` /users/1.0/session/signout : user logout
 - `GET` /users/1.0/session/upgrade : param (secondFactor) user session upgrade with 2FA code, eula...
 - `GET` /users/1.0/session/2fa : param (secondFactor) to verify the 2FA code before enabling it
 - `GET` /users/1.0/config/ : get the user module configuration for front-end auto adaptation
 - `GET` /users/1.0/profile/basic : get the main user profile information (login, email, first name, last name, etc.)
 - `PUT` /users/1.0/profile/password/change : a logged user can change its password
 - `PUT` /users/1.0/profile/password/reset : change password w/o being logged in, need email confirmation
 - `POST` /users/1.0/profile/password/request : request a password reset, need email confirmation
 - `DELETE` /users/1.0/profile/ : Logical user account deletion, physical deletion will be done later
 - `PUT` /users/1.0/profile/2fa : enable 2fA mechanisms
 - `PUT` /users/1.0/profile/eula : validate / revalidation the user service conditions

