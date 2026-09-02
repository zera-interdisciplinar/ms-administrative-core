-- V5__add_alert_idempotency.sql

-- garante no máximo um alerta OPEN por par (rule_id, event_id); alertas sem
-- correlação de regra (rule_id/event_id nulos) não entram na deduplicação
CREATE UNIQUE INDEX ux_alert_open_rule_event
    ON alert (rule_id, event_id)
    WHERE status = 'OPEN' AND rule_id IS NOT NULL AND event_id IS NOT NULL;
