## Implementation example for the private ticket management page

## Nuxt.js 4
A full implementation for the private ticket management page has been made for Nuxt.js 4. The project is accessible
on GitHub at the following URL: [towercontrol-front](https://github.com/towercontrol-io/towercontrol-front)

With the following direct links:
- [Tickets page (entry point)](https://github.com/towercontrol-io/towercontrol-front/blob/main/app/pages/front/private/tickets.vue)
- [Ticket list component](https://github.com/towercontrol-io/towercontrol-front/blob/main/app/components/tickets/TicketList.vue)
  - Used with a `tickets` prop containing the array of `PrivTicketAbstractResponseItf`.
  - Emits a `select` event with the selected ticket ID when the user clicks a row.
- [Ticket content & reply component](https://github.com/towercontrol-io/towercontrol-front/blob/main/app/components/tickets/TicketContent.vue)
  - Used with a `ticketId` prop set from the selected ticket ID.
  - Used with `isAdmin` prop set to `false` and `authKey` prop left empty (not needed for authenticated users).
- [API data structures](https://github.com/towercontrol-io/towercontrol-front/blob/main/app/types/tickets.ts)
- [API integration plugin](https://github.com/towercontrol-io/towercontrol-front/blob/main/app/plugins/api.backend.tickets.ts)
  - `ticketsModulePrivateList` : retrieve the list of user tickets
  - `ticketsModulePrivateOneTicket` : retrieve the full content of one ticket
  - `ticketsModulePrivateUpdate` : add a reply to a ticket

---

## Step by step communication with backend

### Step 1: Retrieve the ticket list

The front-end makes a `GET` request to the backend API endpoint `/tickets/1.0/ticket`. To also include closed tickets,
add the query parameter `?closed=true`.

#### Way to retrieve the ticket list from backend

```typescript
    const ticketsModuleListGet: string = '/tickets/1.0/ticket';

    /**
     * Get the list of the authenticated user's own tickets
     */
    ticketsModulePrivateList: async (includeClosed: boolean = false): Promise<{ success?: PrivTicketAbstractResponseItf[]; error?: ActionResult | { message: string } }> => {
        try {
            const url = includeClosed
                ? `${ticketsModuleListGet}?closed=true`
                : ticketsModuleListGet;
            const response = await apiCallwithTimeout<PrivTicketAbstractResponseItf[]>(
                'GET',
                url,
                undefined,
                false   // authentication required
            );
            return { success: response };
        } catch (error: any) {
            return { error };
        }
    },
```

#### Response from the API

The response is a JSON array. Each element has the following structure:

```typescript
export interface PrivTicketAbstractResponseItf {
    /** Ticket unique identifier */
    id: number;

    /** Ticket owner login or email — only set for admin calls, empty for regular users */
    owner?: string;

    /** Short ticket title (Markdown allowed) */
    topic: string;

    /** Ticket creation date in milliseconds since epoch */
    creationMs: number;

    /** Ticket status: "OPEN" or "CLOSED" */
    status: string;

    /**
     * true when the support team has replied and the user is expected to respond.
     * Used to display a visual indicator ("waiting for your answer").
     */
    userPending: boolean;

    /**
     * true when the user has replied and the support team is expected to respond.
     * Used to display a visual indicator ("waiting for support answer").
     */
    adminPending: boolean;

    /** Total number of tickets — used for pagination metadata */
    countItems: number;
}
```

When the response code is `400`, the request parameters are invalid. The response is an `ActionResult` structure.
When the response code is `403`, the user is not authenticated or lacks rights. The response is an `ActionResult` structure.

An empty array `[]` is a valid response meaning the user has no tickets yet. Display an empty-state message in this case.

---

### Step 2: Display the ticket list

When the list is successfully retrieved:
1. Render each entry as a row or card showing: topic, status badge, creation date (from `creationMs`).
2. Highlight rows where `userPending = true` (support has answered, user must reply).
3. Highlight rows where `adminPending = true` (user has answered, support must reply).
4. When the user clicks a row, navigate to or reveal the ticket detail (Step 3) using the `id` field.
5. If the array is empty, display an empty state message (use i18n key `tickets-list-empty`).

---

### Step 3: Retrieve the ticket detail

The front-end makes a `GET` request to `/tickets/1.0/ticket/{ticketId}/` where `{ticketId}` is the numeric ticket ID.

#### Way to retrieve one ticket from backend

```typescript
    const ticketsModuleOneTicketGet: string = '/tickets/1.0/ticket';

    /**
     * Get the full content of one ticket, including all replies
     */
    ticketsModulePrivateOneTicket: async (ticketId: number): Promise<{ success?: PrivTicketUserDetailResponseItf; error?: ActionResult | { message: string } }> => {
        try {
            const response = await apiCallwithTimeout<PrivTicketUserDetailResponseItf>(
                'GET',
                `${ticketsModuleOneTicketGet}/${ticketId}/`,
                undefined,
                false   // authentication required
            );
            return { success: response };
        } catch (error: any) {
            return { error };
        }
    },
```

#### Response from the API

```typescript
export interface PrivTicketUserDetailResponseItf {
    /** Ticket unique identifier */
    id: number;

    /** Original ticket content in Markdown */
    content: string;

    /** Moment of creation in milliseconds since epoch */
    creationMs: number;

    /** List of replies in chronological order */
    responses: PrivMessageContent[];
}

export interface PrivMessageContent {
    /** Reply unique identifier */
    id: string;

    /** Reply date in milliseconds since epoch */
    creationMs: number;

    /** Reply content in Markdown */
    content: string;

    /**
     * true  → this message was written by the ticket owner (the user)
     * false → this message was written by the support team
     */
    fromUser: boolean;
}
```

When the response code is `400` or `404`, the ticket was not found or parameters are invalid.
When the response code is `403`, the user is not authenticated or does not own this ticket.

---

### Step 4: Display the ticket detail

When the ticket details are successfully retrieved:
1. Display the ticket ID as a reference header (e.g., `Ticket #91`).
2. Render the original ticket `content` in **Markdown**.
3. Display all replies in chronological order (sorted by `creationMs` ascending):
   - Render each reply `content` in **Markdown**.
   - Show the date/time using `creationMs` (convert to a human-readable locale-aware format).
   - Apply distinct visual styling:
     - `fromUser = true` → user message: align to the right, use the user's accent color.
     - `fromUser = false` → support message: align to the left, use the support accent color.
4. Display a reply form at the bottom (see Step 5).

---

### Step 5: Add a reply to the ticket

The user fills in the reply text area with **Markdown** content. He can optionally choose to close the ticket.

#### Validation before submission
- Content must not be empty.
- Content must have a minimum meaningful length (recommended minimum: 10 characters).
- The front-end should reject content containing raw `<script>` or HTML tags.

#### Data structure for adding a reply

```typescript
export interface PrivTicketUserMessageBody {
    /** Ticket id */
    id: number;

    /** Reply content in Markdown (empty string only when just closing the ticket) */
    content: string;

    /** Admin-only internal note — must be empty or absent for regular users */
    adminContent?: string;

    /** Set to true to close the ticket after this reply */
    closeTicket: boolean;

    /** Keep for Knowledge Base once closed — always false for regular users */
    closeKb: boolean;

    /** Authorization key for public anonymous response — must be empty or absent for authenticated users */
    authKey?: string;
}
```

For an authenticated user adding a reply, the fields should be:
- `id`: The ticket ID.
- `content`: The reply content in Markdown (from the text area).
- `adminContent`: Empty string or not set.
- `closeTicket`: `true` if the user wants to close the ticket, `false` otherwise.
- `closeKb`: Always `false` for regular users.
- `authKey`: Empty string or not set (not needed for authenticated users).

#### Way to submit a reply to the backend

```typescript
    const ticketsModuleUpdatePut: string = '/tickets/1.0/ticket';

    /**
     * Add a reply to an existing ticket or close it
     */
    ticketsModulePrivateUpdate: async (body: PrivTicketUserMessageBody): Promise<{ success?: ActionResult; error?: ActionResult | { message: string } }> => {
        try {
            const response = await apiCallwithTimeout<ActionResult>(
                'PUT',
                ticketsModuleUpdatePut,
                body,
                false   // authentication required
            );
            return { success: response };
        } catch (error: any) {
            return { error };
        }
    },
```

#### Response from the API

The API returns `200` (or `201`) with an `ActionResult` on success:

```typescript
export interface ActionResult {
    /** Result status: "OK", "ERROR", etc. */
    status: string;

    /** HTTP status code */
    status_code: number;

    /** i18n-ready message key */
    message: string;
}
```

When the response code is `400`, the request format is invalid or the content does not pass validation.
When the response code is `403`, the user is not authenticated, does not own the ticket, or lacks the `ROLE_SUPPORT_USER` role.

---

### Step 6: Refresh after adding a reply

After a successful reply (`200` or `201`):
1. Display a success message using the i18n key from the response `message` field (e.g., `tickets-new-response-added`).
2. Clear the reply text area.
3. Reload the ticket detail (Step 3) to display the newly added message.
4. Scroll to the bottom of the replies list to reveal the new message.

If the ticket was closed (`closeTicket = true`):
1. Display a confirmation that the ticket is now closed.
2. Navigate back to the ticket list (Step 1) and refresh it.

---

### Step 7: Handle errors

Throughout the process, handle various error scenarios:

**403 Forbidden** — user not authenticated or session expired:
- Display an error message using i18n key `tickets-ticket-access-denied`.
- Redirect the user to the login page.

**400 Bad Request** — validation errors:
- Display the specific i18n key returned in the `message` field of `ActionResult`.
- Common keys: `tickets-creation-content-too-short`, `tickets-content-invalid-markdown`, `tickets-ticket-not-found`.

**404 Not Found** — ticket does not exist:
- Display `tickets-ticket-not-found` message.
- Navigate back to the ticket list.

**50x Server Error** — backend server error:
- Display a generic error message.

For all errors, use the i18n keys from the [i18n asset file](i18n.md) to display translated messages to the user.

---

## Page layout recommendation

The recommended layout for the ticket management page is a **two-panel or tabbed layout**:

```
+------------------------------------------------------+
| My Support Tickets                [+ New Ticket]     |
+---------------------------+  +-----------------------+
| OPEN   CLOSED             |  | # Ticket #91          |
|---------------------------|  | [topic]               |
| [!] Ticket #91  OPEN      |  |-----------------------|
|     Cannot connect ...    |  | [original content MD] |
|     Created 2026-01-15    |  |-----------------------|
|---------------------------|  | [Support] 2026-01-16  |
| Ticket #87    OPEN        |  | Please try ...        |
|     Sensor offline ...    |  |-----------------------|
|     Created 2026-01-10    |  | [You] 2026-01-17      |
|---------------------------|  | I tried but ...       |
| Ticket #42    CLOSED      |  |-----------------------|
|     ...                   |  | [ Reply text area   ] |
|                           |  | [☐ Close ticket]      |
|                           |  | [        Send       ] |
+---------------------------+  +-----------------------+
```

- `[!]` indicator means `userPending = true` → the user has a reply waiting.
- The left panel is the `TicketList` component.
- The right panel is the `TicketContent` component.

---

## Security considerations

- Always include the Bearer token in the `Authorization` header.
- Never display raw error stack traces to the user.
- Validate reply content length and format on the front-end before submission to avoid unnecessary API calls.
- The `adminContent` and `closeKb` fields are reserved for support managers; do not expose them in the regular user interface.

