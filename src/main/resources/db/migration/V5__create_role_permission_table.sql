CREATE TABLE role_permission (
     role_id  BIGINT NOT NULL,
     permission_id  BIGINT NOT NULL,

     CONSTRAINT pk_role_permissions PRIMARY KEY (role_id, permission_id),
     CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES role (role_id),
     CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permission (permission_id)
);