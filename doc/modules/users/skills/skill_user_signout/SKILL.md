---
name: skill_user_signout
description: A skill to implement user sign out (logout) functionality with session invalidation. Based on IoT Tower Control backend.
license: GPL-3.0
metadata:
    author: "disk91"
    version: "1.0.0"
---

# User Sign Out (Logout) using the IoT Tower Control backend

## Overview
This skill allows you to implement user sign out (logout) functionality with complete session invalidation. It is based 
on the IoT Tower Control backend, which provides a robust and scalable platform for building IoT platforms. This skill 
details how to create the front-end part of the sign out solution and how to integrate it with the backend. The sign out 
process invalidates all active sessions for the user across all devices by updating the session secret.

## When to use this skill
When you've been asked to integrate user logout, sign out, or disconnect functionality in the application.

## How to use this skill
This skill helps to generate working code for implementing user sign out. It contains examples in TypeScript, made for 
Nuxt.js 4 but the code can be adapted to other frameworks.

## Principles of the sign out process
The sign out process is a simple but critical security feature:
- **Single endpoint call**: One API call to sign out the user
- **Complete session invalidation**: All active sessions across all devices are immediately invalidated
- **JWT token invalidation**: All existing JWT tokens become invalid
- **Session secret renewal**: The backend updates the user's session secret, making all tokens unusable
- **Audit logging**: The sign out action is logged for security auditing

The sign out process:
1. User initiates sign out (clicks logout button, menu option, etc.)
2. Front-end calls the sign out endpoint with the current JWT token
3. Backend invalidates all sessions by renewing the session secret
4. Front-end clears local storage and redirects to public page
5. User must log in again to access protected resources

## Workflow of the sign out process
The file [signout_impl.md](assets/signout_impl.md) contains the workflow of the sign out process, with the different 
steps to implement in the front-end and the expected response from the backend. It also contains the structure and API calls 
to be performed with the list of [i18n keys](assets/i18n.md) to use for displaying messages to the user.

As a functional summary:

### Step 1: Call Sign Out Endpoint
- The user clicks on the sign out/logout button or link
- The front-end calls the sign out endpoint with the current JWT token
- It sends a `GET` request to `/users/1.0/session/signout`
- The expected response is `200` with message `user-signed-out`

### Step 2: Handle Response
- On success (`200`), the backend has invalidated all sessions
- The front-end should immediately:
  - Clear all stored JWT tokens (authentication and renewal tokens)
  - Clear any user data from local storage/session storage/cookies
  - Reset application state to logged-out state
  - Redirect user to login page or public homepage

### Step 3: Handle Errors (Optional)
- On failure (`400`), display error message
- Even if the API call fails, clear local tokens as a safety measure
- Redirect to login page

## API endpoint used for user sign out
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
- [User Session API data structures](https://github.com/towercontrol-io/towercontrol-front/blob/main/app/types/users.ts)
- [User Session API integration](https://github.com/towercontrol-io/towercontrol-front/blob/main/app/plugins/api.backend.users.ts)

### `/users/1.0/session/signout`
Sign out endpoint to invalidate all user sessions.
- It requires a valid JWT token with at least `ROLE_REGISTERED_USER` and `ROLE_LOGIN_1FA`
- It accepts a `GET` request
- The endpoint is detailed in file [signout_impl.md](assets/signout_impl.md)
- Returns `200` on success with message `user-signed-out`
- Returns `400` on failure with error message

**Important security notes**:
- This endpoint invalidates **ALL** sessions for the user, not just the current one
- Any other device where the user is logged in will be logged out immediately
- The user must log in again on all devices to access protected resources
- API accounts (with `ROLE_LOGIN_API`) cannot use this endpoint as they use long-lived tokens

## Security considerations
- **Complete Session Invalidation**: All sessions across all devices are invalidated simultaneously
- **Session Secret Renewal**: The backend generates a new session secret, making all existing JWT tokens invalid
- **JWT Token Structure**: JWT tokens include the session secret in their signature, so renewing it invalidates all tokens
- **No Partial Logout**: There is no way to log out from just one device; sign out is global
- **Immediate Effect**: Token invalidation is immediate and doesn't require waiting for token expiration
- **Audit Trail**: All sign out actions are logged with IP address for security monitoring
- **Front-end Cleanup**: Always clear local storage even if the API call fails
- **API Accounts Exception**: API accounts with long-lived tokens are unaffected by user sign out

## Use cases
- **User-initiated logout**: User clicks logout button
- **Security measure**: Force logout after password change
- **Admin action**: Admin can force user sign out
- **Session expiration**: After timeout, redirect to login (optional)
- **Security breach**: User can logout all devices from one device

This API endpoint is part of the IoT Tower Control backend, the base url should be an environment variable. You may ask 
developer for it when generating the code for the sign out functionality if you don't know it.
