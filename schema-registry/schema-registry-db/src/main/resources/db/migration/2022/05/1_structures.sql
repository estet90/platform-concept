CREATE TABLE schema_registry.structures
(
    id         BIGSERIAL    NOT NULL,
    name       VARCHAR(200) NOT NULL,
    created_at timestamptz  NOT NULL DEFAULT current_timestamp,
    updated_at timestamptz  NOT NULL DEFAULT current_timestamp,

    CONSTRAINT structures_pk PRIMARY KEY (id),

    CONSTRAINT structures_name_unq UNIQUE (name)
);