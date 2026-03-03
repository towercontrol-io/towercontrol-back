## Implementation example for the public FAQ page

## Nuxt.js 4
A full implementation for the public FAQ has been made for Nuxt.js 4. The project is accessible on GitHub at the following URL: [IoTowerFront](https://github.com/disk91/IoTowerFront)
With the following direct links:
- [Public FAQ page](https://github.com/disk91/IoTowerFront/blob/main/app/pages/front/public/faq.vue)
- [FAQ API data structures](https://github.com/disk91/IoTowerFront/blob/main/app/types/tickets.ts)
- [FAQ API integration](https://github.com/disk91/IoTowerFront/blob/main/app/plugins/api.backend.tickets.ts)

## Step by step communication with backend

### Step 1: Retrieve public FAQ entries
The front-end makes a `GET` request to the backend API endpoint `/tickets/1.0/public/faq` with optional query parameters 
for pagination. This endpoint does not require authentication and can be accessed by anonymous users.

#### Query parameters
The following query parameters can be used to control pagination:
- `page`: Page number starting at 0 (default: 0)
- `size`: Number of entries per page (default: 10)

Example: `/tickets/1.0/public/faq?page=0&size=10`

#### Data Structure for FAQ response
The API endpoint returns an array of FAQ entries, each containing the following information:

```typescript
export interface PrivTicketFaqResponseItf {
    /** Unique FAQ identifier */
    id: number;

    /** FAQ title/topic (Markdown format) */
    topic: string;

    /** FAQ content/answer (Markdown format) */
    content: string;

    /** Total number of FAQ entries available (for pagination) */
    totalFaq: number;
}
```

#### Way to retrieve FAQ entries from backend
Here is an example of implementation in TypeScript for Nuxt.js 4, but the code can be adapted to other frameworks.

```typescript
const ticketsModulePublicFaqGet: string = '/tickets/1.0/public/faq';

/**
 * Get public FAQ entries (no authentication required)
 */
ticketsModulePublicFaqGet: async (page: number = 0, size: number = 10): Promise<{ success?: PrivTicketFaqResponseItf[]; error?: ActionResult | { message: string } }> => {
    try {
        const response = await apiCallwithTimeout<PrivTicketFaqResponseItf[]>(
            'GET',
            `${ticketsModulePublicFaqGet}?page=${page}&size=${size}`,
            null,
            true
        );
        return { success: response }
    } catch (error : any) {
        return { error };
    }
}
```

#### Response from the API
The response from the API is an array of FAQ entries. The response code is `200` when the request is successful.

When the response code is `400` or `50X`, the FAQ retrieval failed and a structure ActionResult is returned, with 
the error message contained in field `message` and corresponding code in field `status_code`.

In case of error, the error should be displayed to the user. The `message` field is an i18n key that can be used to 
display a translated message to the user, the list of i18n keys is available in file [i18n asset file](i18n.md) of this skill.

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
   * Example: "tickets-invalid-pagination-params"
   */
  message: string;
};
```

### Step 2: Display FAQ entries
Once the FAQ entries are retrieved from the backend, they should be displayed to the user. Each entry contains:
- `id`: The unique identifier of the FAQ entry
- `topic`: The question or topic (Markdown format)
- `content`: The detailed answer (Markdown format)
- `totalFaq`: The total number of FAQ entries available (used for pagination)

The fields `id` and `totalFaq` should not be displayed to the user, they are only used for internal management and pagination.

The front-end should:
1. Parse and render the Markdown content in both `topic` and `content` fields
2. Display the entries in a user-friendly format
3. Implement pagination controls using the `totalFaq` field to calculate the total number of pages
4. Allow users to navigate between pages

### Step 3: Handle pagination
To implement pagination:
1. Calculate the total number of pages: `totalPages = Math.ceil(totalFaq / size)` where `size` is the number of entries per page
2. Display pagination controls (previous, next, page numbers)
3. When the user clicks on a page, make a new request with the updated `page` parameter
4. Update the display with the new FAQ entries

### Step 4: Handle errors
If an error occurs during the FAQ retrieval:
1. Display an error message to the user
2. Use the i18n key from the error response to display a translated message
3. Provide a way for the user to retry the operation (e.g., a reload button)

## Implementation Parameters
You may ask the developer for the following parameters to generate the code:
- The number of FAQ entries to display per page (page size)
- If you should display the `topic` and the `content` or if your display a list of `topic` and display the 
content when one of the topic is clicked by the user (accordion style).

## Example implementation structure

### Component structure
A typical implementation might have:
- A main FAQ page component that handles the API calls and state management
- A FAQ entry component that displays individual FAQ entries with Markdown rendering
- A pagination component for navigation between pages
- Error handling and loading states

### State management
The component should manage:
- Current page number
- Page size
- FAQ entries array
- Total FAQ count
- Loading state
- Error state

### Example flow
```
1. Component mounts
2. Set loading state to true
3. Call API with page=0 and size=10
4. On success:
   - Store FAQ entries
   - Store total FAQ count
   - Calculate total pages
   - Set loading state to false
5. On error:
   - Display error message
   - Set loading state to false
6. User clicks pagination:
   - Update page number
   - Go back to step 2
```

