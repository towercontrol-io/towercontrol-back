---
name: skill_user_registration
description: A skill to implement user self-registration with email verification. Based on IoT Tower Control backend.
license: GPL-3.0
metadata:
    author: "disk91"
    version: "1.0.0"
---

# User Self-Registration using the IoT Tower Control backend

## Overview
This skill allows you to implement user self-registration with email verification support. It is based on the IoT Tower 
Control backend, which provides a robust and scalable platform for building IoT platforms. This skill details how to 
create the front-end part of the registration solution and how to integrate it with the backend. The registration process 
is a two-step process: first requesting registration with an email, then creating the account after email verification.

## When to use this skill
When you've been asked to integrate user registration, sign-up, or account creation functionality in the application.

## How to use this skill
This skill helps to generate working code for creating a user registration page. It contains examples in TypeScript, 
made for Nuxt.js 4 but the code can be adapted to other frameworks.

## Principles of the registration process
The registration process is a public endpoint where a user can request account creation using:
- `Email`: The user's email address for account registration and future login.
  - The email should be validated to ensure it is in the correct format.
  - The email is mandatory
  - The email will be verified through a confirmation link
- `Invitation Code` (optional): A code that may be required depending on platform configuration.
  - The invitation code is optional and depends on configuration
  - When required, registration without a valid code will fail

After email verification, the user provides:
- `Password`: The user's password for the new account.
  - The password is mandatory
  - The password must meet complexity requirements (configurable)
  - The password is never stored in plain text on the backend
- `Condition Validation` (optional): User acceptance of terms and conditions.
  - May be required or optional depending on configuration
  - Can be validated during registration or later during first login

The registration process is progressive and handles multiple scenarios:
1. **Registration Request**: User submits email address (and optional invitation code)
2. **Email Verification**: User receives an email with a verification link containing a validation ID
3. **Account Creation**: User creates password and completes registration using the validation ID
4. **Account Validation**: Account may be auto-validated or require admin approval

## Workflow of the registration process
The file [user_registration.md](assets/user_registration.md) contains the workflow of the registration process, with the 
different steps to implement in the front-end and the expected response from the backend. It also contains the structure 
and API calls to be performed with the list of [i18n keys](assets/i18n.md) to use for displaying messages to the user.

As a functional summary:

### Step 1: Request Registration
- The user fills the registration form with their email address
- Optionally, an invitation code if required by the platform
- The form syntax is validated on the front-end
- It is submitted to the backend API endpoint with a `POST` request to `/users/1.0/registration/register`
- The expected response is always `200` with message `user-registration-received`
  - For security reasons, the API always returns success even if the email is invalid or already registered
  - This prevents attackers from discovering which emails are registered

### Step 2: Email Verification
- The user receives an email with a verification link
- The link contains a `validationID` parameter that uniquely identifies the registration request
- The link points to the front-end registration completion page (e.g., `/register?verificationKey=ABC123`)
- The validation link has an expiration time (configurable on the backend)

### Step 3: Complete Account Creation
- The user clicks the verification link and is redirected to the registration completion page
- The page extracts the `validationID` from the URL
- The user fills in additional information:
  - Password (mandatory)
  - Condition acceptance (may be mandatory depending on configuration)
- The form is submitted with a `POST` request to `/users/1.0/creation/create`
- The expected response is `201` when successful with message `user-creation-created`
- Errors return `400` with specific error messages (e.g., `user-creation-password-rules-matching`)

#### Captcha Support (Non Community Edition)
- If captcha validation is required by the configuration, the user must complete a captcha challenge during registration.
- The captcha is associated with the registration key and must be validated before account creation can proceed.
- A Captcha implementation example is [Captcha Implementation Example](https://github.com/towercontrol-io/towercontrol-front/blob/main/app/components/tools/Captcha.vue)

### Step 4: Account Status
- If auto-validation is enabled, the user can immediately able to log in and can be routed to the login page with a success message
- If admin validation is required, the user must wait for admin approval
- The user receives appropriate feedback based on the configuration

## Configuration and Adaptability
The registration process is highly configurable. The front-end should query the configuration endpoint to adapt its behavior:

### Configuration Endpoint: `/users/1.0/config/`
This public endpoint returns configuration parameters that affect the registration UI:
- `selfRegistration`: Whether self-registration is allowed
- `invitationCodeRequired`: Whether an invitation code is required
- `registrationLinkByEmail`: Whether verification is done via email link
- `autoValidation`: Whether accounts are auto-validated or require admin approval
- `eulaRequired`: Whether EULA acceptance is mandatory
- `passwordMinSize`: Minimum password length
- `passwordMinUpperCase`: Minimum uppercase characters required
- `passwordMinLowerCase`: Minimum lowercase characters required
- `passwordMinDigits`: Minimum digit characters required
- `passwordMinSymbols`: Minimum symbol characters required
- `registrationCaptchaRequired`: Whether captcha validation is required (Non Community Edition)

## API endpoints used for user registration
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
- [User Registration API data structures](https://github.com/towercontrol-io/towercontrol-front/blob/main/app/types/users.ts)
- [User Registration API integration](https://github.com/towercontrol-io/towercontrol-front/blob/main/app/plugins/api.backend.users.ts)

### `/users/1.0/config/`
Configuration endpoint to retrieve registration settings.
- It does not require authentication, this endpoint is public
- It accepts a `GET` request
- Returns `200` with configuration object
- The configuration is detailed in file [user_registration.md](assets/user_registration.md)

### `/users/1.0/registration/register`
Initial registration request endpoint.
- It does not require authentication, this endpoint is public
- It accepts a `POST` request with the registration request in the body
- The POST body structure and response structures are detailed in file [user_registration.md](assets/user_registration.md)
- Returns `200` on all cases with message `user-registration-received` (for security reasons)
- The user receives an email with verification link if the request is valid

### `/users/1.0/creation/create`
Account creation endpoint after email verification.
- It does not require authentication, this endpoint is public
- It accepts a `POST` request with the account creation details in the body
- The POST body structure and response structures are detailed in file [user_registration.md](assets/user_registration.md)
- Returns `201` on success with message `user-creation-created`
- Returns `400` on failure with specific error message (i18n key)

## Security considerations
- **Email Privacy**: Registration always returns success to prevent email enumeration attacks
- **Validation Link Expiration**: Email verification links expire after a configurable time
- **Password Complexity**: Backend enforces password rules that should be validated on the front-end
- **Rate Limiting**: Backend implements rate limiting on registration attempts
- **Invitation Codes**: Optional invitation code system to restrict registration to authorized users
- **Captcha Support**: Non-Community Edition supports captcha validation to prevent automated registrations
- **Admin Validation**: Optional admin approval step before users can access the system

This API endpoints are part of the IoT Tower Control backend, the base url should be an environment variable. You may ask 
developer for it when generating the code for the registration page if you don't know it.

If you don't know, please ask developer about the version of the backend, it can be Community Edition or Non Community 
Edition, the registration process is a bit different between the two versions, especially regarding the captcha support.

