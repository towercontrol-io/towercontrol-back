## Implementation example for the user login page

## Nuxt.js 4
A full implementation for user authentication has been made for Nuxt.js 4. The project is accessible on GitHub at the following URL: [towercontrol-front](https://github.com/towercontrol-io/towercontrol-front)
With the following direct links:
- [Login Page](https://github.com/towercontrol-io/towercontrol-front/blob/main/app/pages/front/public/login.vue)
- [Login Component](https://github.com/towercontrol-io/towercontrol-front/blob/main/app/components/users/LoginForm.vue)
- [User Authentication API data structures](https://github.com/towercontrol-io/towercontrol-front/blob/main/app/types/users.ts)
- [User Authentication API integration](https://github.com/towercontrol-io/towercontrol-front/blob/main/app/plugins/api.backend.users.ts)

## Step by step communication with backend

### Step 0: Correctly understand the authentication specificities 
Depending on how the backend is configured, the authentication behavior may vary, so it is important to know the backend 
configuration to correctly implement the user interface.

The configuration can be retrieved via the endpoint `GET /users/1.0/config/` and contains the following fields that 
impact the authentication workflow to be implemented in the front-end:

```typescript
export interface UserConfigResponse {
 /**
   * Self registration is allowed
   * Example: true
   */
  selfRegistration?: boolean

  /**
   * Invitation code required
   * Example: true
   */
  invitationCodeRequired?: boolean

  /**
   * Registration link will be sent by email
   * Example: true
   */
  registrationLinkByEmail: boolean

  /**
   * User auto-validation is allowed / admin must not manually validate the user
   * Example: true
   */
  autoValidation: boolean

  /**
   * EULA validation is required
   * Example: true
   */
  eulaRequired: boolean

  /**
   * Password minimum size
   * Example: 8
   */
  passwordMinSize: number;

  /**
   * Password minimum number of upper case characters
   * Example: 0
   */
  passwordMinUpperCase: number;

  /**
   * Password minimum number of lower case characters
   * Example: 0
   */
  passwordMinLowerCase: number;

  /**
   * Password minimum number of digit characters
   * Example: 0
   */
  passwordMinDigits: number;

  /**
   * Password minimum number of symbols characters
   * Example: 0
   */
  passwordMinSymbols: number;

  /**
   * Account deletion, purgatory delay in hours (0 means immediate deletion)
   * Example: 24
   */
  deletionPurgatoryDelayHours : number;

  /**
   * User can create a sub group under the virtual group
   * Example: true
   * Required
   */
  subGroupUnderVirtualAllowed: boolean;

  /**
   * The backend supports the non-community edition features
   * Example: false
   * Required
   */
  nonCommunityEdition: boolean;

  /**
   * Registration process requires a captcha validation (NCE edition)
   * Example: false
   * Required
   */
  registrationCaptchaRequired: boolean;
};
```

- **selfRegistration** is true when users are allowed to register and sign-in form can have a link to the 
signup process.
- **passwordMinSize** is the minimum size required for passwords, the front-end should validate the password format 
before sending it on password renewal. No need to check on login form. Value `0` means that there is no minimum size requirement.
- **passwordMinUpperCase**, **passwordMinLowerCase**, **passwordMinDigits** and **passwordMinSymbols** are the minimum 
number of uppercase, lowercase, digit and symbol characters required in the password. The front-end should validate the 
password format before sending it on password renewal. No need to check on login form. Value `0` means that there is no minimum size requirement.


### Step 1: Initial Authentication (Sign In)
The following structure is filled by the front-end and `POST` to backend API endpoint `/users/1.0/session/signin`. 
The response contains JWT tokens and status indicators for the authentication process.

#### Data Structure for signing request
```typescript
export interface UserLoginBody {
    /** User email address used as login identifier */
    email: string;

    /** User password for authentication */
    password: string;
}
```

#### Way to submit the login to backend
Here is an example of implementation in TypeScript for Nuxt.js 4, but the code can be adapted to other frameworks.

```typescript
    const usersModuleSessionSigninPost: string = '/users/1.0/session/signin';
    
    /**
     * User Sign In (public API)
     */
    usersModuleSessionSignin: async (body: UserLoginBody): Promise<{ success?: UserLoginResponse; error?: ActionResult | { message: string } }> => {
        try {
            const response = await apiCallwithTimeout<UserLoginResponse>(
                'POST',
                usersModuleSessionSigninPost,
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
The response from the API contains the JWT tokens and status indicators for the authentication process. 
The response code is `200` when the authentication is successful or when the user needs to complete additional steps.

When the response code is `400`, the authentication failed and a structure ActionResult is returned, with 
the error message contained in field `message` and corresponding code in field `status_code`.

```typescript
export interface UserLoginResponse {
    /** User email address */
    email?: string;

    /** User login (hashed identifier) */
    login?: string;

    /** JWT Token for API authentication, must be prefixed with 'Bearer ' */
    jwtToken: string;

    /** JWT Token for session renewal, has longer expiration time */
    jwtRenewalToken: string;

    /** True if the password is expired, user must change it */
    passwordExpired: boolean;

    /** True if user conditions need to be accepted */
    conditionToValidate: boolean;

    /** True if 2FA is required to complete authentication */
    twoFARequired: boolean;

    /** True if 2FA has been successfully validated */
    twoFAValidated: boolean;

    /** Expected 2FA code size (e.g., 6 for authenticator apps) */
    twoFASize: number;

    /** Type of 2FA configured (EMAIL, AUTHENTICATOR, SMS) */
    twoFAType: 'EMAIL' | 'AUTHENTICATOR' | 'SMS';
}

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
   * Example: "user-login-refused"
   */
  message: string;
}
```

### Step 2: Handle Authentication Constraints

After initial authentication, the front-end should check the response flags to determine the next steps, it is possible
that multiple flags are true at the same time, in this case the front-end should handle them iteratively, starting with 
password expiration, then conditions acceptance and finally 2FA.

#### Case 1: Password Expired
If `passwordExpired` is true, the user must change their password before proceeding.

**Endpoint**: `PUT /users/1.0/profile/password/change`

**Condition**: the endpoint requires authentication, so the JWT token received in the login 
response must be included in the request headers with `BEARER` prefix.

**Request Body**:
```typescript
export interface UserPasswordChangeBody {
  /**
   * User new password  
   * @example "changeme"  
   * @required
   */
  password: string

  /**
   * User Password change authorization key (for the public endpoint)  
   * @example "jhjfkhqsldjkfhsqljkdhfljsqdhfazuheufhazjkfnds"  
   * @optional
   */
  changeKey?: string
};
```
- **password** contains the new password that the user wants to set. 
It must respect the password rules defined in the configuration (e.g., minimum size, character requirements).
- **changeKey** is not required and must be empty in this situation.

**Response**: 
- `200` with ActionResult message `user-profile-password-changed` on success
- `400` with error message on failure (e.g., `user-profile-password-not-valid`)

#### Case 2: Conditions to Validate
If `conditionToValidate` is true, the user must accept the updated user conditions.

**Endpoint**: `PUT /users/1.0/profile/eula`

**Condition**: the endpoint requires authentication, so the JWT token received in the login
response must be included in the request headers with `BEARER` prefix.

**Request Body**:
The request body is empty for this endpoint.

**Response**: 
- `200` with ActionResult message `user-profile-eula-accepted` on success
- `400` with error message on failure

#### Case 3: Two-Factor Authentication
If `twoFARequired` is true, the user must provide the 2FA code. The type of 2FA is indicated by `twoFAType`:
- **EMAIL**: A code has been sent to the user's email
- **AUTHENTICATOR**: User must enter code from authenticator app
- **SMS**: User will receive code via SMS (if supported)

The user will complete 2FA in Step 3 using the session upgrade endpoint.

### Step 3: Session Upgrade
After resolving any constraints (password change, condition acceptance, or receiving 2FA code), 
the user must upgrade their session to gain full access.

**Endpoint**: `GET /users/1.0/session/upgrade?secondFactor={code}`

**Query Parameters**:
- `secondFactor` (optional): The 2FA code when 2FA is required

**Example Implementation**:
```typescript
    const usersModuleSessionUpgradeGet: string = '/users/1.0/session/upgrade';
    
    /**
     * Upgrade User Session (requires valid JWT with ROLE_LOGIN_1FA)
     */
    usersModuleSessionUpgrade: async (secondFactor?: string): Promise<{ success?: UserLoginResponse; error?: ActionResult | { message: string } }> => {
        try {
            const url = secondFactor 
                ? `${usersModuleSessionUpgradeGet}?secondFactor=${encodeURIComponent(secondFactor)}`
                : usersModuleSessionUpgradeGet;
            
            const response = await apiCallwithTimeout<UserLoginResponse>(
                'GET',
                url,
                undefined,
                false  // Requires authentication
            );
            return { success: response }
        } catch (error: any) {
            return { error };
        }
    }
```

**Response**: 
- `200` with updated `UserLoginResponse` when successful
- `400` with error message on failure (e.g., `user-session-2fa-code-invalid`, `user-upgrade-brute-force`)

The upgrade should be called iteratively:
1. After password change → Call upgrade without secondFactor
2. After condition acceptance → Call upgrade without secondFactor
3. After receiving 2FA code → Call upgrade with secondFactor

When `twoFAValidated` is true in the response, the authentication is complete and the user has full access.

### Step 4: Complete Authentication
When all constraints are resolved:
- `passwordExpired` is false
- `conditionToValidate` is false
- `twoFARequired` is false OR `twoFAValidated` is true

The user is fully authenticated and can access all authorized endpoints using the new `jwtToken`.
this token should be stored securely on the front-end (e.g., in memory, secure cookie) and included in the 
`Authorization` header for subsequent API calls.

### Step 5: Session Management

#### Sign Out
To invalidate all user sessions and sign out:

**Endpoint**: `GET /users/1.0/session/signout`

**Example Implementation**:
```typescript
    const usersModuleSessionSignoutGet: string = '/users/1.0/session/signout';
    
    /**
     * User Sign Out (requires valid JWT)
     */
    usersModuleSessionSignout: async (): Promise<{ success?: ActionResult; error?: ActionResult | { message: string } }> => {
        try {
            const response = await apiCallwithTimeout<ActionResult>(
                'GET',
                usersModuleSessionSignoutGet,
                undefined,
                false  // Requires authentication
            );
            return { success: response }
        } catch (error: any) {
            return { error };
        }
    }
```

**Response**: 
- `200` with ActionResult message `user-signed-out` on success
- `400` with error message on failure

#### Token Renewal
The `jwtRenewalToken` has a longer expiration time and can be used to obtain a new `jwtToken` without requiring the user 
to sign in again. Use the same upgrade endpoint without parameters when the main token expires.

## Authentication Flow Diagram

```
   ┌─────────────────┐
   │  Submit Login   │
   │ (email/password)│
   └────────┬────────┘
            │
            v
   ┌─────────────────┐
   │   POST /signin  │
   └────────┬────────┘
            │
            v
       ┌────┴────┐
       │ Success?│
       └────┬────┘
            │
       ┌────┴────────────────┬─────────────────┬──────────────┐
       │                     │                 │              │
       v                     v                 v              v
   ┌─────────┐       ┌─────────────┐   ┌─────────────┐  ┌────────┐
   │Password │       │ Conditions  │   │  2FA        │  │  Full  │
   │Expired? │       │ to Validate?│   │  Required?  │  │ Access │
   └────┬────┘       └──────┬──────┘   └──────┬──────┘  └────────┘
        │                   │                 │
        v                   v                 v
   ┌─────────┐       ┌─────────────┐    ┌─────────────┐
   │ Change  │       │   Accept    │    │   Receive   │
   │Password │       │  Conditions │    │  2FA Code   │
   └────┬────┘       └──────┬──────┘    └──────┬──────┘
        │                   │                  │
        └──────────┬────────┴──────────────────┘
                   │
                   v
            ┌──────────────────┐
            │ GET /upgrade?2fa │
            └──────┬───────────┘
                   │
                   v
            ┌──────────────┐
            │ Full Access  │
            └──────────────┘
```

## Error Handling

All error responses follow the ActionResult structure with i18n keys. Common error messages include:

- `user-login-refused`: Invalid email or password
- `user-profile-password-not-valid`: Password does not meet requirements
- `user-profile-key-not-valid`: Invalid password reset key
- `user-session-2fa-code-invalid`: Invalid 2FA code
- `user-session-2fa-code-missing`: 2FA code is required but not provided
- `user-upgrade-brute-force`: Too many failed attempts, user is temporarily blocked
- `user-profile-condition-not-found`: User conditions are not configured
- `user-logout-refused`: Sign out failed

For a complete list of error messages, refer to the [i18n asset file](i18n.md).

## Security Notes

1. **Brute Force Protection**: The backend tracks failed login attempts per user and per IP address. After reaching the 
limit, additional attempts are blocked for a configured period.

2. **JWT Token Usage**: Always prefix the JWT token with `Bearer ` when making API calls:
   ```typescript
   headers: {
     'Authorization': `Bearer ${jwtToken}`
   }
   ```

3. **Session Invalidation**: Calling the sign out endpoint invalidates all active sessions for the user, including other devices.

4. **2FA Timeout**: 2FA codes expire after a configured timeout (typically a few minutes). Users must complete 2FA within this window.

5. **Token Renewal**: Use the renewal token to refresh the authentication token before it expires, avoiding unnecessary re-authentication.

