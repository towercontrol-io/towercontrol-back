## Implementation example for the ticket creation form

## Nuxt.js 4
A full implementation for the contact form has been made for Nuxt.js 4. the project is accessible on GitHub at the following URL: [towercontrol-front](https://github.com/towercontrol-io/towercontrol-front)
With the following direct links:
- [Ticket list & creation page](https://github.com/towercontrol-io/towercontrol-front/blob/main/app/pages/front/private/tickets.vue)
- [Ticket Creation Form component](https://github.com/towercontrol-io/towercontrol-front/blob/main/app/components/tickets/TicketForm.vue)
- [Ticket Creation API data structures](https://github.com/towercontrol-io/towercontrol-front/blob/main/app/types/tickets.ts)
- [Ticket Creation API integration](https://github.com/towercontrol-io/towercontrol-front/blob/main/app/plugins/api.backend.tickets.ts)

## Step by step communication with backend

### Step 1 : publish ticket creation form 
For a ticket form, step 1, fields `topic`, `content`, are mandatory, other fields are not set.

It is possible to add automatic elements to the content, such as the name of users. In this case, it is personal data, 
so it is important to explicitly mark it as personal data using the strikethrough symbol `~~` so that the backend can 
protect it by encrypting it and obfuscating its use later, for example when training an LLM.

### Step 2 : enrich the ticket with metadata (OPTIONAL)

The program can decide to add additional fields to define metadata and, among other things, enrich the context. 
The `context` field  is a set of key-value pairs where you can enter information such as the mobile brand, the browser 
used, the screen resolution, and so on. It always follows the same model: a `key` field defines the type of value that follows, and the 
`value` is a free-text field.

The `techContext` element can also be filled in the context text field, which is a free field that can be formatted 
as desired, but is less convenient for search. These fields are left to the developer’s discretion, who must ensure a 
certain level of consistency so that the data can later be leveraged.

### Step 3 : submit the form to backend
The following structure is filled by the front end and `POST` to backend API endpoint `/tickets/1.0/ticket` ; the
response is important to go on step 2.

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


export interface CustomField {
    // Define CustomField interface based on your needs
    key: string;
    value: string;
}
```

#### Way to submit the contact form to backend
Here is an example of implementation in type script for Nuxt.js 4, but the code can be adapted to other frameworks.

```typescript
    const ticketsModuleCreatePost: string = '/tickets/1.0/ticket';
    
    /**
     * Create a new Ticket (private API)
     */
    ticketsModulePrivateCreation: async (body:PrivTicketCreationBody): Promise<{ success?: PrivTicketCreationResponseItf; error?: ActionResult | { message: string } }> => {
        try {
            const response = await apiCallwithTimeout<PrivTicketCreationResponseItf>(
                'POST',
                ticketsModuleCreatePost,
                body,
                false
            );
            return { success: response }
        } catch (error : any) {
            return { error };
        }
    },
```

#### Response from the API
The API should respond `201` and the response defined above should contain the ticket ID in field `ticketId` when the ticket 
has been created successfully. 

When the response code is `400` or `403` or `50X`, the contact form submission failed and a structure ActionResult is returned, with 
the error message contained in field `message` and corresponding code in field `status_code`. 

In case of error, it's not possible to go to step 4 and user must fix or wait for the issue to be fixed before trying again. The 
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

## Step 4 : display confirmation
When the contact form submission has been a success, the user should receive a confirmation message indicating that their inquiry has 
been received and will be processed. The message is translatable with i18n key like `contact-form-submission-success` that 
should be displayed to the user.
