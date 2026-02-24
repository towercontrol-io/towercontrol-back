## Implementation example for the contact form page

## Nuxt.js 4
A full implementation for the contact form has been made for Nuxt.js 4. the project is accessible on GitHub at the following URL: [towercontrol-front](https://github.com/towercontrol-io/towercontrol-front)
With the following direct links:
- [Contact Form page](https://github.com/towercontrol-io/towercontrol-front/blob/main/app/pages/front/public/contact.vue)
- [Contact Form component](https://github.com/towercontrol-io/towercontrol-front/blob/main/app/components/tickets/ContactForm.vue)
- [Contact Form API data structures](https://github.com/towercontrol-io/towercontrol-front/blob/main/app/types/tickets.ts)
- [Contact Form API integration](https://github.com/towercontrol-io/towercontrol-front/blob/main/app/plugins/api.backend.tickets.ts)

## Step by step communication with backend

### Step 1 : publish a contact form and get the security challenge
The following structure is filled by the front end and `POST` to backend API endpoint `/tickets/1.0/public/create` ; the 
response is important to go on step 2.

For a contact form, step 1, fields `topic`, `content`, `email` are mandatory, other fields are not set.

#### Data Structure for contact form submission
```typescript
export interface PrivTicketCreationBody {
    /** Ticket short title (Markdown allowed) */
    topic: string;

    /** Ticket content in Markdown */
    content: string;

    /** Contextual custom fields used to add metadata to the ticket */
    context?: CustomField[];

    /** Anonymous user email (if not logged in) empty if logged in */
    email?: string;

    /** Optional technical context information. Not set by used but collected by frontend or smartphone application. */
    techContext?: string;

    /** Confirmation code to validate ticket creation for public users */
    confirmationCode?: string;

    /** True when ticket can be used to enrich FAQ / Knowledge base. Reserved to support managers. */
    faqEligible?: boolean;

    /** True when ticket can be made public in FAQ / Knowledge base. Reserved to support managers. */
    faqPublic?: boolean;

    /** Optional LLM context information to help LLM to better understand the ticket and provide better answers to users. */
    llmContent?: string;
}
```

#### Way to submit the contact form to backend
Here is an example of implementation in type script for Nuxt.js 4, but the code can be adapted to other frameworks.

```typescript
    const ticketsModulePublicCreationPost: string = '/tickets/1.0/public/create';
    
    /**
     * Create a new Tiket (public API)
     */
    ticketsModulePublicCreation: async (body:PrivTicketCreationBody): Promise<{ success?: PrivTicketCreationResponseItf; error?: ActionResult | { message: string } }> => {
        try {
            const response = await apiCallwithTimeout<PrivTicketCreationResponseItf>(
                'POST',
                ticketsModulePublicCreationPost,
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
The response from the API is the following, with a `confirmationCode` that should be used in step 2 to confirm the 
contact form submission. The field `ticketId` is set to 0. The response code is `200` when the contact form submission is successful.

When the response code is `400` or `403` or `50X`, the contact form submission failed and a structure ActionResult is returned, with 
the error message contained in field `message` and corresponding code in field `status_code`. 

In case of error, it's not possible to go to step 2 and user must fix or wait for the issue to be fixed before trying again. The 
error is displayed to the user. The `message` field is an i18n key that can be used to display a translated message to the user, 
the list of i18 keys is available in file [i18n asset file](i18n.md) of this skill.

```typescript

export interface PrivTicketCreationResponseItf {

    /** Ticket ID user can use in communication later, set when the ticket has been created */
    ticketId?: number;

    /** Confirmation code to validate ticket creation for public users, set for public creation only */
    confirmationCode?: string;

}

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
   * Example: "err-user-creation-email-already-exist"
   */
  message: string;
};
```

## Step 2 : confirm the contact form submission with the security challenge
The second step is to confirm the contact form submission with the security challenge. The front end should send a new 
`POST` request to the backend API endpoint `/tickets/1.0/public/create` with the same data as step 1 but with the 
`confirmationCode` field is mandatory and filled with the value received in step 1.

The field `ticketId` is set with the ticket ID. The response code is `201` when the contact form submission is successful.

When the response code is `400` or `403` or `50X`, the contact form submission failed and a structure ActionResult is returned, with
the error message contained in field `message` and corresponding code in field `status_code`.

In case of error, it's not possible to go to step 3 and user must fix or wait for the issue to be fixed before trying again. The
error is displayed to the user. The `message` field is an i18n key that can be used to display a translated message to the user,
the list of i18 keys is available in file [i18n asset file](i18n.md) of this skill.

## Step 3 : display confirmation
When the contact form submission has been a success, the user should receive a confirmation message indicating that their inquiry has 
been received and will be processed. The message is translatable with i18n key like `contact-form-submission-success` that 
should be displayed to the user.
