---
name: skill_private_ticket_management
description: A skill to list user tickets, view ticket content and add responses, as an identified user. Based on IoT Tower Control backend.
license: GPL-3.0
metadata:
    author: "disk91"
    version: "1.0.0"
---

# Manage support tickets as an identified user — List, View and Reply

## Overview
This skill allows you to build a ticket management page for identified (authenticated) users. It is based on the
IoT Tower Control backend, which provides a robust and scalable platform for building IoT platforms. This skill covers
three closely related interactions:

1. **List** the user's own tickets (open and optionally closed).
2. **View** the full content of a single ticket with its exchange history.
3. **Reply** to an open ticket or close it.

## When to use this skill
When you've been asked to integrate a support ticket management view in the application so that authenticated users can
consult their own tickets and interact with the support team.

## How to use this skill
This skill helps to generate working code for the ticket management page. It contains examples in TypeScript, made for
Nuxt.js 4, but the code can be adapted to other frameworks.

## Principles of the ticket management page

### Ticket list
The list view shows all tickets owned by the authenticated user:
- Each ticket entry displays: ticket ID, topic (title), creation date and current status (`OPEN` or `CLOSED`).
- Two visual indicators are available:
  - `userPending = true`: the support team has replied and the user is expected to respond.
  - `adminPending = true`: the user has replied and the support team is expected to respond.
- By default, only open tickets are listed. An option (toggle or query param) can display closed tickets as well.
- When no ticket is found, an empty state message is displayed.

### Ticket detail
The detail view shows the full content of one ticket:
- The original ticket content, rendered in **Markdown**.
- All replies in chronological order, each rendered in **Markdown**.
- Visual distinction between messages from the user (`fromUser = true`) and from the support team (`fromUser = false`).
- The date/time of each message, computed from the `creationMs` field (milliseconds since epoch).

### Reply to a ticket
From the detail view, the user can:
- Add a new reply in **Markdown** format.
  - The reply must not be empty and must have a minimum length.
- Optionally close the ticket (toggle/checkbox `closeTicket`).
- After a successful reply, the ticket detail is refreshed to show the new message.

### File attachments

File attachments are a first-class feature of tickets:

#### Viewing attached files
When displaying a ticket's detail, the `context` array may contain `CustomField` entries where the `value`
starts with `"file_"`. Each such entry represents an attached file:
- `key`: the file type label chosen by the uploader (e.g., `screenshot`, `log`, `config`).
- `value`: `"file_"` followed by the file's `uniqueName` (e.g., `"file_550e8400-...-1712345678901.jpg"`).

To display these attachments the front-end must call `GET /files/1.0/{uniqueName}/info` for each entry to
retrieve the file metadata (including `accessKey`, `mimeCategory` and `thumbnailUniqueName`). Since the user
is the file owner, the response will include the `accessKey`.

For image files (`mimeCategory === "IMAGE"`):
- Render a thumbnail: `GET /files/1.0/{uniqueName}/thumbnail?key={accessKey}`
- Provide a **"Copy Markdown link"** button that copies the following string to the clipboard:
  ```
  ![originalName](BACKEND_API_BASE/files/1.0/{uniqueName}/full?key={accessKey})
  ```
  This Markdown syntax can be pasted into a reply to embed the image inline.
- Provide a direct download link: `GET /files/1.0/{uniqueName}/full?key={accessKey}`

For non-image files, provide only the download link.

#### Attaching files to a reply
When adding a reply, the user can attach one or more new files using the same mechanism as ticket creation:
1. Upload each file via `POST /files/1.0/upload` with `accessType=PRIVATE` and `withAccessKey=true`.
2. Add the resulting `CustomField` entries to the `context` field of `PrivTicketUserMessageBody`.
3. For image uploads, a thumbnail preview and a **"Copy Markdown link"** button are provided.

Full TypeScript structures and API call examples are documented in [ticket_management.md](assets/ticket_management.md).

## Workflow

The file [ticket_management.md](assets/ticket_management.md) contains the complete step-by-step workflow with all data
structures and API call examples. The list of [i18n keys](assets/i18n.md) is also provided.

### Step 1 — Load the ticket list
- The front-end makes a `GET` request to `/tickets/1.0/ticket`.
- The optional query parameter `closed=true` can be added to include closed tickets.
- The expected response is `200` with a JSON array of ticket abstracts.
- The array may be empty (no tickets yet): display an empty-state message.

### Step 2 — Select a ticket
- The user clicks on a ticket in the list.
- The ticket ID is used to navigate to (or reveal) the detail view.

### Step 3 — Load the ticket detail
- The front-end makes a `GET` request to `/tickets/1.0/ticket/{ticketId}/`.
- The expected response is `200` with the full ticket content and its reply history.

### Step 4 — Reply (optional)
- The user fills in the reply text area.
- The front-end makes a `PUT` request to `/tickets/1.0/ticket` with the reply body.
- The expected response is `200` (updated).
- After success, the ticket detail is reloaded (Step 3) and the text area is cleared.

### Step 5 — Handle errors
- `403`: The user is not authenticated or lacks sufficient rights → display an appropriate message using i18n keys.
- `400`: Validation error → display the error message using i18n keys.
- `404`: Ticket not found → display an appropriate message.
- `50x`: Server error → display a generic error message.

## API endpoints used

The API endpoints are relative to an API base URL provided via an environment variable. In a Nuxt.js implementation,
this variable is defined in the `nuxt.config.ts` file as follows:

```typescript
export default defineNuxtConfig({
  runtimeConfig: {
    public: {
      BACKEND_API_BASE: 'http://localhost:8091',  // Backend API base URL
    }
  },
})
```

The best way to implement the interface with the backend is to use the libraries available here (in TypeScript for Nuxt.js):
- [Ticket API data structures](https://github.com/towercontrol-io/towercontrol-front/blob/main/app/types/tickets.ts)
- [Ticket API integration](https://github.com/towercontrol-io/towercontrol-front/blob/main/app/plugins/api.backend.tickets.ts)

### `GET /tickets/1.0/ticket`
List the authenticated user's own tickets.
- Requires authentication (Bearer token).
- Optional query parameter: `closed=true` to include closed tickets.
- Returns an array of `PrivTicketAbstractResponseItf`.
- Full request/response structures are detailed in [ticket_management.md](assets/ticket_management.md).

### `GET /tickets/1.0/ticket/{ticketId}/`
Get the full content of one ticket, including all replies.
- Requires authentication (Bearer token).
- Path parameter: `ticketId` (Number) — the unique ticket ID.
- Returns a `PrivTicketUserDetailResponseItf`.
- Full request/response structures are detailed in [ticket_management.md](assets/ticket_management.md).

### `PUT /tickets/1.0/ticket`
Add a reply to an existing ticket or close it.
- Requires authentication (Bearer token) **and** the role `ROLE_SUPPORT_USER`.
- Body structure: `PrivTicketUserMessageBody` (includes an optional `context` field for new file attachments).
- Returns an `ActionResult` on success (`200`).
- Full request/response structures are detailed in [ticket_management.md](assets/ticket_management.md).

### `GET /files/1.0/{uniqueName}/info`
Retrieve file metadata for a ticket attachment.
- Requires authentication (Bearer token).
- Path variable: `uniqueName` — extracted from the `context` field value (strip the `"file_"` prefix).
- Returns `FileUploadResponseItf` including `accessKey`, `mimeCategory`, `thumbnailUniqueName`.
- Full structures are detailed in [ticket_management.md](assets/ticket_management.md).

### `POST /files/1.0/upload`
Upload a new file attachment when adding a reply.
- Requires authentication (Bearer token).
- Must be called with `accessType=PRIVATE` and `withAccessKey=true`.
- Full structures and TypeScript examples are in [ticket_management.md](assets/ticket_management.md).

## Authentication requirements
All three private ticket endpoints require:
- A valid authenticated session/token.
- The Bearer token must be included in every request header.
- The `PUT` endpoint additionally requires the role `ROLE_SUPPORT_USER`.

If authentication fails:
- HTTP `403` (Forbidden) is returned.
- The user should be redirected to the login page.
- An appropriate error message should be displayed using i18n keys.

## Reference implementation (Nuxt.js 4)
A full working implementation is available on GitHub:
- **Tickets page (entry point)**: [tickets.vue](https://github.com/towercontrol-io/towercontrol-front/blob/main/app/pages/front/private/tickets.vue)
- **Ticket list component**: [TicketList.vue](https://github.com/towercontrol-io/towercontrol-front/blob/main/app/components/tickets/TicketList.vue)
- **Ticket content & reply component**: [TicketContent.vue](https://github.com/towercontrol-io/towercontrol-front/blob/main/app/components/tickets/TicketContent.vue)
- **API data structures**: [tickets.ts](https://github.com/towercontrol-io/towercontrol-front/blob/main/app/types/tickets.ts)
- **API integration plugin**: [api.backend.tickets.ts](https://github.com/towercontrol-io/towercontrol-front/blob/main/app/plugins/api.backend.tickets.ts)

