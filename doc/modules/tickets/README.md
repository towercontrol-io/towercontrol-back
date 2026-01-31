## Tickets module

Ticket management module handles support interactions with users. This modules is part of the Non Community Edition 
'NCE' of the platform.

### Purpose

The tickets management module handles support interactions with users:
- By creating support tickets
- By maintaining a knowledge base that can be used by an LLM
- By providing contact forms that can be processed in the same way as a support request.

Users with the `ROLE_SUPPORT_MANAGER` role will be able to handle tickets, respond to them, and close them.
Users with the `ROLE_SUPPORT_USER` role will be able to create tickets this should be default but it can be used to 
deactivate the feature.

### Data model, principes

Support data will be an exchange between the user and `ROLE_SUPPORT_MANAGER`, but they will separate as much as possible 
the technical data associated with a context, the _personal_ data which do not constitute an interesting set to index 
and which should even be removed from a knowledge base, and the relevant data in the form of the question asked (which 
can be rewritten) and the answer given, which can be enriched with parts not visible to the user.

This data can be exported in Markdown format to form a knowledge base for an LLM enabling automatic answer generation 
and the construction of FAQs or suggested replies while the user is typing their question.

Personal data will be marked `%` element to hide `%` in the Markdown to be removed from the knowledge base exported. 
Ticket reviewer will care about it. Personal data are not displayed to admin & user until the admin decide to unmask 
them for a single access. This action is traced in audit log.

### Anonymous Tickets

The module also supports the creation of anonymous tickets which can be created, for example, via contact forms.
In this case, the model is special because there is no user account and we are required to keep the user's email address
to reply to them. In that case, the `login` field will be used to store the interlocutor's email and it must be encrypted.
In the `login` field, we will then find a prefix `email_` followed by the encrypted email.

### Ticket responses

It must be possible for a user to reply to tickets easily via a direct link that does not require being logged in. This
link must be limited to a single reply and be time‑limited. This way it is possible to consult and reply from the
notification email without being logged in. This must apply only to the ticket author. The responding administrator
must be logged in.

### FAQ Tickets

A ticket can become an FAQ entry: it is a ticket with a defined subject and an associated answer in the ticket body that 
can generally be used as a basis for building an FAQ. Typically, a ticket created by a support manager who wants to save 
a template question/answer for users to consult. Some FAQs can be publicly accessible without logging in, while others 
are available only to authenticated users.

#### Ticket data model

The data is stored in two main tables in postgresql:
 - `tickets`: contains the ticket's basic information (id, status, priority, user, etc.)
 
    ```json
    {
      "id" : String,                                 // Unique ticket identifier
      "ticketId" : int,                              // Easier Id for user to track ticket in list
      "userLogin" : String,                          // Login of the user creating the ticket
      "creationMs" : Long,                           // Creation timestamp in milliseconds
      "closedMs" : Long,                             // Closing timestamp in milliseconds
      "status" : Enum,                               // Ticket status (open, resp_pending, close, close_kb)
      "topic" : String,                              // Ticket topic (Md)
      "content" : String,                            // Ticket content (initial message) (Markdown format)
      "llmDescription" : String,                     // Description for LLM knowledge base (rewritten content) (Md)
      "context" : [ CustomField ],                   // Custom fields providing context information (groups, device...)
      "faqEligible" : boolean,                       // true when ticket can be used to enrich FAQ / Knowledge base
      "faqPublic" : boolean,                         // true when ticket can be made public in FAQ / Knowledge base
                                                     // -------------------------------
                                                     // For later used
      "priority" : Enum,                             // Ticket priority (low, medium, high, urgent)
      "assignedTo" : String                          // Login of the support manager assigned to the ticket
      "directAccessToken" : String,                  // Token allowing direct access to the ticket for the user (reply without login)
      "directAccessExpiryMs" : Long,                 // Expiry timestamp for the direct access token
      "techContext" : String,                        // Technical context information like sent by application
      "userPending" : boolean,                       // true when admin added a message and user response is pending
      "adminPending" : boolean                       // true when user added a message and admin response is pending
    }
    ```

- `ticket_messages`: contains the messages exchanged within a ticket (id, ticket_id, message, author, date, etc.)

    ```json
    {
      "id" : String,                                 // Unique ticket response identifier    
      "ticketId" : String,                           // Ref to ticket Id
      "responderLogin" : String,                     // Login of the response author
      "creationMs" : Long,                           // Creation timestamp in milliseconds
      "content" : String,                            // Ticket content (initial message) (Markdown format)
      "llmDescription" : String                      // Description for LLM knowledge base (rewritten content) (Md)
    }
    ```
  
## Behavior

When a ticket is created, a message is sent to the user responsible for handling it. This is a unique email account 
that will be configured in the properties file via `tickets.manager.email`. If it is empty, no email will be sent and 
open tickets are accessible in the interface to users who have the `ROLE_SUPPORT_MANAGER` role.

When properties `tickets.discord.webhook.url` is set, a notification is also set on the Discord webhook to notify on 
ticket creation and updates.

When an administrator sends a reply to a user, an email is sent to the user to inform them provided that `tickets.email.user.update` 
is true.