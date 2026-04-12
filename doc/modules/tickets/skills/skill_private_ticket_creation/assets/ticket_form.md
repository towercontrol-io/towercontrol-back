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

### Step 2.5 (OPTIONAL) : attach files to the ticket

Before submitting the ticket, the user can attach one or more files. Files are managed by the Files module API.

#### File upload API endpoint
Files are uploaded to `POST /files/1.0/upload` as `multipart/form-data`.

| Form field      | Required | Description                                                                    |
|-----------------|----------|--------------------------------------------------------------------------------|
| `file`          | yes      | Binary file content                                                            |
| `accessType`    | yes      | Always `PRIVATE` for ticket attachments                                        |
| `description`   | no       | Short description (e.g. the ticket topic)                                      |
| `fileName`      | no       | Original filename provided by the client                                       |
| `withAccessKey` | yes      | Always `true` — generates the access key required for direct URL image loading |

#### File upload response structure

```typescript
export interface FileUploadResponseItf {
    /** Unique technical identifier (UUID) */
    id: string;

    /** Generated unique filename used as file identifier in all URLs */
    uniqueName: string;

    /** Original filename provided at upload time */
    originalName: string;

    /** Optional human-readable description */
    description?: string;

    /** MIME category: "IMAGE", "PDF", "TEXT", or "GENERIC" */
    mimeCategory: 'IMAGE' | 'PDF' | 'TEXT' | 'GENERIC';

    /** Full detected MIME type (e.g. "image/png") */
    mimeType: string;

    /** File size in bytes */
    size: number;

    /** Login of the file owner */
    ownerId: string;

    /** Access control (always "PRIVATE" for ticket attachments) */
    accessType: string;

    /** Number of times the file has been downloaded */
    accessCount: number;

    /** Creation timestamp in milliseconds since epoch */
    createdAt: number;

    /** 16-character access key enabling unauthenticated URL access */
    accessKey: string;

    /** Unique filename of the generated thumbnail (images only, null otherwise) */
    thumbnailUniqueName?: string;
}
```

#### TypeScript example for file upload

```typescript
const filesModuleUploadPost: string = '/files/1.0/upload';

/**
 * Upload a file as a PRIVATE attachment with an access key for a ticket
 */
filesModuleTicketUpload: async (
    file: File,
    description?: string
): Promise<{ success?: FileUploadResponseItf; error?: ActionResult | { message: string } }> => {
    try {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('accessType', 'PRIVATE');
        formData.append('withAccessKey', 'true');
        if (description) formData.append('description', description);
        if (file.name)   formData.append('fileName', file.name);

        // multipart/form-data upload — do NOT set Content-Type header manually
        const response = await apiCallMultipartWithTimeout<FileUploadResponseItf>(
            'POST',
            filesModuleUploadPost,
            formData,
            false  // authentication required
        );
        return { success: response };
    } catch (error: any) {
        return { error };
    }
},
```

#### Mapping an uploaded file to a ticket context field

Once a file is successfully uploaded, append a `CustomField` entry to the ticket body's `context` array:

```typescript
// fileType is the label chosen by the user, e.g. "screenshot", "log", "config"
const customField: CustomField = {
    key:   fileType,
    value: `file_${uploadedFile.uniqueName}`
};
ticketBody.context = [...(ticketBody.context ?? []), customField];
```

#### Markdown link for images

When the uploaded file is an image (`mimeCategory === "IMAGE"`), generate a Markdown image link so the user
can paste it directly into the ticket `content` text area to embed the image inline:

```typescript
function getMarkdownImageLink(
    file: FileUploadResponseItf,
    backendBaseUrl: string
): string {
    return `![${file.originalName}](${backendBaseUrl}/files/1.0/${file.uniqueName}/full?key=${file.accessKey})`;
}
```

Display a **"Copy Markdown link"** button next to each uploaded image that copies this string to the clipboard.

#### Attachment list UI

Render the list of attached files below the content text area. For each file:
- Display the original filename and the user-chosen type label.
- For images (`mimeCategory === "IMAGE"`):
  - Show a thumbnail preview using:
    `GET /files/1.0/{uniqueName}/thumbnail?key={accessKey}`
  - Show a **"Copy Markdown link"** button.
- Allow the user to remove a file from the attachment list. Removing a file only discards the `CustomField`
  from the `context` array; the file remains in the files module (the user can delete it separately if needed).

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
