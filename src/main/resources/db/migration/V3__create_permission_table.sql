CREATE TABLE permission (
    permission_id  BIGSERIAL,
     name VARCHAR(100) NOT NULL,
     description  VARCHAR(255),

     CONSTRAINT pk_permissions PRIMARY KEY (permission_id),
     CONSTRAINT uq_permissions_name UNIQUE (name)
);