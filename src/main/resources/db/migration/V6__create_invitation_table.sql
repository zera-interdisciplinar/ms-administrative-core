-- V6__create_invitation_table.sql

CREATE TABLE invitation (
    id UUID NOT NULL,
    code CHAR(6) NOT NULL,
    manager_id UUID NOT NULL,
    unit_id UUID NOT NULL,
    status VARCHAR(15) NOT NULL,
    expires_at TIMESTAMP(0) WITHOUT TIME ZONE NOT NULL,
    used_by_user_id UUID NULL,
    created_at TIMESTAMP(0) WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP(0) WITHOUT TIME ZONE NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT invitation_manager_id_foreign FOREIGN KEY (manager_id) REFERENCES user_account (id),
    CONSTRAINT invitation_unit_id_foreign FOREIGN KEY (unit_id) REFERENCES unit (id),
    CONSTRAINT invitation_used_by_user_id_foreign FOREIGN KEY (used_by_user_id) REFERENCES user_account (id)
);

-- garante no máximo um convite pendente ativo por código
CREATE UNIQUE INDEX ux_invitation_pending_code
    ON invitation (code)
    WHERE status = 'PENDING';
