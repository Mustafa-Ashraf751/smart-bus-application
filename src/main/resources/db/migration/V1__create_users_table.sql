CREATE TABLE users (
     user_id  BIGSERIAL,
     name VARCHAR(100) NOT NULL,
     phone  VARCHAR(20),
     address  VARCHAR(255),
     email  VARCHAR(150) NOT NULL,
     password_hash  VARCHAR(255) NOT NULL,
     status VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP,
     updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

     CONSTRAINT pk_users PRIMARY KEY (user_id),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);