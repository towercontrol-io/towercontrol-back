---
name: skill_contact_form
description: A skill to create a contact form for users to reach out to support or sales teams. Based on IoT Tower Control backend.
license: GPL-3.0
metadata:
    author: "disk91"
    version: "1.0.0"
---

# Create a contact form page using the IoT Tower Control backend

## Overview
This skill allows you to create a contact form page for users to reach out to support or sales teams. It is based on the 
IoT Tower Control backend, which provides a robust and scalable platform for building IoT platforms. This skills details 
how to create the front-end part of the contact form solution and how to integrate in with the backend. The contact form 
is single page where a not identified user can enter their email, type a topic and a message at destination of the support 
or sales teams.

## When to use this skill
When you've been asked for integrating a contact form in the application.

## How to use this skill
This skill help to generate working code for creating a contact form page. It contains examples in typescript, made for
Nuxt.js 4 but the code can be adapted to other frameworks.

## Principles of the contact form
The contact form is a single page, usually publicly accessible, where a user can enter the following information:
- `Email`: The user's email address, which will be used for communication. 
  - The email should be validated to ensure it is in the correct format.
  - The email is mandatory
- `Topic`: A brief subject or topic of the message, which helps categorize the inquiry.
  - The topic is mandatory
  - The topic accepted format is Markdown
- `Message`: The detailed message or inquiry that the user wants to send to the support or sales teams.
  - The message is mandatory
  - The message accepted format is Markdown

The contact form should have a submit button that, when clicked, sends the information to the backend for processing. 
The way the contact for is submitted is a bit specific to limit the spam and abuse of the contact form, as described above.

When the contact form has been submitted, the user should receive a confirmation message indicating that their inquiry 
has been received and will be processed. In case of an error during submission, the user should receive an appropriate error message:
the backend returns a message translatable with i18n key like `contact-form-submission-error` that should be displayed to the user.

## Workflow of the contact form creation
The file [contact_form.md](assets/contact_form.md) contains the workflow of the contact form creation, with the different 
steps to implement in the front-end and the expected response from the backend. It also contains the structure and API calls 
to be performed with the list of [i18n keys](assets/i18n.md) to use for displaying messages to the user.

As a functional summary:
### Step 1
- The user fills the contact form
- The form syntax is validated on the front-end according to the principles described above.
- It is submitted to the backend API endpoint with a `POST` request to `/tickets/1.0/public/create`.
- The expected response is `200` and it contains a `confirmationCode` that should be used in step 2 to confirm the contact form submission. 

### Step 2
- The software enrich the data with the `confirmationCode` received in step 1 
- It sends a new `POST` request to the backend API endpoint `/tickets/1.0/public/create` with the same data as step 1 but with the `confirmationCode` field is mandatory and filled with the value received in step 1.
- The expected response is `201` when the contact form submission is successful. The field `ticketId` is set with the ticket ID.

### Step 3
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
- [Contact Form API data structures](https://github.com/towercontrol-io/towercontrol-front/blob/main/app/types/tickets.ts)
- [Contact Form API integration](https://github.com/towercontrol-io/towercontrol-front/blob/main/app/plugins/api.backend.tickets.ts)

### `/tickets/1.0/public/create`
It is the only API endpoint used for contact form submission.
- It does not require authentication, this endpoint is public and can be used by not identified users.
- It accepts a `POST` request with the contact form data in the body of the request.
- The POST body structure and response structures are detailed in file [contact_form.md](assets/contact_form.md).

This API endpoints is part of the IoT Tower Control backend, the base url should be an environment variable. You may ask 
developer for it where generating the code for the contact form page if your don't know it.