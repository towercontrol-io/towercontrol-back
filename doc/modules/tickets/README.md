## Tickets module

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

Personal data will be marked `<` element to hide `>` in the markdown to be removed from the knowledge base exported. 
Ticket reviewer will care about it.

#### Ticket data model

The data is stored in two main tables in postgresql:
 - `tickets`: contains the ticket's basic information (id, status, priority, user, etc.)
 
    ```json
    {
      "id" : String,                                 // Unique ticket identifier
      "userLogin" : String,                          // Login of the user creating the ticket
      "creationMs" : Long,                           // Creation timestamp in milliseconds
      "closedMs" : Long,                             // Closing timestamp in milliseconds
      "status" : Enum,                               // Ticket status (open, resp_pending, close, close_kb)
      "topic" : String,                              // Ticket topic (Md)
      "content" : String,                            // Ticket content (initial message) (Markdown format)
      "llmDescription" : String,                     // Description for LLM knowledge base (rewritten content) (Md)
      "context" : [ CustomField ],                   // Custom fields providing context information (groups, device...)
                                                     // -------------------------------
                                                     // For later used
      "priority" : Enum,                             // Ticket priority (low, medium, high, urgent)
      "assignedTo" : String                          // Login of the support manager assigned to the ticket
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