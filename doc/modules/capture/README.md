## Capture module

This module is responsible for receiving data from external sensors via different protocols. It can operate in push or 
pull mode with scheduled data collection. It therefore consists of different services that will adapt to the needs of 
each type of interface.

Its role is not to interpret the data which, in general, are converted to a pivot format that can be processed at a 
higher level.

Data are stored in raw form so they can be replayed later and for each data item a unique object will be propagated to 
allow updating the entire chain.

Currently supported integrations:

#### Push

- HTTPS endpoint (REST API) with JSON, authentication using a JWT token derived from an API key. Text format (JSON)

Planned for the future

#### Push

- MQTT(s) with JSON and Protobuf formatting, authentication by client certificate or username/password

#### Pull

- HTTP(s) with JSON and Protobuf formatting, authentication via header (api_key or JWT token)

### Natively supported integrations
- `Helium / Chirpstack` via HTTP(s) with JSON formatting, authentication via header (api_key or JWT token)