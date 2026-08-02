-- V3__refactor_alert_table.sql

-- remove FKs antes de dropar as tabelas
ALTER TABLE alert DROP CONSTRAINT alert_priority_id_foreign;
ALTER TABLE alert DROP CONSTRAINT alert_kind_id_foreign;

-- remove colunas antigas
ALTER TABLE alert DROP COLUMN priority_id;
ALTER TABLE alert DROP COLUMN kind_id;
ALTER TABLE alert DROP COLUMN reference;

-- adiciona novas colunas
ALTER TABLE alert ADD COLUMN event_id UUID NULL;
ALTER TABLE alert ADD COLUMN rule_id UUID NULL;
ALTER TABLE alert ADD COLUMN user_id UUID NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE alert ADD COLUMN description TEXT NOT NULL DEFAULT '';
ALTER TABLE alert ADD COLUMN notes TEXT NULL;
ALTER TABLE alert ADD COLUMN severity VARCHAR(10) NOT NULL DEFAULT 'LOW';
ALTER TABLE alert ADD COLUMN kind VARCHAR(50) NOT NULL DEFAULT '';
ALTER TABLE alert ADD COLUMN occurred_at TIMESTAMP(0) WITHOUT TIME ZONE NOT NULL DEFAULT NOW();

-- remove os defaults temporários usados só para popular colunas NOT NULL em dados existentes
ALTER TABLE alert ALTER COLUMN user_id DROP DEFAULT;
ALTER TABLE alert ALTER COLUMN description DROP DEFAULT;
ALTER TABLE alert ALTER COLUMN severity DROP DEFAULT;
ALTER TABLE alert ALTER COLUMN kind DROP DEFAULT;
ALTER TABLE alert ALTER COLUMN occurred_at DROP DEFAULT;

-- constraints
ALTER TABLE alert ADD CONSTRAINT alert_severity_check CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH'));
ALTER TABLE alert ADD CONSTRAINT alert_user_id_foreign FOREIGN KEY (user_id) REFERENCES user_account (id);

-- índices
CREATE INDEX idx_alert_user_id ON alert (user_id);
CREATE INDEX idx_alert_unit_id ON alert (unit_id);
CREATE INDEX idx_alert_status ON alert (status);
CREATE INDEX idx_alert_created_at ON alert (created_at);

-- drop das tabelas auxiliares
DROP TABLE alert_priority;
DROP TABLE alert_kind;