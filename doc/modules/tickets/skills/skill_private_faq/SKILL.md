---
name: skill_private_faq
description: A skill to display private FAQ entries that require authentication. Based on IoT Tower Control backend.
license: GPL-3.0
metadata:
    author: "disk91"
    version: "1.0.0"
---

# Display private FAQ entries using the IoT Tower Control backend

## Overview
This skill allows you to create a private FAQ page that displays frequently asked questions and answers for authenticated users. 
It is based on the IoT Tower Control backend, which provides a robust and scalable platform for building IoT platforms. 
This skill details how to create the front-end part of the private FAQ solution and how to integrate it with the backend. 
The private FAQ page requires authentication and provides additional FAQ content that is only accessible to registered users.

## When to use this skill
When you've been asked to integrate a private FAQ page in the application that requires user authentication.

## How to use this skill
This skill helps to generate working code for creating a private FAQ page. It contains examples in TypeScript, made for
Nuxt.js 4 but the code can be adapted to other frameworks.

## Principles of the private FAQ
The private FAQ is a page accessible only to authenticated users where they can:
- Browse a list of frequently asked questions and answers specific to registered users
- View FAQ entries with topics and detailed content in Markdown format
- Navigate through paginated results if there are many FAQ entries
- Access additional information not available in the public FAQ

The FAQ entries are created and managed by support managers through the backend API. The private FAQ page displays 
entries that require authentication and may contain more detailed or sensitive information compared to public FAQ entries.

## Private FAQ data structure
Each FAQ entry contains the following information:
- `id`: Unique FAQ identifier (Number) - The unique ID of the FAQ entry
- `topic`: FAQ topic (String, Markdown) - The question or topic of the FAQ entry
- `content`: FAQ content (String, Markdown) - The detailed answer or content of the FAQ entry
- `totalFaq`: Total number of FAQ entries (Number) - Used for pagination to know the total number of entries available

## Workflow of the private FAQ display
The file [private_faq.md](assets/private_faq.md) contains the workflow of the private FAQ display, with the different 
steps to implement in the front-end and the expected response from the backend. It also contains the structure and API calls 
to be performed with the list of [i18n keys](assets/i18n.md) to use for displaying messages to the user.

As a functional summary:
### Step 1: Verify authentication
- Ensure the user is authenticated before accessing the private FAQ page
- Redirect to login page if the user is not authenticated

### Step 2: Retrieve FAQ entries
- The front-end makes an authenticated `GET` request to `/tickets/1.0/faq`
- Include the authentication token in the request headers
- Optional query parameters can be used for pagination:
  - `page`: Page number (default: 0)
  - `size`: Number of entries per page (default: 10)
- The expected response is `200` with an array of FAQ entries

### Step 3: Display FAQ entries
- Display the list of FAQ entries received from the backend
- (Important) Each entry shows the topic and content in **Markdown** format
- Implement pagination controls using the `totalFaq` field to calculate total pages

### Step 4: Handle errors
- If the API returns an error (400, 403 or 50x), display an appropriate error message
- Handle 403 errors specifically as authentication/authorization failures
- Use i18n keys for error messages to support internationalization

## API endpoint used for private FAQ retrieval
The API endpoint is relative to an API base URL that is provided via an environment variable. In a Nuxt.js implementation, 
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
- [FAQ API data structures](https://github.com/disk91/IoTowerFront/blob/main/app/types/tickets.ts)
- [FAQ API integration](https://github.com/disk91/IoTowerFront/blob/main/app/plugins/api.backend.tickets.ts)

### `/tickets/1.0/faq`
This is the API endpoint used for private FAQ retrieval.
- It requires authentication so the Bearer token must be included in the request headers
- It accepts a `GET` request with optional query parameters for pagination
- The authentication token must be included in the request headers
- The response structure is detailed in file [private_faq.md](assets/private_faq.md)

This API endpoint is part of the IoT Tower Control backend, the base URL should be an environment variable. You may ask 
the developer for it when generating the code for the private FAQ page if you don't know it.

## Authentication requirements
The private FAQ endpoint requires:
- User must be authenticated (valid session/token)
- Authentication token must be included in the request headers (typically as Bearer token)

If authentication fails:
- HTTP 403 (Forbidden) response will be returned
- User should be redirected to the login page
- Display an appropriate error message using i18n keys

