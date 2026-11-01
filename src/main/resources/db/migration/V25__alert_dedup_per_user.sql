-- V25__alert_dedup_per_user.sql

-- o mesmo evento passa a gerar um alerta por gestor da unidade (ms-inventory envia para todos).
-- Com o indice em (rule_id, event_id) so o primeiro gestor ganhava o registro e os demais caiam
-- em "duplicata", nunca recebendo o alerta.
DROP INDEX ux_alert_open_rule_event;

CREATE UNIQUE INDEX ux_alert_open_rule_event_user
    ON alert (rule_id, event_id, user_id)
    WHERE status = 'OPEN' AND rule_id IS NOT NULL AND event_id IS NOT NULL;

UPDATE catalogo_tabela
   SET regra_negocio = 'Deduplicacao por indice parcial unico (rule_id, event_id, user_id) enquanto status=OPEN: reenvio do mesmo evento para o mesmo usuario nao cria alerta novo, mas o mesmo evento pode gerar um alerta por destinatario (todos os gestores da unidade). `severity` e restrito a LOW/MEDIUM/HIGH por CHECK. Mudar os valores de `kind` quebra o ms-inventory em silencio.'
 WHERE tabela = 'alert';

UPDATE catalogo_coluna
   SET regra_negocio = 'Com event_id e user_id, forma a chave de deduplicacao enquanto OPEN.'
 WHERE tabela = 'alert' AND coluna IN ('rule_id', 'event_id');
