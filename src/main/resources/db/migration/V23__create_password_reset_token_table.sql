-- V23__create_password_reset_token_table.sql
-- Token opaco de recuperacao de senha: mesmo molde do refresh_token (guardamos so o hash SHA-256).
-- used_at (em vez de um booleano) registra quando o token foi consumido, util para investigar
-- tentativa de reuso.

CREATE TABLE password_reset_token (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    token_hash CHAR(64) NOT NULL,
    expires_at TIMESTAMP(0) WITHOUT TIME ZONE NOT NULL,
    used_at TIMESTAMP(0) WITHOUT TIME ZONE,
    created_at TIMESTAMP(0) WITHOUT TIME ZONE NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT password_reset_token_token_hash_unique UNIQUE (token_hash),
    CONSTRAINT password_reset_token_user_id_foreign FOREIGN KEY (user_id) REFERENCES user_account (id)
);

CREATE INDEX idx_password_reset_token_user_id ON password_reset_token (user_id);
CREATE INDEX idx_password_reset_token_expires_at ON password_reset_token (expires_at);
