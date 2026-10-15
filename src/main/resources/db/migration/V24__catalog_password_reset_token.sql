-- V24__catalog_password_reset_token.sql
--
-- Catalogo da tabela password_reset_token (V23). fn_catalogo_divergencia() (V18) falha se uma
-- tabela/coluna nova nao tiver linha correspondente aqui -- ver DataCatalogIntegrationTest.
INSERT INTO catalogo_tabela (tabela, dominio, descricao, regra_negocio, nivel_acesso, origem) VALUES
('password_reset_token', 'Identidade',
 'Token opaco de recuperacao de senha emitido por POST /auth/password-reset/request e consumido '
 || 'por POST /auth/password-reset/confirm (ver RequestPasswordResetImpl/ConfirmPasswordResetImpl).',
 'Guarda apenas o SHA-256 do valor bruto, igual ao refresh_token. used_at marca consumo e torna o '
 || 'token invalido para sempre; nao ha campo "revoked" porque, diferente de sessao, este token nao '
 || 'e revogado em massa, so expira ou e usado uma vez.',
 'INTERNO', 'APLICACAO');

INSERT INTO catalogo_coluna (tabela, coluna, descricao, regra_negocio, nivel_acesso, contem_pii) VALUES
('password_reset_token','id','Identificador do pedido de recuperacao.',NULL,'INTERNO',FALSE),
('password_reset_token','user_id','Usuario que pediu a recuperacao.',NULL,'INTERNO',FALSE),
('password_reset_token','token_hash','SHA-256 do token bruto enviado por e-mail.','O valor bruto so existe no link enviado ao usuario; nunca persistido.','SECRETO',FALSE),
('password_reset_token','expires_at','Expiracao do token (janela curta, minutos).',NULL,'INTERNO',FALSE),
('password_reset_token','used_at','Momento em que o token foi consumido; NULL enquanto valido.','Um segundo uso do mesmo token bruto falha porque o hash continua o mesmo, mas used_at ja esta preenchido.','INTERNO',FALSE),
('password_reset_token','created_at','Emissao do token.',NULL,'INTERNO',FALSE);
