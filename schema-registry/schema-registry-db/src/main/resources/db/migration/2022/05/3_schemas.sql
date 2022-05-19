CREATE TABLE schema_registry.schemas
(
    id         BIGSERIAL     NOT NULL,
    path       VARCHAR(1000) NOT NULL,
    version_id BIGINT        NOT NULL,
    link       VARCHAR(300)  NOT NULL,

    CONSTRAINT schemas_pk PRIMARY KEY (id),

    CONSTRAINT schemas_version_id_fk FOREIGN KEY (version_id) REFERENCES schema_registry.versions (id) ON DELETE CASCADE,
    CONSTRAINT schemas_id_path_unq UNIQUE (id, path)
);