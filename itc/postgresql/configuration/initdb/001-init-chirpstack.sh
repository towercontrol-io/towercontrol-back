#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    create role ${COMMON_PSQL_USER} with login password '${COMMON_PSQL_PASSWORD}';
    create database itc with owner ${COMMON_PSQL_USER};
EOSQL