CREATE TABLE role (
    role_id  BIGSERIAL,
    name VARCHAR(50)  NOT NULL,
    description VARCHAR(255),

    CONSTRAINT pk_roles PRIMARY KEY (role_id),
    CONSTRAINT uq_roles_name UNIQUE (name)
);