## Implementation example for the user sign out page

## Nuxt.js 4
A full implementation for user sign out has been made for Nuxt.js 4. The project is accessible on GitHub at the following URL: [towercontrol-front](https://github.com/towercontrol-io/towercontrol-front)
With the following direct links:
- [Sign Out Implementation](https://github.com/towercontrol-io/towercontrol-front/blob/main/app/pages/front/private/signout.vue)
- [User Session API data structures](https://github.com/towercontrol-io/towercontrol-front/blob/main/app/types/users.ts)
- [User Session API integration](https://github.com/towercontrol-io/towercontrol-front/blob/main/app/plugins/api.backend.users.ts)

## Step by step communication with backend

### Step 1: Call Sign Out Endpoint
When the user decides to sign out, call the sign out endpoint with the current JWT token.

**Endpoint**: `GET /users/1.0/session/signout`

**Requirements**:
- User must have a valid JWT token
- JWT token must include at least `ROLE_REGISTERED_USER` and `ROLE_LOGIN_1FA` roles
- API accounts (with `ROLE_LOGIN_API`) cannot use this endpoint

#### Way to call the sign out endpoint
Here is an example of implementation in TypeScript for Nuxt.js 4, but the code can be adapted to other frameworks.

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

**How to include JWT token in the request**:
The JWT token should be included in the Authorization header with the Bearer prefix:

```typescript
headers: {
    'Authorization': `Bearer ${jwtToken}`
}
```

This is typically handled automatically by your API client if the token is stored in the application state.

#### Response from the API
**Success Response**: 
- `200` with ActionResult message `user-signed-out`
- All user sessions have been invalidated

**Error Response**: 
- `400` with ActionResult message `user-logout-refused`
- This can happen if the user is not found or if there's an internal error

```typescript
export interface ActionResult {
  /**
   * Result of a given action
   */
  status: ACTION_RESULT;

  /**
   * Associated custom code, http code
   */
  status_code: number;

  /**
   * Associated custom message ready for i18n
   */
  message: string;
}
```

### Step 2: Clean Up Front-end State
After calling the sign out endpoint (whether it succeeds or fails), immediately clean up the front-end state:

#### Clear stored tokens
```typescript
// Clear JWT tokens from storage
localStorage.removeItem('jwtToken');
localStorage.removeItem('jwtRenewalToken');
sessionStorage.removeItem('jwtToken');
sessionStorage.removeItem('jwtRenewalToken');

// Or if using a state management system
authStore.clearTokens();
```

#### Clear user data
```typescript
// Clear any stored user data
localStorage.removeItem('userData');
localStorage.removeItem('userProfile');
sessionStorage.clear();

// Reset application state
authStore.logout();
userStore.reset();
```

#### Clear cookies (if used)
```typescript
// Clear authentication cookies
document.cookie = 'jwtToken=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;';
document.cookie = 'jwtRenewalToken=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;';
```

### Step 3: Redirect User
After cleaning up, redirect the user to an appropriate page:

```typescript
// Redirect to login page
router.push({
    path: '/front/public/signin',
    query: { message: 'logged-out-successfully' }
});

// Or redirect to home page
router.push('/');
```

## Complete Sign Out Implementation Example

Here's a complete example of a sign out function in TypeScript for Nuxt.js 4:

```typescript
/**
 * Sign out the current user
 */
async function signOut() {
    try {
        // Call the backend sign out endpoint
        const result = await usersModuleSessionSignout();
        
        if (result.success) {
            console.log('Sign out successful:', result.success.message);
        } else {
            console.warn('Sign out failed:', result.error);
            // Continue with local cleanup even if API call fails
        }
    } catch (error) {
        console.error('Error during sign out:', error);
        // Continue with local cleanup even if API call fails
    } finally {
        // Always clean up local state, regardless of API call result
        await cleanUpLocalState();
        
        // Redirect to login page
        await router.push({
            path: '/front/public/signin',
            query: { message: 'logged-out' }
        });
    }
}

/**
 * Clean up all local state and storage
 */
function cleanUpLocalState() {
    // Clear tokens from local storage
    localStorage.removeItem('jwtToken');
    localStorage.removeItem('jwtRenewalToken');
    
    // Clear user data
    localStorage.removeItem('userData');
    localStorage.removeItem('userProfile');
    
    // Clear session storage
    sessionStorage.clear();
    
    // Reset application state
    authStore.clearTokens();
    authStore.clearUser();
    userStore.reset();
    
    // Clear any cookies
    document.cookie = 'jwtToken=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;';
}
```

## UI Implementation Examples

### Simple Logout Button
```vue
<template>
  <button @click="handleSignOut" class="btn-logout">
    Sign Out
  </button>
</template>

<script setup lang="ts">
const { signOut } = useAuth();

const handleSignOut = async () => {
  await signOut();
};
</script>
```

### Logout with Confirmation Dialog
```vue
<template>
  <div>
    <button @click="showConfirmDialog = true" class="btn-logout">
      Sign Out
    </button>
    
    <ConfirmDialog
      v-if="showConfirmDialog"
      title="Confirm Sign Out"
      message="Are you sure you want to sign out? You will be logged out from all devices."
      @confirm="handleSignOut"
      @cancel="showConfirmDialog = false"
    />
  </div>
</template>

<script setup lang="ts">
const showConfirmDialog = ref(false);
const { signOut } = useAuth();

const handleSignOut = async () => {
  showConfirmDialog.value = false;
  await signOut();
};
</script>
```

## Sign Out Flow Diagram

```
┌─────────────────┐
│ User Clicks     │
│ Logout Button   │
└────────┬────────┘
         │
         v
┌─────────────────┐
│ Call Backend    │
│ GET /signout    │
│ with JWT token  │
└────────┬────────┘
         │
         v
┌─────────────────┐
│ Clear Local     │
│ Storage         │
│ - JWT tokens    │
│ - User data     │
│ - Session data  │
└────────┬────────┘
         │
         v
┌─────────────────┐
│ Reset App State │
│ - Auth store    │
│ - User store    │
└────────┬────────┘
         │
         v
┌─────────────────┐
│ Redirect to     │
│ Login Page      │
└─────────────────┘
```

## Error Handling

All error responses follow the ActionResult structure with i18n keys.

### Sign Out Errors (Returns 400):
- `user-logout-refused`: Generic logout failure (user not found, internal error)
- `user-profile-no-access`: User doesn't have permission (unlikely for self sign out)
- `user-profile-user-not-found`: User account not found

**Important**: Even if the API call fails, always clean up local storage and redirect to login page. This ensures the user is logged out on the front-end side, improving security.

For a complete list of error messages, refer to the [i18n asset file](i18n.md).

## Security Notes

1. **Complete Session Invalidation**: The sign out endpoint invalidates ALL active sessions for the user, across all devices.

2. **Session Secret Renewal**: The backend generates a new session secret, which is part of the JWT signature. This makes all existing tokens invalid immediately.

3. **Multiple Device Logout**: When a user signs out from one device, they are automatically signed out from all other devices.

4. **Always Clean Up Locally**: Even if the API call fails, always clear local storage and redirect to login. This prevents a false sense of security.

5. **No Rollback**: Once the sign out endpoint is called, the session invalidation cannot be undone. The user must log in again.

6. **API Accounts**: Long-lived API tokens (created with `ROLE_LOGIN_API`) are not affected by user sign out. They must be revoked separately through the API key management endpoints.

7. **Audit Trail**: All sign out actions are logged in the audit system with user login, timestamp, and IP address.

## Best Practices

1. **Confirm Before Sign Out**: For better UX, consider showing a confirmation dialog, especially on mobile where logout buttons might be accidentally tapped.

2. **Show Feedback**: Display a brief message after sign out to confirm the action.

3. **Handle Network Errors**: Always clean up local state even if the API call fails due to network issues.

4. **Redirect Strategy**: 
   - For web apps: Redirect to login page with optional message parameter
   - For mobile apps: Navigate to login screen and clear navigation stack

5. **State Management**: Use a centralized auth store/composable to manage sign out logic consistently across your application.

6. **Token Cleanup**: Ensure all storage mechanisms are cleared (localStorage, sessionStorage, cookies, memory).

7. **Prevent Re-authentication Loops**: After sign out, ensure the app doesn't automatically try to refresh tokens or re-authenticate.

8. **Loading State**: Show a loading indicator during sign out to prevent multiple clicks.

