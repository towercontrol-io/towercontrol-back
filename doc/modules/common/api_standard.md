## API standard
### Return codes
- 200: action has been a success (GET, PUT, DELETE, POST)
- 201: creation has been a success (POST with creation)
- 202: action has been accepted and async process has started (GET,POST)
- 204: the query is correct but no data is available (GET)
- 206: the request returns only a part of the requested content (GET)
- 400: the query is incorrect, parameter format is incorrect
- 404: the query is incorrect, the passed element does not exist
- 403: the rights are not sufficient to perform the action
- 409: the creation failed as the document already exists
- 418: (I'm a teapot) when a hacking scenario is detected
- 425: (too early) when an async request result query comes until the end of computation or over quota API
- 429: (too many requests) when the user has reached the limit of requests

Every error code provides the following structure (ActionResult class):
```json
{
    "status": "string",   // error code 
    "message": "string"   // i18n ready message : err-modulename-context-message-description
}
```

