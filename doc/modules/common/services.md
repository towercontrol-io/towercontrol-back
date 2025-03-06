# Common Service & Tools catalog

## Tools

### GeolocationTools
Provides tools to solve geolocation problems.
- `isAValidCoordinate` - check if a coordinate is valid
- `distanceBetween` - calculate the distance between two coordinates

## GoogleWiFiGeolocationService
Based on the Google geolocation API, if provides a position based on the WiFi mac addresses provided.

Service is associated to configuration entries in `common.properties` file:
- `common.google.api.key`: the Google API key, overridable by environment variable `COMMON_GOOGLE_API_KEY`

## ChirpstackIntegrationService
Provides integration with Chirpstack LoRaWAN network server.
