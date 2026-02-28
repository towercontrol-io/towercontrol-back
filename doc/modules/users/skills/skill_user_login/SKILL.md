---
name: skill_user_login
description: A skill to implement user authentication (login/signin) with optional 2FA support. Based on IoT Tower Control backend.
license: GPL-3.0
metadata:
    author: "disk91"
    version: "1.0.0"
---

# User Login & Authentication using the IoT Tower Control backend

## Overview
This skill allows you to implement user authentication (login/signin) with support for two-factor authentication (2FA). 
It is based on the IoT Tower Control backend, which provides a robust and scalable platform for building IoT platforms. 
This skill details how to create the front-end part of the authentication solution and how to integrate it with the backend. 
The login process is a multi-step process that handles various scenarios including password expiration, user condition 
validation, and 2FA authentication.

## When to use this skill
When you've been asked to integrate user authentication, login, or signin functionality in the application.

## How to use this skill
This skill helps to generate working code for creating a user authentication page. It contains examples in TypeScript, 
made for Nuxt.js 4 but the code can be adapted to other frameworks.

## Principles of the login process
The login process is a public endpoint where a user can authenticate using:
- `Email`: The user's email address used as login identifier.
  - The email should be validated to ensure it is in the correct format.
  - The email is mandatory
- `Password`: The user's password for authentication.
  - The password is mandatory
  - The password is never stored in plain text on the backend

The authentication process is progressive and handles multiple scenarios:
1. **Basic Authentication (1FA)**: Initial login with email and password
2. **Password Expiration**: User may need to change expired password before full access
3. **User Conditions**: User may need to accept updated terms and conditions
4. **Two-Factor Authentication (2FA)**: Additional security layer if enabled
5. **Session Upgrade**: Complete authentication process to gain full access

## Workflow of the login process
The file [user_login.md](assets/user_login.md) contains the workflow of the login process, with the different 
steps to implement in the front-end and the expected response from the backend. It also contains the structure and API calls 
to be performed with the list of [i18n keys](assets/i18n.md) to use for displaying messages to the user.

As a functional summary:

### Step 1: Initial Authentication (Sign In)
- The user fills the login form with email and password
- The form syntax is validated on the front-end
- It is submitted to the backend API endpoint with a `POST` request to `/users/1.0/session/signin`
- The expected response is `200` and it contains a JWT token with authentication status indicators:
  - `jwtToken`: The authentication token for API calls
  - `jwtRenewalToken`: A longer-lived token for session renewal
  - `passwordExpired`: Indicates if password change is required
  - `conditionToValidate`: Indicates if user conditions need to be accepted
  - `twoFARequired`: Indicates if 2FA is needed
  - `twoFAValidated`: Indicates if 2FA has been completed

### Step 2: Handle Authentication Constraints
After initial authentication, the user may need to:
- **Change expired password** when response has `passwordExpired` set to true : Use `/users/1.0/profile/password/change` endpoint
- **Accept user conditions** when response has `conditionToValidate` set to true: Use `/users/1.0/profile/eula` endpoint
- **Complete 2FA** when response has `twoFARequired`set to true : Use `/users/1.0/session/upgrade` endpoint with `secondFactor` parameter

### Step 3: Session Upgrade
- After resolving any constraints (password, conditions, 2FA), the user calls the upgrade endpoint
- Send a `GET` request to `/users/1.0/session/upgrade` with optional `secondFactor` query parameter
- The expected response is `200` with an updated JWT token when all conditions are met
- When all conditions are met, the `twoFAValidated` flag is true and the user has full access

### Step 4: Session Management
- **Sign Out**: Use `/users/1.0/session/signout` endpoint to invalidate all user sessions
- **Token Renewal**: Use the `jwtRenewalToken` to get a new authentication token without re-authentication

## API endpoints used for user authentication
The API endpoints are relative to an API base URL that is provided via an environment variable. In a Nuxt.js implementation, 
this variable is defined in the `nuxt.config.ts` file as follows:

```typescript
export default defineNuxtConfig({
  ...
  runtimeConfig: {
    public: {
      BACKEND_API_BASE: 'http://localhost:8091',  // Backend API base URL
      ...
    }
  },
```

The best way to implement the interface with the backend is to use the libraries available here (in TypeScript for Nuxt.js):
- [User Authentication API data structures](https://github.com/towercontrol-io/towercontrol-front/blob/main/app/types/users.ts)
- [User Authentication API integration](https://github.com/towercontrol-io/towercontrol-front/blob/main/app/plugins/api.backend.users.ts)

### `/users/1.0/session/signin`
Main authentication endpoint for user login.
- It does not require authentication, this endpoint is public
- It accepts a `POST` request with the login credentials in the body
- The POST body structure and response structures are detailed in file [user_login.md](assets/user_login.md)
- Returns `200` on success with JWT tokens and authentication status
- Returns `400` on failure with error message (i18n key: `user-login-refused`)

### `/users/1.0/session/upgrade`
Session upgrade endpoint to complete authentication process.
- It requires a valid JWT token with at least `ROLE_LOGIN_1FA`
- It accepts a `GET` request with optional `secondFactor` query parameter
- Returns `200` with updated JWT token when conditions are met
- Returns `400` on failure with error message

### `/users/1.0/session/signout`
Sign out endpoint to invalidate user sessions.
- It requires a valid JWT token with `ROLE_REGISTERED_USER`
- It accepts a `GET` request
- Returns `200` on success with confirmation message (i18n key: `user-signed-out`)
- Returns `400` on failure with error message

### `/users/1.0/profile/password/change`
Password change endpoint for authenticated users.
- It requires a valid JWT token
- It accepts a `PUT` request with old and new passwords
- Returns `200` on success with confirmation message (i18n key: `user-profile-password-changed`)
- Returns `400` on failure with error message

### `/users/1.0/profile/eula`
User conditions validation endpoint.
- It requires a valid JWT token
- It accepts a `PUT` request to accept the current user conditions
- Returns `200` on success with confirmation message (i18n key: `user-profile-eula-accepted`)
- Returns `400` on failure with error message

## Security considerations
- **Brute Force Protection**: The backend implements brute force protection on login attempts per user and per IP
- **JWT Token Security**: All JWT tokens must be prefixed with `Bearer` keyword when used in API calls
- **Session Invalidation**: Sign out invalidates all active sessions for the user
- **No Error Details**: For security reasons, authentication failures return generic error messages

This API endpoints are part of the IoT Tower Control backend, the base url should be an environment variable. You may ask 
developer for it when generating the code for the authentication page if you don't know it.

