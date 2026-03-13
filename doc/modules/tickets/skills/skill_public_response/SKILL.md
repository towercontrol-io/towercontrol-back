---
name: skill_public_response
description: A skill to allow anonymous users to view and respond to tickets they created. Based on IoT Tower Control backend.
license: GPL-3.0
metadata:
    author: "disk91"
    version: "1.0.0"
---

# View and respond to public anonymous tickets using the IoT Tower Control backend

## Overview
This skill allows anonymous users who created a ticket through the contact form to view and respond to their ticket 
without needing to authenticate. It is based on the IoT Tower Control backend, which provides a robust and scalable 
platform for building IoT platforms. This skill details how to create the front-end part of the public ticket response 
solution and how to integrate it with the backend. The public ticket response page is accessible via a specific URL 
containing the ticket ID and an access key.

## When to use this skill
When you've been asked to implement a page that allows anonymous users to view and respond to their support tickets 
without authentication.

## How to use this skill
This skill helps to generate working code for creating a public ticket response page. It contains examples in TypeScript, 
made for Nuxt.js 4 but the code can be adapted to other frameworks.

## Principles of the public ticket response
The public ticket response is a page accessible without authentication where users can:
- View the content of a ticket they created. Ticket content is Markdown formatted
- See all responses from support team and their own responses. Responses are Markdown formatted
- Add new responses in Markdown format
- Access is controlled by a unique access key provided when the ticket was created and pass to the page as a parameter.

The ticket content and responses are displayed in chronological order. Users can distinguish between messages from 
support staff and their own messages. The page is accessed via a URL like: `/front/public/contact-view?ticketId=6&accessKey=vwdf1773dsfhjskhsd` where 
`ticketId` is the ID of the ticket and `accessKey` is the unique access key that authorizes access to the ticket details and responses.

## Public ticket response data structure
The ticket detail contains the following information:
- `id`: Ticket identifier (Number) - The unique ID of the ticket
- `content`: Ticket content (String, Markdown) - The original ticket content
- `responses`: List of responses (Array) - All responses to the ticket, each containing:
  - `id`: Response identifier (String)
  - `creationMs`: Response date in milliseconds since epoch (Number)
  - `content`: Response content (String, Markdown)
  - `fromUser`: Boolean indicating if the message is from the user (true) or support staff (false)

## Workflow of the public ticket response
The file [public_response.md](assets/public_response.md) contains the workflow of the public ticket response, with the 
different steps to implement in the front-end and the expected response from the backend. It also contains the structure 
and API calls to be performed with the list of [i18n keys](assets/i18n.md) to use for displaying messages to the user.

As a functional summary:
### Step 1: Retrieve ticket details
- The front-end makes a `GET` request to `/tickets/1.0/public/ticket`
- Required query parameters:
  - `ticketId`: The ticket ID (Number)
  - `accessKey`: The access key to authorize access (String)
- The expected response is `200` with the ticket details including all responses
- If access is denied (invalid key), the API returns `403`
- If the ticket is not found, the API returns `400`
- If the ticket has been expired, the API returns `410` (Gone). It's important to display this information to the user when returned.

### Step 2: Display ticket content and responses
- Display the original ticket content in **Markdown** format
- Display all responses in chronological order, also in **Markdown** format
- Distinguish visually between user messages and support staff messages using the `fromUser` field
- Show the date of each response using the `creationMs` field

### Step 3: Add a new response
- The user can add a new response using a text area with Markdown support
- The front-end makes a `PUT` request to `/tickets/1.0/public/ticket`
- Required body fields:
  - `id`: Ticket ID (Number)
  - `content`: Response content in Markdown (String)
  - `authKey`: Access key to authorize the action (String)
  - `closeTicket`: Set to false unless the user wants to close the ticket (Boolean)
- The expected response is `200` when the response is successfully added
- After adding a response, refresh the ticket details to display the new response

### Step 4: Handle errors
- If the API returns an error (400, 403, 410 or 50x), display an appropriate error message
- Use i18n keys for error messages to support internationalization
- Common errors include: invalid access key, ticket not found, content validation errors

## API endpoints used for public ticket response
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
- [Tickets API data structures](https://github.com/disk91/IoTowerFront/blob/main/app/types/tickets.ts)
- [Tickets API integration](https://github.com/disk91/IoTowerFront/blob/main/app/plugins/api.backend.tickets.ts)

### `GET /tickets/1.0/public/ticket`
This is the API endpoint used to retrieve a public ticket's details.
- It does not require authentication, but requires a valid access key
- It accepts a `GET` request with query parameters `ticketId` and `accessKey`
- The response structure is detailed in file [public_response.md](assets/public_response.md)

### `PUT /tickets/1.0/public/ticket`
This is the API endpoint used to add a response to a public ticket.
- It does not require authentication, but requires a valid access key in the body
- It accepts a `PUT` request with the response data in the body
- The request and response structures are detailed in file [public_response.md](assets/public_response.md)

These API endpoints are part of the IoT Tower Control backend, the base URL should be an environment variable. You may ask 
the developer for it when generating the code for the public ticket response page if you don't know it.

