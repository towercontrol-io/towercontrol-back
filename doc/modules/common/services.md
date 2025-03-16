# Common Service & Tools catalog

## Tools

### GeolocationTools
Provides tools to solve geolocation problems.
- `isAValidCoordinate` - check if a coordinate is valid
- `distanceBetween` - calculate the distance between two coordinates


## WiFiMacGeolocationService
Provides a Wi-Fi mac geolocation caching and positioning service based on Location recorded by user to map its facilities
Service is associated to configuration entries in `common.properties` file:
- `common.wifimac.cache.size` : Number of element to keep in cache, overridable by environment variable `COMMON_WIFIMAC_CACHE_SIZE`, 0 deactivate cache use
- `common.wifimac.cache.ttl` : Time to live of cache element in **seconds** in cache, overridable by environment variable `COMMON_WIFIMAC_CACHE_TTL`
- `common.wifimac.cache.logperiod` : Period of cache log in **milliseconds**, when >=24h the log is disabled, overridable by environment variable `COMMON_WIFIMAC_CACHE_LOGPERIOD`

Service propose to record locations, they can be manually set with a `CERIFIED` coordinate, so they will be unchanged over
time. `attenuation` allows to have a per hotspot radio model. It's also possible to report new location over time like from a GPS tracker or an external resolution
system. In this case, the status is `VALID`and the position will be updated over time when a better `rssi` is provided.
In case the same **mac** address is reported from different and incompatible locations, the mac address becomes `INVALIDATED` and will not ever been used and updated.

### API
- `getWiFiMacLocation` - search if a mac-address exists in the database / cache and return it
- `upsertWiFiMacLocation` - insert or update a mac-address location in the database / cache
- `getGeoLocation` - return the geolocation of a mac-address based on the database / cache ; associate macs and update location in the meantime



## GoogleWiFiGeolocationService
Based on the Google geolocation API, if provides a position based on the Wi-Fi mac addresses provided.

Service is associated to configuration entries in `common.properties` file:
- `common.google.api.key`: the Google API key, overridable by environment variable `COMMON_GOOGLE_API_KEY`

## ChirpstackIntegrationService
Provides integration with Chirpstack LoRaWAN network server.
