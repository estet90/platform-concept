DROP SCHEMA IF EXISTS schema_registry CASCADE;

DO
$$
    BEGIN
        IF EXISTS(SELECT FROM pg_roles WHERE rolname = 'schema_registry_user') THEN
            EXECUTE 'REASSIGN OWNED BY schema_registry_user TO postgres;';
            EXECUTE 'DROP OWNED BY schema_registry_user;';
        END IF;
    END
$$;


DROP USER IF EXISTS schema_registry_user;