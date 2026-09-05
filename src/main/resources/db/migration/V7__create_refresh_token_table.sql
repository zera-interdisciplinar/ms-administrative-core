-- V7__create_refresh_token_table.sql
-- Refresh tokens opacos e persistidos: guardamos apenas o hash SHA-256 do valor bruto.

CREATE TABLE refresh_token (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    token_hash CHAR(64) NOT NULL,
    expires_at TIMESTAMP(0) WITHOUT TIME ZONE NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP(0) WITHOUT TIME ZONE NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT refresh_token_token_hash_unique UNIQUE (token_hash),
    CONSTRAINT refresh_token_user_id_foreign FOREIGN KEY (user_id) REFERENCES user_account (id)
);

CREATE INDEX idx_refresh_token_user_id ON refresh_token (user_id);
CREATE INDEX idx_refresh_token_expires_at ON refresh_token (expires_at);
