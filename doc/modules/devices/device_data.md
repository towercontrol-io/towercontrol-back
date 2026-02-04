## Device Data and associated models (TODO)

A device will receive data of different types; these will be decoded and stored. The chosen approach is to have storage 
tables associated with a data type, which will allow specific processing for that data type, for example to merge it 
into an aggregated historical view. Since not all data types can be known, other generic types will allow storing the 
remaining data. The data format is stored in a device family found in the device structure under the name `familyId`.

### Device families 

The concept of "device family" is closely related to the notion of device type or device profile found elsewhere.
It is a set of common characteristics grouped together with the goal of applying a behavior without duplicating
information across all devices. For example, what will be described below: the data that can be extracted from a
received payload. It is possible to have generic profiles that can be shared among users to avoid copying information
while still allowing the inclusion of details specific to a device fleet. For this reason, a family will have a link
to a parent from which missing fields in the referenced family will be retrieved.

Before processing Metrics, there may be a pre-processing phase for the received data, for example a signature 
verification or a decryption that may lead to the rejection of the received frame. This pre-processing is a function 
that can be invoked directly via a generic interface. (TODO)

```json
{
  "id": "string",                     // technical uniq identifier used inside the platform
  "name": "string",                   // i18n slug family name, this is the name of the family, given by device owner or manager
  "description": "string",            // i18n slug family description, this is a free text description of the family, given by device owner or manager
  "enDescription": "string",          // Human readable, english, used as fallback when i18n is not available
  "parentFamilyId": "string",         // link to upper fammilly where some fields can be inherited if not defined here
  "metrics": [ Metric ],              // list of metrics that can be extracted from received frames

  ...

  "customParams": {                   // custom parameters, this is a free form object to store custom parameters for the family
    "key": "value1",
    "value": "value2"
  }
}
```

#### Metrics

A `Metric` is a data point that can be extracted from a received communication. It is not necessarily present in every 
frame; it can be pre-extracted and available in JSON format, but it can also be extracted from a binary frame. 
Therefore, there is a function that extracts the metric and also converts it to the metric's reference unit. Thus, a 
temperature will always be stored in °C as a double for precision, regardless of the unit and precision of the received 
measurement. This allows conversion to °F or K without object-specific processing, and similar data from different 
sensors can be compared and displayed together without post-processing.

The other fields define the metric's name and description. The recommendation is to use a representation that can be 
easily handled by an i18n solution to adapt the UI. For practical reasons and readability, an English translation is 
also stored and can serve as a fallback for display.

```json
{
  "id": "string",                     // technical uniq identifier used inside the platform
  "name": "string",                   // i18n slug metric name, this is the name to identify the metric
  "description": "string",            // i18n slug metric description, this is a free
  "jsonKey" : "string",               // When pre-decoded, in the pivot, to be extracted with this key name ; null / empty when from binary
  "type": "metricType",               // type of metric, based on predefined and generic types (T°, Boolean, Counter, Gauge, etc.)
  "decodingClass": "string",          // decoding class to be used to extract the metric from pivot frame.
  "parsingFunction": "string",        // parsing function to be used to extract the metric from pivot payload (Javascript style, tbd) (TODO)
  ...
}
```

After decoding, metrics are stored in appropriate collections where they can be queried and processed specifically, for 
example to aggregate them. A data point is identified by:
- The deviceId
- The metricId
- The timestamp

If a metric already exists for the same tuple, it will be replaced by the new value (there may be optimizations based 
on the timestamp to avoid unnecessary searches for new data). For traceability, the metric also contains a reference to 
the pivot object from which it originates: this can be used to verify that a data point has been updated rather than 
being new. The key remains the one given above, since for example there may be a CPU temperature and a sensor 
temperature for the same pivot object at the same timestamp.

### Device Data Storage

According to the previous definitions, we have multiple data storages collections for every data type. Each data point 
is basically following this structure:

```json
{
  "deviceId": "string",               // device unique identifier
  "metricId": "string",               // metric unique identifier
  "pivotId": "string",                // pivot unique identifier from which this data point originates
  "timestampMs": "number",            // timestamp in MS since epoch
  "value": "mixed",                   // value of the data point, type depends on metric type (double, int, boolean, string, json...)

  "previousTimestampMs": "number",    // (optional) previous timestamp in MS since epoch, when data point was updated
  "previousValue": "mixed",           // (optional) previous value of the data point, when data point was updated
  ...
}
```

