---
name: skill_public_faq
description: A skill to display public FAQ entries that don't require authentication. Based on IoT Tower Control backend.
license: GPL-3.0
metadata:
    author: "disk91"
    version: "1.0.0"
---

# Display public FAQ entries using the IoT Tower Control backend

## Overview
This skill allows you to create a public FAQ page that displays frequently asked questions and answers. It is based on 
the IoT Tower Control backend, which provides a robust and scalable platform for building IoT platforms. This skill details 
how to create the front-end part of the public FAQ solution and how to integrate it with the backend. The public FAQ page 
is accessible without authentication and can be used on public websites to provide self-service support to users.

## When to use this skill
When you've been asked to integrate a public FAQ page in the application that can be accessed by anonymous users.

## How to use this skill
This skill helps to generate working code for creating a public FAQ page. It contains examples in TypeScript, made for
Nuxt.js 4 but the code can be adapted to other frameworks.

## Principles of the public FAQ
The public FAQ is a page accessible without authentication where users can:
- Browse a list of frequently asked questions and answers
- View FAQ entries with topics and detailed content in Markdown format
- Navigate through paginated results if there are many FAQ entries

The FAQ entries are created and managed by support managers through the backend API. The public FAQ page only displays 
entries that have been marked as public by support managers.

## Public FAQ data structure
Each FAQ entry contains the following information:
- `id`: Unique FAQ identifier (Number) - The unique ID of the FAQ entry
- `topic`: FAQ topic (String, Markdown) - The question or topic of the FAQ entry
- `content`: FAQ content (String, Markdown) - The detailed answer or content of the FAQ entry
- `totalFaq`: Total number of FAQ entries (Number) - Used for pagination to know the total number of entries available

## Workflow of the public FAQ display
The file [public_faq.md](assets/public_faq.md) contains the workflow of the public FAQ display, with the different 
steps to implement in the front-end and the expected response from the backend. It also contains the structure and API calls 
to be performed with the list of [i18n keys](assets/i18n.md) to use for displaying messages to the user.

As a functional summary:
### Step 1: Retrieve FAQ entries
- The front-end makes a `GET` request to `/tickets/1.0/public/faq`
- Optional query parameters can be used for pagination:
  - `page`: Page number (default: 0)
  - `size`: Number of entries per page (default: 10)
- The expected response is `200` with an array of FAQ entries

### Step 2: Display FAQ entries
- Display the list of FAQ entries received from the backend
- (Important) Each entry shows the topic and content in **Markdown** format
- Implement pagination controls using the `totalFaq` field to calculate total pages

### Step 3: Handle errors
- If the API returns an error (400 or 50x), display an appropriate error message
- Use i18n keys for error messages to support internationalization

## API endpoint used for public FAQ retrieval
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

### `/tickets/1.0/public/faq`
This is the API endpoint used for public FAQ retrieval.
- It does not require authentication, this endpoint is public and can be used by anonymous users.
- It accepts a `GET` request with optional query parameters for pagination.
- The response structure is detailed in file [public_faq.md](assets/public_faq.md).

This API endpoint is part of the IoT Tower Control backend, the base URL should be an environment variable. You may ask 
the developer for it when generating the code for the public FAQ page if you don't know it.

