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

#### How to setup Helium / Chirpstack integration (case of single tenant)
1) Create an API key with the roles `ROLE_GLOBAL_CAPTURE` and `ROLE_BACKEND_CAPTURE` and `ROLE_DEVICE_WRITE`
2) Generate the JWT token for this API key (valid for 10 years for example)
3) Create an endpoint with corresponding protocol, this endpoint is owned by the same user as the JWT owner.
4) On Chirpstack, create an HTTP integration with the following parameters:
   - Endpoint URL: `https://<your_domain>/capture/1.0/ingest/<your_endpoint_id>/`
   - Headers: `Authorization` and value `Bearer <your_jwt_token>`
   - Uplink Data Template: default (you can customize it later if needed)