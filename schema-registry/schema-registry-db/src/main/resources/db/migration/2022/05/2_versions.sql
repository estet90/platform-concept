CREATE TABLE schema_registry.versions
(
    id           BIGSERIAL    NOT NULL,
    name         VARCHAR(200) NOT NULL,
    structure_id BIGINT       NOT NULL,
    link         VARCHAR(300) NOT NULL,
    created_at   timestamptz  NOT NULL DEFAULT current_timestamp,

    CONSTRAINT versions_pk PRIMARY KEY (id),

    CONSTRAINT versions_structure_id_fk FOREIGN KEY (structure_id) REFERENCES schema_registry.structures (id) ON DELETE CASCADE,
    CONSTRAINT versions_structure_id_name_unq UNIQUE (structure_id, name)
);

