## Configuration files

### Service Configuration files
Each of the services have its own configuration file. Configuration file is placed at root of the java execution in a 
directory name `configuration`. This directory contains one file per service with the name of the service.

### Common service configuration
The common properties are located in `configuration/common.properties`. This file contains the following properties:
- `common.psql.url`: the database postgresql url ; usually setup in regard of the container configuration for a monolithic deployment.
The env properties `COMMON_PSQL_HOST` and `COMMON_PSQL_PORT` are used to override the url.
- `common.psql.username`: the database username - possible override by the environment variable `COMMON_PSQL_USER`
- `common.psql.password`: the database password - possible override by the environment variable `COMMON_PSQL_PASSWORD`


- `common.mongo.sharding.enabled` : when true the mongodb will be configured as a sharding database.