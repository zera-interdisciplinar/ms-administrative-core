-- V3__update_alert_tables.sql

-- 1. Atualizar a tabela alert_priority
ALTER TABLE alert_priority
    ADD COLUMN severity VARCHAR(10) NOT NULL;

-- 2. Atualizar a tabela alert
ALTER TABLE alert
    DROP COLUMN IF EXISTS reference,
    ADD COLUMN event_id UUID NULL,
    ADD COLUMN rule_id UUID NULL,
    ADD COLUMN user_id UUID NOT NULL,
    ADD COLUMN source VARCHAR(100) NOT NULL,
    ADD COLUMN reason TEXT NOT NULL,
    ADD COLUMN notes TEXT NULL,
    ADD COLUMN occurred_at TIMESTAMP(0) WITHOUT TIME ZONE NOT NULL,
    ALTER COLUMN status SET DEFAULT 'OPEN',
    ADD CONSTRAINT alert_user_id_foreign FOREIGN KEY (user_id) REFERENCES user_account (id);

-- 3. Índices para queries frequentes
CREATE INDEX idx_alert_user_id ON alert (user_id);
CREATE INDEX idx_alert_unit_id ON alert (unit_id);
CREATE INDEX idx_alert_status ON alert (status);
CREATE INDEX idx_alert_created_at ON alert (created_at);

-- 4. Seed de prioridades
INSERT INTO alert_priority (id, name, description, level, severity) VALUES
    ('a1b2c3d4-0001-0001-0001-000000000001', 'LOW',    'Low priority alert',    1, 'LOW'),
    ('a1b2c3d4-0002-0002-0002-000000000002', 'MEDIUM', 'Medium priority alert', 2, 'MEDIUM'),
    ('a1b2c3d4-0003-0003-0003-000000000003', 'HIGH',   'High priority alert',   3, 'HIGH');


-- TABELAS COMPLETAS PARA MODELAGEM
-- CREATE TABLE alert_kind (
--     id UUID NOT NULL,
--     name VARCHAR(50) NOT NULL,
--     description TEXT NOT NULL,
--     PRIMARY KEY (id)
-- );

-- CREATE TABLE alert_priority (
--     id UUID NOT NULL,
--     name VARCHAR(10) NOT NULL,
--     description TEXT NOT NULL,
--     level INTEGER NOT NULL,
--     severity VARCHAR(10) NOT NULL,
--     PRIMARY KEY (id)
-- );

-- CREATE TABLE alert (
--     id UUID NOT NULL,
--     event_id UUID NULL,
--     rule_id UUID NULL,
--     user_id UUID NOT NULL,
--     unit_id UUID NOT NULL,
--     status VARCHAR(15) NOT NULL DEFAULT 'OPEN',
--     priority_id UUID NOT NULL,
--     kind_id UUID NOT NULL,
--     source VARCHAR(100) NOT NULL,
--     reason TEXT NOT NULL,
--     notes TEXT NULL,
--     occurred_at TIMESTAMP(0) WITHOUT TIME ZONE NOT NULL,
--     created_at TIMESTAMP(0) WITHOUT TIME ZONE NOT NULL,
--     updated_at TIMESTAMP(0) WITHOUT TIME ZONE NOT NULL,
--     PRIMARY KEY (id),
--     CONSTRAINT alert_user_id_foreign FOREIGN KEY (user_id) REFERENCES user_account (id),
--     CONSTRAINT alert_unit_id_foreign FOREIGN KEY (unit_id) REFERENCES unit (id),
--     CONSTRAINT alert_priority_id_foreign FOREIGN KEY (priority_id) REFERENCES alert_priority (id),
--     CONSTRAINT alert_kind_id_foreign FOREIGN KEY (kind_id) REFERENCES alert_kind (id)
-- );