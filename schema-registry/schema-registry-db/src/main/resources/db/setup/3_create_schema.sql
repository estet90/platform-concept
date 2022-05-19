CREATE SCHEMA schema_registry;

CREATE USER schema_registry_user WITH
    LOGIN
    NOSUPERUSER
    NOCREATEDB
    NOCREATEROLE
    INHERIT
    NOREPLICATION
    CONNECTION LIMIT -1
    PASSWORD 'user';

GRANT CONNECT ON DATABASE schema_registry TO schema_registry_user;