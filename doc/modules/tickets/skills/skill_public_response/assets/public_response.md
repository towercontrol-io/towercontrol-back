## Implementation example for the public ticket response page

## Nuxt.js 4
A full implementation for the public ticket response can be based on the contact form implementation for Nuxt.js 4. 
The project is accessible on GitHub at the following URL: [towercontrol-front](https://github.com/towercontrol-io/towercontrol-front)
With the following relevant links:
- [Public Ticket View page](https://github.com/towercontrol-io/towercontrol-front/blob/main/app/pages/front/public/contact.vue)
- [Public Ticket View component](https://github.com/towercontrol-io/towercontrol-front/blob/main/app/components/tickets/ContactForm.vue)
- [Public Ticket View API data structures](https://github.com/towercontrol-io/towercontrol-front/blob/main/app/types/tickets.ts)
- [Public Ticket View API integration](https://github.com/towercontrol-io/towercontrol-front/blob/main/app/plugins/api.backend.tickets.ts)
  - `ticketsModulePublicOneTicket` : read the ticket & messages
  - `ticketsModulePublicUpdate` : add a response to the ticket

## Step by step communication with backend

### Step 1: Retrieve ticket details
The front-end retrieves the `ticketId` and `accessKey` from the URL query parameters (e.g., from `/front/public/contact-view?ticketId=6&accessKey=vwdf1773dsfhjskhsd`).

Then, it makes a `GET` request to backend API endpoint `/tickets/1.0/public/ticket` with these parameters.

#### Way to retrieve ticket details from backend
Here is an example of implementation in TypeScript for Nuxt.js 4, but the code can be adapted to other frameworks.

```typescript
    const ticketsModulePublicTicketGet: string = '/tickets/1.0/public/ticket';
    
    /**
     * Get a public ticket details
     */
    ticketsModulePublicTicketGet: async (ticketId: number, accessKey: string): Promise<{ success?: PrivTicketUserDetailResponseItf; error?: ActionResult | { message: string } }> => {
        try {
            const response = await apiCallwithTimeout<PrivTicketUserDetailResponseItf>(
                'GET',
                `${ticketsModulePublicTicketGet}?ticketId=${ticketId}&accessKey=${encodeURIComponent(accessKey)}`,
                undefined,
                true
            );
            return { success: response }
        } catch (error : any) {
            return { error };
        }
    }
```

#### Response from the API
The response from the API is the following structure when successful (response code `200`):

```typescript
export interface PrivTicketUserDetailResponseItf {
    /** Ticket ID */
    id: number;

    /** Ticket content in Markdown */
    content: string;

    /** List of responses & replies to the ticket */
    responses: PrivMessageContent[];
}

export interface PrivMessageContent {
    /** Reply id */
    id: string;

    /** Response date in milliseconds since epoch */
    creationMs: number;

    /** Ticket message content (Markdown allowed) */
    content: string;

    /** Message redacted by the ticket owner (true) or support staff (false) */
    fromUser: boolean;
}
```

When the response code is `400`, the ticket was not found or parameters are invalid. The response is an ActionResult structure.
When the response code is `403`, the access key is invalid or access is denied. The response is an ActionResult structure.
When the response code is `410` (Gone), the ticket has been deleted due to over quota. The response is an ActionResult structure.

In case of error, display the error message to the user. The `message` field is an i18n key that can be used to display 
a translated message to the user, the list of i18n keys is available in file [i18n asset file](i18n.md) of this skill.

The format of `ActionResult` is the following is accessible in the codebase : [ActionResult](https://github.com/towercontrol-io/towercontrol-front/blob/ee7d646d6bc9936ef76cfb4756e982e3a01d545b/app/types/common.ts#L30)

### Step 2: Display ticket content and responses
When the ticket details are successfully retrieved:
1. Display the ticket ID at the top of the page
2. Display the original ticket content in a card or section, rendering the Markdown content
3. Display all responses in chronological order (sorted by `creationMs` ascending)
4. For each response:
   - Render the Markdown content
   - Display the date/time using `creationMs` (convert to human-readable format)
   - Use different visual styling based on `fromUser`:
     - `fromUser = true`: Style as user message (e.g., align right, different color)
     - `fromUser = false`: Style as support message (e.g., align left, different color)

### Step 3: Add a new response
The user can add a new response to the ticket by filling a text area with Markdown content and submitting it. He can 
select to send a response or to close the ticket.

#### Data Structure for adding a response
```typescript
export interface PrivTicketUserMessageBody {
    /** Ticket id */
    id: number;

    /** Response content (Markdown allowed), empty when just closing the ticket */
    content: string;

    /** Close the ticket */
    closeTicket: boolean;

    /** Keep for KB (admin only) once closed */
    closeKb: boolean;

    /** Authorization Key for public anonymous response */
    AuthKey: string;
}
```

For a public user adding a response, the fields should be:
- `id`: The ticket ID (from URL parameter)
- `content`: The response content in Markdown (from text area)
- `adminContent`: Empty string or not set
- `closeTicket`: `false` (unless user wants to close the ticket, which could be an optional feature)
- `closeKb`: always `false` or not set
- `AuthKey`: The access key (from URL parameter)

#### Way to submit a response to the backend
Here is an example of implementation in TypeScript for Nuxt.js 4, but the code can be adapted to other frameworks.

```typescript
    const ticketsModulePublicTicketPut: string = '/tickets/1.0/public/ticket';
    
    /**
     * Add a response to a public ticket
     */
    ticketsModulePublicTicketPut: async (body: PrivTicketUserMessageBody): Promise<{ success?: ActionResult; error?: ActionResult | { message: string } }> => {
        try {
            const response = await apiCallwithTimeout<ActionResult>(
                'PUT',
                ticketsModulePublicTicketPut,
                body,
                true
            );
            return { success: response }
        } catch (error : any) {
            return { error };
        }
    }
```

#### Response from the API
The response from the API when successful (response code `200`) is an ActionResult structure.

When the response code is `400`, the request format is invalid or the content doesn't pass validation. The response is an ActionResult structure.
When the response code is `403`, the access key is invalid or access is denied. The response is an ActionResult structure.

In case of error, display the error message to the user. The `message` field is an i18n key that can be used to display 
a translated message to the user, the list of i18n keys is available in file [i18n asset file](i18n.md) of this skill.

### Step 4: Refresh after adding a response
After successfully adding a response (response code `200`):
1. Display a success message to the user using the i18n key from the response
2. Clear the text area
3. Refresh the ticket details by making another `GET` request (Step 1) to display the newly added response
4. Scroll to the bottom of the responses list to show the new response

### Step 5: Handle errors
Throughout the process, handle various error scenarios:

**403 Forbidden**: The access key is invalid or expired
- Display an error message: "You don't have access to this ticket" (use i18n key)
- Optionally hide the ticket content and only show the error

**400 Bad Request**: Invalid parameters or validation errors
- Display the specific error message from the API response
- Common errors include:
  - Invalid ticket ID
  - Content is empty or too short
  - Invalid Markdown syntax
  - Ticket not found

**410 Gone**: The ticket has been expired
- Display a message: "This ticket is no longer available" (use i18n key)

**50x Server Error**: Backend server error
- Display a generic error message: "An error occurred, please try again later" (use i18n key)

For all errors, use the i18n keys from the [i18n asset file](i18n.md) to display translated messages to the user.

## Security considerations
- Never expose the access key in logs or error messages
- Validate that the access key is present before making API calls
- The access key is sensitive and should be treated like a password

