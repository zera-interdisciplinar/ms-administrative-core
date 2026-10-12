-- V21__create_device_token.sql
--
-- Tabela de device tokens FCM, usada para push notification dos alertas (NotifyUserImpl).
--
-- POR QUE UNIQUE em token, e nao em (user_id, token): o mesmo token FCM identifica um device, nao
-- um usuario. Se a pessoa desinstala o app, troca de conta ou reinstala no mesmo aparelho, o
-- Firebase pode reemitir o MESMO token para o novo login. Com UNIQUE(token), o proximo registro
-- so move o token pro novo user_id (UPDATE) em vez de duplicar a linha e mandar push pro usuario
-- errado tambem.
CREATE TABLE device_token (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    token VARCHAR(4096) NOT NULL,
    platform VARCHAR(20) NOT NULL,
    created_at TIMESTAMP(0) WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP(0) WITHOUT TIME ZONE NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT device_token_token_unique UNIQUE (token),
    CONSTRAINT device_token_user_id_foreign FOREIGN KEY (user_id) REFERENCES user_account (id) ON DELETE CASCADE
);

CREATE INDEX idx_device_token_user_id ON device_token (user_id);
