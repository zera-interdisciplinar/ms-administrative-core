-- V22__catalog_device_token.sql
--
-- Catalogo da tabela device_token (V21). fn_catalogo_divergencia() (V18) falha se uma tabela/coluna
-- nova nao tiver linha correspondente aqui -- ver DataCatalogIntegrationTest.
INSERT INTO catalogo_tabela (tabela, dominio, descricao, regra_negocio, nivel_acesso, origem) VALUES
('device_token', 'Alertas',
 'Device token FCM de um usuario, usado para entregar o alerta tambem como push (ver NotifyUserImpl).',
 'UNIQUE em token, nao em user_id: o mesmo token FCM identifica um device, nao uma pessoa. Reinstalar o app ou logar com outra conta no mesmo aparelho reatribui o token ao novo usuario em vez de duplicar a linha.',
 'CONFIDENCIAL', 'APLICACAO');

INSERT INTO catalogo_coluna (tabela, coluna, descricao, regra_negocio, nivel_acesso, contem_pii) VALUES
('device_token','id','Identificador do registro de device token.',NULL,'INTERNO',FALSE),
('device_token','user_id','Usuario dono do device no momento.','Muda quando o mesmo token reaparece em outro login (ver regra da tabela).','INTERNO',FALSE),
('device_token','token','Token FCM opaco emitido pelo Firebase para o device.','Identifica o aparelho, nao a pessoa; e o valor usado para disparar o push.','CONFIDENCIAL',TRUE),
('device_token','platform','IOS, ANDROID ou WEB.',NULL,'INTERNO',FALSE),
('device_token','created_at','Data/hora do primeiro registro deste token.',NULL,'INTERNO',FALSE),
('device_token','updated_at','Data/hora da ultima atualizacao (reatribuicao de usuario ou plataforma).',NULL,'INTERNO',FALSE);
