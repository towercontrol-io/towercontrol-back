#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    create role itc with login password '$PSQL_ITC_PASSWORD';
    create database itc with owner itc;
EOSQL