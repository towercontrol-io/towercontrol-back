## Implementation example for the user registration page

## Nuxt.js 4
A full implementation for user registration has been made for Nuxt.js 4. The project is accessible on GitHub at the following URL: [towercontrol-front](https://github.com/towercontrol-io/towercontrol-front)
With the following direct links:
- [Registration Request Page](https://github.com/towercontrol-io/towercontrol-front/blob/main/app/pages/front/public/req-register.vue)
- [Registration Completion Page](https://github.com/towercontrol-io/towercontrol-front/blob/main/app/pages/front/public/register.vue)
- [Registration Captcha (Non Community Edition)](https://github.com/towercontrol-io/towercontrol-front/blob/main/app/components/tools/Captcha.vue)
- [User Registration API data structures](https://github.com/towercontrol-io/towercontrol-front/blob/main/app/types/users.ts)
- [User Registration API integration](https://github.com/towercontrol-io/towercontrol-front/blob/main/app/plugins/api.backend.users.ts)

## Step by step communication with backend

### Step 0: Get Configuration (Optional but Recommended)
Before displaying the registration form, query the configuration endpoint to adapt the UI to platform settings.

**Endpoint**: `GET /users/1.0/config/`

**Example Implementation**:
```typescript
    const usersModuleConfigGet: string = '/users/1.0/config/';
    
    /**
     * Get User Module Configuration (public API)
     */
    usersModuleConfig: async (): Promise<{ success?: UserConfigResponse; error?: ActionResult | { message: string } }> => {
        try {
            const response = await apiCallwithTimeout<UserConfigResponse>(
                'GET',
                usersModuleConfigGet,
                undefined,
                true  // Public endpoint
            );
            return { success: response }
        } catch (error: any) {
            return { error };
        }
    }
```

**Response Structure**:
```typescript
export interface UserConfigResponse {
    /** Self registration is allowed */
    selfRegistration: boolean;

    /** Invitation code is required */
    invitationCodeRequired: boolean;

    /** Registration link will be sent by email */
    registrationLinkByEmail: boolean;

    /** User auto-validation is allowed (no admin approval needed) */
    autoValidation: boolean;

    /** EULA validation is required */
    eulaRequired: boolean;

    /** Password minimum size */
    passwordMinSize: number;

    /** Password minimum number of uppercase characters */
    passwordMinUpperCase: number;

    /** Password minimum number of lowercase characters */
    passwordMinLowerCase: number;

    /** Password minimum number of digit characters */
    passwordMinDigits: number;

    /** Password minimum number of symbol characters */
    passwordMinSymbols: number;

    /** Account deletion purgatory delay in hours */
    deletionPurgatoryDelayHours: number;

    /** User can create sub-group under virtual group */
    subGroupUnderVirtualAllowed: boolean;

    /** The running version is Non Community Edition */
    nonCommunityEdition: boolean;

    /** Registration process requires captcha validation (NCE edition) */
    registrationCaptchaRequired: boolean;
}
```

Use this configuration to:
- Show/hide invitation code field
- Display password requirements
- Show/hide EULA checkbox
- Display appropriate messages about account validation

### Step 1: Request Registration
The user fills the registration request form and submits their email address (and optional invitation code).

#### Data Structure for registration request
```typescript
export interface UserAccountRegistrationBody {
    /** Email address for account creation */
    email: string;

    /** Optional invitation code (if required by platform configuration) */
    registrationCode?: string;
}
```

#### Way to submit the registration request to backend
Here is an example of implementation in TypeScript for Nuxt.js 4, but the code can be adapted to other frameworks.

```typescript
    const usersModuleRegistrationRegisterPost: string = '/users/1.0/registration/register';
    
    /**
     * Request User Registration (public API)
     */
    usersModuleRegistrationRegister: async (body: UserAccountRegistrationBody): Promise<{ success?: ActionResult; error?: ActionResult | { message: string } }> => {
        try {
            const response = await apiCallwithTimeout<ActionResult>(
                'POST',
                usersModuleRegistrationRegisterPost,
                body,
                true  // Public endpoint
            );
            return { success: response }
        } catch (error: any) {
            return { error };
        }
    }
```

#### Response from the API
For security reasons, the API always returns `200` with success message, even if:
- The email is invalid
- The email is already registered
- The invitation code is invalid

This prevents attackers from discovering which email addresses are registered in the system.

**Response**: 
- `200` with ActionResult message `user-registration-received`

```typescript
export interface ActionResult {
  /**
   * Result of a given action
   * Example: "OK"
   */
  status: ACTION_RESULT;

  /**
   * Associated custom code, http code
   * Example: 200
   */
  status_code: number;

  /**
   * Associated custom message ready for i18n
   * Example: "user-registration-received"
   */
  message: string;
}
```

**Front-end handling**:
- Display success message to user: "Registration request received, please check your email"
- Inform user to check spam folder if email doesn't arrive
- Optionally provide information about link expiration time

### Step 2: Email Verification
After a successful registration request (valid email, not already registered, etc.), the backend sends an email to the user.
This is managed by the backend, nothing to implement on the front-end side for this step, but here are the details of how it works:

**Email content** (configured on backend):
- Subject: Configurable via backend i18n messages
- Body: Contains a verification link with the format: `{FRONT_BASE_URL}{REGISTRATION_PATH}?verificationKey={VALIDATION_ID}`
- Example: `https://myapp.com/front/public/register?verificationKey=ABC123XYZ789`

**Backend configuration**:
The registration path is configured with the environment variable `USERS_REGISTRATION_PATH`:
```yaml
environment:
  - USERS_REGISTRATION_PATH=/front/public/register?verificationKey=!0!
```
The `!0!` placeholder is replaced by the actual validation ID.

**Validation link properties**:
- The validation ID is a unique identifier for the registration request
- The link expires after a configurable time (default: configured in backend)
- The link can only be used once
- The email address is encrypted and linked to the validation ID in the backend

### Step 3: Complete Account Creation
When the user clicks the verification link, they are redirected to the registration completion page.

#### Extract validation ID from URL
```typescript
// In Nuxt.js, extract from route query params
const route = useRoute();
const validationID = route.query.verificationKey as string;

// Validate that validationID exists
if (!validationID) {
    // Show error: invalid or missing verification key
}
```

#### Data Structure for account creation
```typescript
export interface UserAccountCreationBody {
    /** 
     * Email for account creation (not used for self-registration, kept for future use)
     * Should be empty or null when using validation ID
     */
    email?: string;

    /** User password (mandatory) */
    password: string;

    /** User has checked and accepted conditions validation */
    conditionValidation?: boolean;

    /** Validation ID received from verification link */
    validationID: string;
}
```

#### Validate password requirements
Before submitting, validate the password against the requirements from configuration:

```typescript
function validatePassword(password: string, config: UserConfigResponse): string[] {
    const errors: string[] = [];
    
    if (password.length < config.passwordMinSize) {
        errors.push(`Password must be at least ${config.passwordMinSize} characters`);
    }
    
    const upperCount = (password.match(/[A-Z]/g) || []).length;
    if (upperCount < config.passwordMinUpperCase) {
        errors.push(`Password must contain at least ${config.passwordMinUpperCase} uppercase letter(s)`);
    }
    
    const lowerCount = (password.match(/[a-z]/g) || []).length;
    if (lowerCount < config.passwordMinLowerCase) {
        errors.push(`Password must contain at least ${config.passwordMinLowerCase} lowercase letter(s)`);
    }
    
    const digitCount = (password.match(/[0-9]/g) || []).length;
    if (digitCount < config.passwordMinDigits) {
        errors.push(`Password must contain at least ${config.passwordMinDigits} digit(s)`);
    }
    
    const symbolCount = (password.match(/[^A-Za-z0-9]/g) || []).length;
    if (symbolCount < config.passwordMinSymbols) {
        errors.push(`Password must contain at least ${config.passwordMinSymbols} symbol(s)`);
    }
    
    return errors;
}
```

#### Way to submit the account creation to backend
Before submitting, for Non Community Edition, based on the captcha requirement in the configuration, you may also need to
validate the captcha response. The captcha is an indirect challenge validation. The captcha implementation example is
available in the [Captcha Implementation Example](https://github.com/towercontrol-io/towercontrol-front/blob/main/app/components/tools/Captcha.vue)

The captcha is initiated with a captcha key, using the registration `validationId`, once completed, the registration will
be possible.

```typescript
    const usersModuleCreationCreatePost: string = '/users/1.0/creation/create';
    
    /**
     * Create User Account (public API)
     */
    usersModuleCreationCreate: async (body: UserAccountCreationBody): Promise<{ success?: ActionResult; error?: ActionResult | { message: string } }> => {
        try {
            const response = await apiCallwithTimeout<ActionResult>(
                'POST',
                usersModuleCreationCreatePost,
                body,
                true  // Public endpoint
            );
            return { success: response }
        } catch (error: any) {
            return { error };
        }
    }
```

#### Response from the API
**Success Response**: 
- `201` with ActionResult message `user-creation-created`
- User account has been created successfully

**Error Responses**: 
- `400` with specific error message (i18n key)

Common error messages:
- `user-creation-account-self-creation-not-allowed`: Self-registration is disabled
- `user-creation-missing-password`: Password is missing
- `user-creation-password-rules-matching`: Password doesn't meet requirements
- `user-creation-terms-not-accepted`: EULA acceptance is required but not provided
- `user-creation-refused`: Generic refusal (invalid validation ID, expired link, etc.)
- `user-creation-account-captcha-refused`: Captcha validation failed (NCE edition)

### Step 4: Post-Creation Handling

After successful account creation, inform the user about next steps based on configuration:

#### If auto-validation is enabled (`autoValidation: true`):
```typescript
// User can immediately proceed to login
// Redirect to login page with success message
router.push({
    path: '/front/public/signin',
    query: { message: 'account-created-you-can-login' }
});
```

#### If admin validation is required (`autoValidation: false`):
```typescript
// User must wait for admin approval
// Show message explaining the situation
showMessage('Your account has been created and is pending admin approval. You will receive an email once your account is validated.');
```

## Registration Flow Diagram

```
┌─────────────────────┐
│ Get Configuration   │
│ GET /config/        │
└──────────┬──────────┘
           │
           v
┌─────────────────────┐
│  Fill Registration  │
│  Form (Email +      │
│  optional code)     │
└──────────┬──────────┘
           │
           v
┌─────────────────────┐
│ POST /registration/ │
│      register       │
└──────────┬──────────┘
           │
           v
    ┌──────┴──────┐
    │  Always     │
    │  Success    │
    └──────┬──────┘
           │
           v
┌─────────────────────┐
│  Check Email        │
│  (if valid request) │
└──────────┬──────────┘
           │
           v
┌─────────────────────┐
│ Click Verification  │
│ Link with           │
│ validationID        │
└──────────┬──────────┘
           │
           v
┌─────────────────────┐
│  Fill Account       │
│  Creation Form      │
│  (Password + EULA)  │
└──────────┬──────────┘
           │
           v
┌─────────────────────┐
│Complete the captcha │
│  when enabled       │
│  and Non Community  │
│  Edition            │
└──────────┬──────────┘
           │
           v
┌─────────────────────┐
│ POST /creation/     │
│      create         │
└──────────┬──────────┘
           │
      ┌────┴────┐
      │Success? │
      └────┬────┘
           │
    ┌──────┴──────┬──────────────┐
    │             │              │
    v             v              v
┌────────┐  ┌─────────┐  ┌──────────────┐
│Created │  │ Error   │  │ Show Error   │
│        │  │ 400     │  │ Message      │
└───┬────┘  └─────────┘  └──────────────┘
    │
    v
┌─────────────┐
│Auto Valid?  │
└──────┬──────┘
       │
  ┌────┴────┬──────────┐
  │         │          │
  v         v          v
┌──────┐ ┌──────┐ ┌────────────┐
│ Yes  │ │  No  │ │ Redirect   │
│Login │ │Wait  │ │ to Login / │
│Page  │ │Admin │ │ Show Info  │
└──────┘ └──────┘ └────────────┘
```

## Error Handling

All error responses follow the ActionResult structure with i18n keys. Common error messages include:

### Registration Request Errors (Always returns 200 for security):
Even if there are errors, the API returns success. Internal errors might include:
- Email already registered
- Invalid email format
- Invalid invitation code
- Rate limit exceeded

### Account Creation Errors (Returns 400):
- `user-creation-account-self-creation-not-allowed`: Self-registration is disabled on the platform
- `user-creation-missing-password`: Password field is empty
- `user-creation-password-rules-matching`: Password doesn't meet complexity requirements
- `user-creation-terms-not-accepted`: EULA acceptance is required but checkbox not checked
- `user-creation-refused`: Generic error (invalid/expired validation ID, user already exists, etc.)
- `user-creation-account-captcha-refused`: Captcha validation failed (NCE edition only)

For a complete list of error messages, refer to the [i18n asset file](i18n.md).

## Security Notes

1. **Email Enumeration Prevention**: The registration request endpoint always returns success to prevent attackers from discovering which emails are registered.

2. **Validation Link Security**:
   - Links expire after a configurable time
   - Each link can only be used once
   - Validation IDs are cryptographically random
   - Email addresses are encrypted in the database

3. **Password Security**:
   - Passwords are never stored in plain text
   - Password complexity rules are enforced
   - Rules are configurable per platform

4. **Rate Limiting**: The backend implements rate limiting on registration attempts to prevent abuse.

5. **Invitation Codes**: Optional system to restrict registration to authorized users only.

6. **Admin Validation**: Optional approval step provides an additional security layer for sensitive platforms.

7. **Captcha Support**: Non-Community Edition supports captcha validation to prevent automated bot registrations.

## Front-end Best Practices

1. **Configuration-Driven UI**: Always query the configuration endpoint to adapt the UI dynamically.

2. **Password Validation**: Validate password on the client-side before submission to provide immediate feedback.

3. **Clear Messaging**: 
   - Explain email verification step clearly
   - Inform about link expiration
   - Differentiate between auto-validation and admin approval

4. **Error Handling**: Display meaningful error messages using i18n keys.

5. **Security Awareness**: Don't provide information that could help attackers (e.g., "email already registered").

6. **Accessibility**: Ensure form fields have proper labels, error messages are announced to screen readers.

7. **Mobile-Friendly**: Design forms that work well on mobile devices (especially password input).

