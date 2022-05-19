GRANT USAGE ON SCHEMA schema_registry TO schema_registry_user;
GRANT ALL ON ALL TABLES IN SCHEMA schema_registry TO schema_registry_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA schema_registry GRANT ALL ON TABLES TO schema_registry_user;
GRANT ALL ON ALL SEQUENCES IN SCHEMA schema_registry TO schema_registry_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA schema_registry GRANT ALL ON SEQUENCES TO schema_registry_user;