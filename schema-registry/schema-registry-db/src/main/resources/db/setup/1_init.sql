DROP DATABASE IF EXISTS schema_registry;
DROP USER IF EXISTS schema_registry_admin;

CREATE USER schema_registry_admin WITH
    LOGIN
    NOSUPERUSER
    NOCREATEDB
    CREATEROLE
    INHERIT
    NOREPLICATION
    CONNECTION LIMIT -1
    PASSWORD 'admin';

CREATE DATABASE schema_registry
    WITH OWNER = schema_registry_admin
    ENCODING = 'UTF8'
    CONNECTION LIMIT = -1;

GRANT CONNECT ON DATABASE schema_registry TO schema_registry_admin;
