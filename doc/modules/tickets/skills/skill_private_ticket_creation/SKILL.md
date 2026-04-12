---
name: skill_private_ticket
description: A skill to create a ticket, as an identified user. 
license: GPL-3.0
metadata:
    author: "disk91"
    version: "1.0.0"
---

# Create a new user identified support ticket

## Overview
This skill allows you to create a support ticket creation page for identified users . It is based on the 
IoT Tower Control backend, which provides a robust and scalable platform for building IoT platforms. This skill details 
how to create the front-end part of the support ticket creation and how to integrate in with the backend. The ticket creation form 
is used by an identified user to create a support request. It can also be automated by an application to report a bug or a crash.

## When to use this skill
When you've been asked for integrating a support ticket creation form in the application.

## How to use this skill
This skill help to generate working code for creating a support ticket form page. It contains examples in typescript, made for
Nuxt.js 4 but the code can be adapted to other frameworks.

## Principles of the contact form
The contact form is a single page, where a user can enter the following information:
- `Topic`: A brief subject or topic of the message, which helps categorize the inquiry.
  - The topic is mandatory
  - The topic accepted format is Markdown
- `Content`: The detailed message or inquiry that the user wants to send to the support or sales teams.
  - The message is mandatory
  - The message accepted format is Markdown
  - When the message contains personal information, they MUST be marked with `~~` syntax, for example: `~~my personal information~~`. This is to ensure that the backend can easily identify and handle personal information in compliance with data protection regulations.
Some field may be competed by the application but not by the user himself:
- `TechContext` **OPTIONAL** :  A technical context that can be used to provide additional information about the user's environment, such as device type, operating system, or any relevant technical details that can assist the support team in understanding and addressing the inquiry more effectively. This field is optional but can be very helpful for troubleshooting technical issues.
  - This is optional
  - The message accepted format is Markdown
  - Text area, no formatting
- `llmContent` **OPTIONAL** : A content that can be used to provide additional information about the user's inquiry.
  - This is optional
  - This content will be used later by an LLM. 
  - It can be more detailed, especially with specific instructions for LLMs. 
  - It will not be displayed to the user and will replace the user content in the information sent to RAG or the LLM.
  - The message accepted format is Markdown
- `TechContext` **OPTIONAL** : is a set of key-value pairs used to provide context information that can be reused later.
  - This is optional
  - For example, you can specify an operating system: the key would be `operating_system`, and the value could be `Linux`, `Android`, or `iOS`.
  - There are no predefined default values. It is up to the developer to choose the keys and values that matter to them. 
  - The key accepted format is standard string, without spaces, and the value accepted format is String or Markdown.

The contact form should have a submit button that, when clicked, sends the information to the backend for processing. 

When the contact form has been submitted, the user should receive a confirmation message indicating that their inquiry 
has been received and will be processed. In case of an error during submission, the user should receive an appropriate error message:
the backend returns a message translatable with i18n key like `contact-form-submission-error` that should be displayed to the user.

In the case of a 403 error, the user may not have the required access rights to use the Support feature. In this situation, 
a specific error message can be returned indicating that they do not have permission, and that they must use the standard 
contact system to get help.

## File Attachments

Users can attach one or more files to a support ticket using the Files module API.

### Principles
- Files are uploaded **before** the ticket is submitted, via `POST /files/1.0/upload`.
- Ticket attachments must always use:
  - `accessType = PRIVATE`
  - `withAccessKey = true` — enables direct link access without requiring authentication on the image `src` URL
- Each uploaded file is referenced in the ticket's `context` field as a `CustomField`:
  - `key`: the file type label, freely chosen by the user (e.g., `screenshot`, `log`, `config`, `document`)
  - `value`: the string `"file_"` followed by the file's `uniqueName` (e.g., `"file_550e8400-...-1712345678901.jpg"`)
- Multiple files can be attached; each produces one `CustomField` entry in `context`.
- For **image files** (`mimeCategory === "IMAGE"`), the front-end must offer a **"Copy Markdown link"** button
  that generates and copies to the clipboard the following Markdown image syntax:
  ```
  ![originalName](BACKEND_API_BASE/files/1.0/{uniqueName}/full?key={accessKey})
  ```
  This can be pasted directly into the ticket `content` field to embed the image inline.
- The Files module API endpoints and full TypeScript data structures are documented in
  [ticket_form.md](assets/ticket_form.md).

## Workflow of the ticket form creation
The file [ticket_form.md](assets/ticket_form.md) contains the workflow of the ticket form creation, with the different 
steps to implement in the front-end and the expected response from the backend. It also contains the structure and API calls 
to be performed with the list of [i18n keys](assets/i18n.md) to use for displaying messages to the user.

As a functional summary:
### Step 1
- The user or the application fills the contact form
- The application complete the optional fields if required.
- The form syntax is validated on the front-end according to the principles described above.
- It is submitted to the backend API endpoint with a `POST` request to `/tickets/1.0/ticket`.
- The expected response is `201` when the contact form submission is successful. The field `ticketId` is set with the ticket ID.

### Step 1.5 (optional) — Attach files
- The user may attach one or more files before submitting the ticket.
- Each file is uploaded individually via `POST /files/1.0/upload` with `accessType=PRIVATE` and `withAccessKey=true`.
- On successful upload the file reference is added to the `context` array as a `CustomField`.
- For image uploads a thumbnail preview is shown and a **"Copy Markdown link"** button is provided.
- See [ticket_form.md](assets/ticket_form.md) for the full file attachment workflow.

### Step 2
- When the contact form submission has been a success, the user should receive a confirmation message indicating that their inquiry has been received and will be processed.
- The ticket ID is displayed to the user as a reference for future communication.

## API endpoint used for the ticket creation
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
- [Ticket API data structures](https://github.com/towercontrol-io/towercontrol-front/blob/main/app/types/tickets.ts)
- [Ticket API integration](https://github.com/towercontrol-io/towercontrol-front/blob/main/app/plugins/api.backend.tickets.ts)

### `/tickets/1.0/ticket`
It is the only API endpoint used for ticket form submission with an identified user.
- It requires authentication so the Bearer token must be included in the request headers
- It accepts a `POST` request with the contact form data in the body of the request.
- The POST body structure and response structures are detailed in file [ticket_form.md](assets/ticket_form.md).

### `POST /files/1.0/upload`
This endpoint is used to upload file attachments before submitting the ticket.
- It requires authentication (Bearer token).
- It accepts a `multipart/form-data` request.
- Must be called with `accessType=PRIVATE` and `withAccessKey=true` for ticket attachments.
- Full request/response structures and TypeScript examples are in [ticket_form.md](assets/ticket_form.md).

This API endpoints is part of the IoT Tower Control backend, the base url should be an environment variable. You may ask 
developer for it where generating the code for the contact form page if your don't know it.

## Authentication requirements
The private ticket endpoints requires:
- User must be authenticated (valid session/token)
- Authentication token must be included in the request headers (typically as Bearer token)

If authentication fails:
- HTTP 403 (Forbidden) response will be returned
- User should be redirected to the login page
- Display an appropriate error message using i18n keys
