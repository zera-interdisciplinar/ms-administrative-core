-- V17__create_data_catalog.sql
--
-- Catalogo de dados: metadados TECNICOS E DE NEGOCIO sobre o proprio schema.
--
-- POR QUE UMA TABELA E NAO UM DOCUMENTO: documento de schema apodrece em silencio. Uma tabela
-- versionada junto com as migracoes muda no mesmo commit que muda o schema, e -- principalmente --
-- pode ser CONFRONTADA com o information_schema. A funcao fn_catalogo_divergencia() faz esse
-- confronto e transforma "temos documentacao" em algo que passa ou falha num teste.
--
-- POR QUE `nivel_acesso` VIVE AQUI: e a regra que as roles da V16 aplicam. O catalogo explica o
-- porque da permissao; a role a executa.

CREATE TABLE catalogo_tabela (
    tabela        VARCHAR(63) NOT NULL,
    dominio       VARCHAR(40) NOT NULL,
    descricao     TEXT NOT NULL,
    regra_negocio TEXT NULL,
    nivel_acesso  VARCHAR(20) NOT NULL,
    origem        VARCHAR(30) NOT NULL,
    PRIMARY KEY (tabela),
    CONSTRAINT catalogo_tabela_nivel_check
        CHECK (nivel_acesso IN ('INTERNO', 'RESTRITO', 'CONFIDENCIAL', 'SECRETO')),
    CONSTRAINT catalogo_tabela_origem_check
        CHECK (origem IN ('APLICACAO', 'LEGADO', 'DERIVADO', 'INFRAESTRUTURA'))
);

CREATE TABLE catalogo_coluna (
    tabela        VARCHAR(63) NOT NULL,
    coluna        VARCHAR(63) NOT NULL,
    descricao     TEXT NOT NULL,
    regra_negocio TEXT NULL,
    nivel_acesso  VARCHAR(20) NOT NULL,
    contem_pii    BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (tabela, coluna),
    CONSTRAINT catalogo_coluna_tabela_foreign FOREIGN KEY (tabela)
        REFERENCES catalogo_tabela (tabela) ON DELETE CASCADE,
    CONSTRAINT catalogo_coluna_nivel_check
        CHECK (nivel_acesso IN ('INTERNO', 'RESTRITO', 'CONFIDENCIAL', 'SECRETO'))
);

COMMENT ON TABLE catalogo_tabela IS 'Catalogo de dados: uma linha por tabela do dominio.';
COMMENT ON TABLE catalogo_coluna IS 'Catalogo de dados: uma linha por coluna, com regra de negocio e nivel de acesso.';
COMMENT ON COLUMN catalogo_tabela.nivel_acesso IS
    'INTERNO: qualquer pessoa da equipe. RESTRITO: equipe do dominio. CONFIDENCIAL: contem dado pessoal. SECRETO: credencial/segredo, nunca exportar.';

-- =============================================================================================
-- TABELAS
-- =============================================================================================
INSERT INTO catalogo_tabela (tabela, dominio, descricao, regra_negocio, nivel_acesso, origem) VALUES
('organization', 'Cadastro',
 'Empresa cliente do Zera. Raiz da hierarquia multi-tenant: toda unidade, usuario e alerta pertence, indiretamente, a uma organizacao.',
 'CNPJ e unico no sistema. `status` governa o acesso de toda a arvore abaixo: organizacao SUSPENDED nao deve permitir operacao de suas unidades. `plan` era FK para a tabela plan ate a V2, quando virou coluna -- nao ha mais catalogo de planos no banco.',
 'CONFIDENCIAL', 'APLICACAO'),

('unit', 'Cadastro',
 'Unidade operacional (filial, planta, loja) de uma organizacao. E a chave de particionamento logico do produto: alertas, usuarios e relatorios sao sempre por unidade.',
 'Pertence a exatamente uma organizacao. Nao ha exclusao em cascata: apagar unidade com usuario vinculado viola a FK, por desenho.',
 'INTERNO', 'APLICACAO'),

('user_account', 'Identidade',
 'Conta de usuario. Chama-se user_account e nao user porque `user` e palavra reservada no Postgres. Usa heranca single-table do JPA com `role` como discriminador.',
 'MANAGER e EMPLOYEE compartilham a tabela. `manager_id` so faz sentido para EMPLOYEE e e auto-referencia -- o schema nao impede um ciclo, entao toda consulta recursiva precisa de protecao de caminho. `status` controla login: apenas ACTIVE autentica.',
 'CONFIDENCIAL', 'APLICACAO'),

('address', 'Cadastro',
 'Endereco de uma unidade ou de uma recicladora. Uma linha aponta para um OU outro, nunca para os dois.',
 'Nao ha constraint garantindo exclusividade entre unit_id e recycling_business_id nem unicidade por unidade -- por isso bi.dim_unidade usa DISTINCT ON para nao duplicar a dimensao.',
 'CONFIDENCIAL', 'APLICACAO'),

('telephone', 'Cadastro',
 'Telefone de contato, polimorfico entre usuario, organizacao, unidade e recicladora.',
 'As quatro FKs sao nullable e o schema nao garante que exatamente uma esteja preenchida; a regra e do dominio.',
 'CONFIDENCIAL', 'APLICACAO'),

('recycling_business', 'Recicladoras',
 'Cadastro INTERNO de empresa recicladora, com CNPJ e e-mail. Nao confundir com os pontos retornados pelo Google Places (/api/v1/recycling-places), que nao sao persistidos.',
 'CNPJ unico. Os Termos de Uso do Google proibem persistir nome/endereco/coordenada de lugares do Places por mais de 30 dias -- por isso aquele recurso nao tem tabela e este nao recebe dado do Places.',
 'INTERNO', 'APLICACAO'),

('alert', 'Alertas',
 'Alerta gerado pelo ms-inventory ou pelo AI core e entregue a um usuario. Contrato publico entre servicos.',
 'Deduplicacao por indice parcial unico (rule_id, event_id) enquanto status=OPEN: reenvio do mesmo evento nao cria alerta novo. `severity` e restrito a LOW/MEDIUM/HIGH por CHECK. Mudar os valores de `kind` quebra o ms-inventory em silencio.',
 'RESTRITO', 'APLICACAO'),

('invitation', 'Identidade',
 'Convite de gestor para um novo funcionario entrar numa unidade, resgatavel por codigo de 6 caracteres.',
 'Indice parcial unico garante um unico convite PENDING por codigo -- codigo pode ser reaproveitado depois de usado. O enum do dominio tem apenas PENDING e USED: nao existe estado EXPIRED, entao convite vencido continua PENDING e e barrado por `expires_at` na leitura.',
 'CONFIDENCIAL', 'APLICACAO'),

('refresh_token', 'Identidade',
 'Refresh token opaco e persistido, usado para renovar a sessao sem novo login.',
 'Guarda apenas o SHA-256 do valor bruto: o token original nunca toca o banco. `revoked` invalida na hora; a remocao fisica so acontece apos a janela de retencao (sp_revogar_tokens_expirados). A insercao aqui dispara o trigger de DAU.',
 'SECRETO', 'APLICACAO'),

('user_access_log', 'Monitoramento',
 'Log de acessos (grao: um evento de autenticacao). Fonte da verdade do DAU.',
 'Escrito exclusivamente pelo trigger trg_registrar_acesso em refresh_token -- nunca pela aplicacao. Isso e o que impede um caminho novo de login de esquecer de contar o acesso.',
 'RESTRITO', 'DERIVADO'),

('usuario_ativo_diario', 'Monitoramento',
 'Rollup diario de DAU (usuarios ativos e acessos por dia).',
 'Mantido incrementalmente pelo trigger, por custo O(1) no login. Sob concorrencia do mesmo usuario no mesmo dia pode divergir em +1; sp_consolidar_dau reconcilia a partir do log, que e a fonte da verdade.',
 'INTERNO', 'DERIVADO'),

('audit_log', 'Governanca',
 'Trilha de auditoria de escrita. Tabela PAI de uma heranca: permanece vazia, e cada tabela auditada grava na filha audit_log_<tabela>.',
 'Registra TG_OP, OLD e NEW em JSONB, o usuario de banco (CURRENT_USER) e o usuario de aplicacao (SET LOCAL zera.app_user). Hash de senha, hash de token e codigo de convite sao removidos do payload antes de gravar. As filhas tem CHECK em `tabela` para o planner podar por exclusao de constraint.',
 'CONFIDENCIAL', 'INFRAESTRUTURA'),

('job_execucao', 'Monitoramento',
 'Trilha de execucao das procedures de manutencao: quando rodou, quanto afetou, se deu certo.',
 'Escrita pelas proprias procedures. Serve para responder "ha quanto tempo esse job nao roda?" sem depender de log de aplicacao.',
 'INTERNO', 'INFRAESTRUTURA'),

('catalogo_tabela', 'Governanca',
 'Este catalogo: uma linha por tabela do dominio.',
 'Mantido nas migracoes, no mesmo commit que altera o schema. Confrontado com o information_schema por fn_catalogo_divergencia().',
 'INTERNO', 'INFRAESTRUTURA'),

('catalogo_coluna', 'Governanca',
 'Este catalogo: uma linha por coluna, com regra de negocio e nivel de acesso.',
 'Toda coluna de tabela catalogada precisa de linha aqui, senao fn_catalogo_divergencia() acusa.',
 'INTERNO', 'INFRAESTRUTURA'),

('disposal_report', 'Legado',
 'Relatorio de descarte. Criada na V1 e NUNCA USADA por este servico: nao ha entidade JPA, repositorio nem endpoint que a toque.',
 'O dominio de descarte vive no ms-inventory. Esta tabela e residuo do desenho inicial, quando os dois servicos eram um so. Documentada aqui justamente para que a proxima pessoa nao gaste tempo procurando o codigo que a usa -- nao existe. Candidata a remocao mediante confirmacao de que o legado nao le dela.',
 'RESTRITO', 'LEGADO');

-- =============================================================================================
-- COLUNAS
-- =============================================================================================
INSERT INTO catalogo_coluna (tabela, coluna, descricao, regra_negocio, nivel_acesso, contem_pii) VALUES
-- organization
('organization','id','Identificador da organizacao.',NULL,'INTERNO',FALSE),
('organization','name','Razao social ou nome fantasia.',NULL,'INTERNO',FALSE),
('organization','cnpj','CNPJ com 14 digitos, sem mascara.','CHECK fn_validar_cnpj(cnpj) em vigor desde a V18, com NOT VALID: vale para todo INSERT/UPDATE novo, mas nao reavalia linhas antigas. Dado legado invalido pode existir ate ser auditado e validado.','CONFIDENCIAL',TRUE),
('organization','status','ACTIVE, INACTIVE ou SUSPENDED.','Transicoes validas definidas no enum Status do dominio.','INTERNO',FALSE),
('organization','email','E-mail de contato da organizacao.',NULL,'CONFIDENCIAL',TRUE),
('organization','plan','Plano contratado.','Virou coluna na V2, quando a tabela plan foi removida. Nao ha catalogo de planos no banco.','INTERNO',FALSE),
('organization','created_at','Data/hora de criacao.',NULL,'INTERNO',FALSE),
('organization','updated_at','Data/hora da ultima alteracao.',NULL,'INTERNO',FALSE),
-- unit
('unit','id','Identificador da unidade.',NULL,'INTERNO',FALSE),
('unit','name','Nome da unidade.',NULL,'INTERNO',FALSE),
('unit','organization_id','Organizacao dona da unidade.','FK obrigatoria: nao existe unidade sem organizacao.','INTERNO',FALSE),
('unit','created_at','Data/hora de criacao.',NULL,'INTERNO',FALSE),
('unit','updated_at','Data/hora da ultima alteracao.',NULL,'INTERNO',FALSE),
-- user_account
('user_account','id','Identificador do usuario. Vai no claim `sub` do JWT.','E por ele que a auditoria liga uma alteracao a uma pessoa (audit_log.usuario_app).','INTERNO',FALSE),
('user_account','name','Nome do usuario.','Vai no claim `name` do JWT -- parte do contrato com o ms-inventory.','CONFIDENCIAL',TRUE),
('user_account','role','MANAGER ou EMPLOYEE.','Discriminador da heranca single-table do JPA. Vira ROLE_* na autorizacao HTTP.','INTERNO',FALSE),
('user_account','password','Hash bcrypt (60 caracteres).','NUNCA sai do servico, nunca entra em log, nunca entra em auditoria (mascarado por fn_mascarar_sensiveis). Senha em claro nao existe em lugar nenhum.','SECRETO',TRUE),
('user_account','email','E-mail de login.','Identificador de autenticacao. Unicidade e garantida pela aplicacao, nao pelo schema.','CONFIDENCIAL',TRUE),
('user_account','status','ACTIVE, INACTIVE ou SUSPENDED.','Apenas ACTIVE autentica.','INTERNO',FALSE),
('user_account','unit_id','Unidade a que o usuario pertence.','Usado por GET /users?role=MANAGER&unitId=... para achar o destinatario de um alerta -- contrato com o ms-inventory.','INTERNO',FALSE),
('user_account','manager_id','Gestor responsavel.','Nulo para MANAGER raiz. Auto-referencia sem protecao de ciclo no schema: consulta recursiva precisa carregar o caminho.','INTERNO',FALSE),
('user_account','image_url','URL da foto de perfil.','Nullable porque usuarios anteriores a V9 nao tem.','CONFIDENCIAL',TRUE),
('user_account','created_at','Data/hora de criacao.',NULL,'INTERNO',FALSE),
('user_account','updated_at','Data/hora da ultima alteracao.',NULL,'INTERNO',FALSE),
-- address
('address','id','Identificador do endereco.',NULL,'INTERNO',FALSE),
('address','city','Cidade.',NULL,'CONFIDENCIAL',TRUE),
('address','state','Unidade federativa.',NULL,'INTERNO',FALSE),
('address','neighborhood','Bairro.',NULL,'CONFIDENCIAL',TRUE),
('address','cep','CEP com 8 digitos, sem mascara.',NULL,'CONFIDENCIAL',TRUE),
('address','number','Numero do logradouro.',NULL,'CONFIDENCIAL',TRUE),
('address','complement','Complemento.',NULL,'CONFIDENCIAL',TRUE),
('address','unit_id','Unidade dona do endereco.','Exclusivo com recycling_business_id; o schema nao garante.','INTERNO',FALSE),
('address','recycling_business_id','Recicladora dona do endereco.','Exclusivo com unit_id; o schema nao garante.','INTERNO',FALSE),
('address','created_at','Data/hora de criacao.','Usada por bi.dim_unidade para escolher o endereco mais recente.','INTERNO',FALSE),
('address','updated_at','Data/hora da ultima alteracao.',NULL,'INTERNO',FALSE),
-- telephone
('telephone','id','Identificador do telefone.',NULL,'INTERNO',FALSE),
('telephone','number','Numero de telefone.',NULL,'CONFIDENCIAL',TRUE),
('telephone','user_id','Usuario dono do telefone.','Uma das quatro FKs polimorficas; exatamente uma deve estar preenchida.','INTERNO',FALSE),
('telephone','organization_id','Organizacao dona do telefone.',NULL,'INTERNO',FALSE),
('telephone','unit_id','Unidade dona do telefone.',NULL,'INTERNO',FALSE),
('telephone','recycling_business_id','Recicladora dona do telefone.',NULL,'INTERNO',FALSE),
('telephone','created_at','Data/hora de criacao.',NULL,'INTERNO',FALSE),
('telephone','updated_at','Data/hora da ultima alteracao.',NULL,'INTERNO',FALSE),
-- recycling_business
('recycling_business','id','Identificador da recicladora.',NULL,'INTERNO',FALSE),
('recycling_business','name','Nome da recicladora.',NULL,'INTERNO',FALSE),
('recycling_business','cnpj','CNPJ com 14 digitos, sem mascara.','Unico.','CONFIDENCIAL',TRUE),
('recycling_business','contact_email','E-mail de contato.',NULL,'CONFIDENCIAL',TRUE),
('recycling_business','created_at','Data/hora de criacao.',NULL,'INTERNO',FALSE),
('recycling_business','updated_at','Data/hora da ultima alteracao.',NULL,'INTERNO',FALSE),
-- alert
('alert','id','Identificador do alerta.',NULL,'INTERNO',FALSE),
('alert','status','OPEN ou CLOSED.','Observado pelo ms-inventory. Fechamento automatico so acontece por acionamento explicito de sp_fechar_alertas_obsoletos.','RESTRITO',FALSE),
('alert','unit_id','Unidade a que o alerta se refere.',NULL,'INTERNO',FALSE),
('alert','user_id','Usuario destinatario.','O alerta ja nasce enderecado; a listagem filtra pelo `sub` do token, nunca por parametro do cliente.','INTERNO',FALSE),
('alert','event_id','Evento de origem no ms-inventory.','Com rule_id, forma a chave de deduplicacao enquanto OPEN.','RESTRITO',FALSE),
('alert','rule_id','Regra que disparou o alerta.','Com event_id, forma a chave de deduplicacao enquanto OPEN.','RESTRITO',FALSE),
('alert','description','Texto do alerta mostrado ao usuario.',NULL,'RESTRITO',FALSE),
('alert','notes','Observacoes.','Recebe o carimbo de fechamento automatico de sp_fechar_alertas_obsoletos.','RESTRITO',FALSE),
('alert','severity','LOW, MEDIUM ou HIGH.','CHECK no schema. Pesos 1/3/9 no BI e em fn_indice_saude_unidade; mudar a escala aqui sem mudar la faz painel e alerta discordarem.','RESTRITO',FALSE),
('alert','kind','Tipo do alerta.','Espelha o AlertKind do ms-inventory -- contrato publico entre servicos.','RESTRITO',FALSE),
('alert','occurred_at','Quando o evento ocorreu no mundo real.','Diferente de created_at: e por esta coluna que se mede a idade do alerta, porque reprocessamento cria linha nova para evento antigo.','RESTRITO',FALSE),
('alert','created_at','Quando a linha foi gravada.',NULL,'INTERNO',FALSE),
('alert','updated_at','Ultima alteracao.','Com occurred_at, da o tempo ate o fechamento no BI.','INTERNO',FALSE),
-- invitation
('invitation','id','Identificador do convite.',NULL,'INTERNO',FALSE),
('invitation','code','Codigo de 6 caracteres para resgate.','Unico apenas entre convites PENDING (indice parcial). Hoje a tabela invitation NAO tem trigger de auditoria, entao este valor nao aparece em audit_log. Se ela passar a ser auditada, este campo precisa entrar em fn_mascarar_sensiveis antes.','SECRETO',FALSE),
('invitation','manager_id','Gestor que convidou.',NULL,'INTERNO',FALSE),
('invitation','unit_id','Unidade de destino do convidado.',NULL,'INTERNO',FALSE),
('invitation','status','PENDING ou USED.','Nao existe EXPIRED no enum do dominio: convite vencido continua PENDING e e barrado por expires_at.','INTERNO',FALSE),
('invitation','expires_at','Prazo de validade.','Unica barreira contra resgate tardio, ja que nao ha estado EXPIRED.','INTERNO',FALSE),
('invitation','used_by_user_id','Usuario criado a partir do convite.',NULL,'INTERNO',FALSE),
('invitation','invitee_name','Nome de quem esta sendo convidado.','Nullable porque convites anteriores a V8 nao tem.','CONFIDENCIAL',TRUE),
('invitation','created_at','Data/hora de criacao.',NULL,'INTERNO',FALSE),
('invitation','updated_at','Data/hora da ultima alteracao.',NULL,'INTERNO',FALSE),
-- refresh_token
('refresh_token','id','Identificador da sessao persistida.',NULL,'INTERNO',FALSE),
('refresh_token','user_id','Dono da sessao.','A insercao desta linha dispara o registro de DAU.','INTERNO',FALSE),
('refresh_token','token_hash','SHA-256 do refresh token bruto.','O valor bruto so existe no cliente. Hoje refresh_token NAO tem trigger de auditoria, entao este hash nao aparece em audit_log. Se ela passar a ser auditada, token_hash precisa entrar em fn_mascarar_sensiveis antes.','SECRETO',FALSE),
('refresh_token','expires_at','Expiracao da sessao.',NULL,'INTERNO',FALSE),
('refresh_token','revoked','Sessao revogada.','Revogacao invalida na hora; a linha so e removida apos a janela de retencao, para nao apagar rastro de investigacao.','INTERNO',FALSE),
('refresh_token','created_at','Emissao da sessao.','E o carimbo de tempo do acesso no DAU.','INTERNO',FALSE),
-- user_access_log
('user_access_log','id','Identificador do evento de acesso.',NULL,'INTERNO',FALSE),
('user_access_log','user_id','Usuario que acessou.',NULL,'RESTRITO',TRUE),
('user_access_log','origem','LOGIN, REFRESH ou BACKFILL.','Vem do parametro de sessao zera.access_origin; sem ele assume LOGIN.','INTERNO',FALSE),
('user_access_log','ocorrido_em','Momento do acesso.',NULL,'RESTRITO',FALSE),
-- usuario_ativo_diario
('usuario_ativo_diario','dia','Dia consolidado.',NULL,'INTERNO',FALSE),
('usuario_ativo_diario','usuarios_ativos','Usuarios distintos que acessaram no dia (DAU).','Contador incremental; reconciliavel por sp_consolidar_dau.','INTERNO',FALSE),
('usuario_ativo_diario','acessos','Total de eventos de acesso no dia.',NULL,'INTERNO',FALSE),
('usuario_ativo_diario','atualizado_em','Ultima atualizacao do rollup.',NULL,'INTERNO',FALSE),
-- audit_log
('audit_log','id','Identificador do registro de auditoria.','BIGSERIAL do pai: a sequencia e compartilhada pelas filhas, entao o id e global.','INTERNO',FALSE),
('audit_log','tabela','Tabela auditada (TG_TABLE_NAME).','CHECK nas filhas permite ao planner podar a heranca quando a consulta filtra por esta coluna.','INTERNO',FALSE),
('audit_log','operacao','INSERT, UPDATE, DELETE ou TRUNCATE (TG_OP). TRUNCATE foi adicionado na V10 por ser o unico evento destrutivo que o FOR EACH ROW nao capturaria.',NULL,'INTERNO',FALSE),
('audit_log','registro_id','Chave primaria da linha afetada.',NULL,'INTERNO',FALSE),
('audit_log','dados_antigos','Linha antes da mudanca (OLD), em JSONB.','Nulo em INSERT. Mascarado.','CONFIDENCIAL',TRUE),
('audit_log','dados_novos','Linha depois da mudanca (NEW), em JSONB.','Nulo em DELETE. Mascarado.','CONFIDENCIAL',TRUE),
('audit_log','usuario_banco','Usuario de banco (CURRENT_USER).','Responde "qual conexao", nao "qual pessoa": a aplicacao usa um unico usuario de pool.','INTERNO',FALSE),
('audit_log','usuario_app','Usuario da aplicacao que originou a mudanca.','Vem de SET LOCAL zera.app_user. Nulo quando a mudanca veio de job, migracao ou psql.','RESTRITO',TRUE),
('audit_log','ocorrido_em','Momento da mudanca.',NULL,'INTERNO',FALSE),
-- job_execucao
('job_execucao','id','Identificador da execucao.',NULL,'INTERNO',FALSE),
('job_execucao','job','Nome da procedure executada.',NULL,'INTERNO',FALSE),
('job_execucao','iniciado_em','Inicio da execucao.',NULL,'INTERNO',FALSE),
('job_execucao','concluido_em','Fim da execucao.',NULL,'INTERNO',FALSE),
('job_execucao','afetados','Numero de linhas afetadas.',NULL,'INTERNO',FALSE),
('job_execucao','detalhe','Parametros e resultado da execucao.',NULL,'INTERNO',FALSE),
('job_execucao','sucesso','Se a execucao concluiu sem erro.',NULL,'INTERNO',FALSE),
-- catalogo
('catalogo_tabela','tabela','Nome da tabela catalogada.',NULL,'INTERNO',FALSE),
('catalogo_tabela','dominio','Area de negocio a que a tabela pertence.',NULL,'INTERNO',FALSE),
('catalogo_tabela','descricao','O que a tabela representa.',NULL,'INTERNO',FALSE),
('catalogo_tabela','regra_negocio','Regras e armadilhas que o schema nao expressa.',NULL,'INTERNO',FALSE),
('catalogo_tabela','nivel_acesso','Classificacao de acesso da tabela.','Aplicada pelas roles da V16.','INTERNO',FALSE),
('catalogo_tabela','origem','APLICACAO, LEGADO, DERIVADO ou INFRAESTRUTURA.',NULL,'INTERNO',FALSE),
('catalogo_coluna','tabela','Tabela da coluna.',NULL,'INTERNO',FALSE),
('catalogo_coluna','coluna','Nome da coluna.',NULL,'INTERNO',FALSE),
('catalogo_coluna','descricao','O que a coluna guarda.',NULL,'INTERNO',FALSE),
('catalogo_coluna','regra_negocio','Regra de negocio associada a coluna.',NULL,'INTERNO',FALSE),
('catalogo_coluna','nivel_acesso','Classificacao de acesso da coluna.',NULL,'INTERNO',FALSE),
('catalogo_coluna','contem_pii','Se a coluna guarda dado pessoal.','Usado para decidir o que pode sair do banco em export ou dump de homologacao.','INTERNO',FALSE),
-- disposal_report (legado)
('disposal_report','id','Identificador do relatorio.',NULL,'INTERNO',FALSE),
('disposal_report','status','Situacao do relatorio.',NULL,'RESTRITO',FALSE),
('disposal_report','url','Link do arquivo do relatorio.',NULL,'RESTRITO',FALSE),
('disposal_report','user_id','Usuario que gerou o relatorio.',NULL,'RESTRITO',TRUE),
('disposal_report','unit_id','Unidade do relatorio.',NULL,'INTERNO',FALSE),
('disposal_report','confirmed_by','Usuario que confirmou.',NULL,'RESTRITO',TRUE),
('disposal_report','confirmed_at','Momento da confirmacao.',NULL,'INTERNO',FALSE),
('disposal_report','batch_id','Lote de descarte.',NULL,'INTERNO',FALSE),
('disposal_report','created_at','Data/hora de criacao.',NULL,'INTERNO',FALSE),
('disposal_report','updated_at','Data/hora da ultima alteracao.',NULL,'INTERNO',FALSE);

-- =============================================================================================
-- CONFRONTO COM O SCHEMA REAL
-- =============================================================================================
--
-- E isto que impede o catalogo de virar ficcao. Retorna uma linha para cada divergencia entre o
-- catalogo e o information_schema, nos dois sentidos: coluna que existe e nao foi documentada, e
-- coluna documentada que nao existe mais.
--
-- As filhas da heranca sao IGNORADAS de proposito: elas replicam exatamente as colunas do pai, e
-- documenta-las seria copiar a mesma descricao quatro vezes -- que e justamente o tipo de
-- duplicacao que faz documentacao divergir.
CREATE OR REPLACE FUNCTION fn_catalogo_divergencia()
RETURNS TABLE (tipo TEXT, tabela TEXT, coluna TEXT, detalhe TEXT)
LANGUAGE sql
STABLE
AS $$
    WITH filhas AS (
        -- Tabelas que herdam de outra: documentadas atraves do pai.
        SELECT c.relname AS tabela
        FROM pg_inherits i
        JOIN pg_class c ON c.oid = i.inhrelid
    ),
    reais AS (
        SELECT c.table_name::TEXT AS tabela, c.column_name::TEXT AS coluna
        FROM information_schema.columns c
        JOIN information_schema.tables t
          ON t.table_schema = c.table_schema AND t.table_name = c.table_name
        WHERE c.table_schema = 'public'
          AND t.table_type = 'BASE TABLE'
          AND c.table_name <> 'flyway_schema_history'
          AND c.table_name NOT IN (SELECT tabela FROM filhas)
    ),
    catalogadas AS (
        SELECT cc.tabela::TEXT, cc.coluna::TEXT FROM catalogo_coluna cc
    )
    SELECT 'COLUNA_NAO_CATALOGADA', r.tabela, r.coluna,
           'Existe no banco e nao esta no catalogo_coluna'
    FROM reais r
    LEFT JOIN catalogadas c ON c.tabela = r.tabela AND c.coluna = r.coluna
    WHERE c.tabela IS NULL

    UNION ALL

    SELECT 'COLUNA_INEXISTENTE', c.tabela, c.coluna,
           'Esta no catalogo_coluna e nao existe mais no banco'
    FROM catalogadas c
    LEFT JOIN reais r ON r.tabela = c.tabela AND r.coluna = c.coluna
    WHERE r.tabela IS NULL

    UNION ALL

    SELECT 'TABELA_NAO_CATALOGADA', r.tabela, NULL,
           'Tabela existe no banco e nao esta no catalogo_tabela'
    FROM (SELECT DISTINCT tabela FROM reais) r
    LEFT JOIN catalogo_tabela ct ON ct.tabela = r.tabela
    WHERE ct.tabela IS NULL;
$$;

COMMENT ON FUNCTION fn_catalogo_divergencia() IS
    'Confronta o catalogo de dados com o information_schema. Resultado vazio = catalogo em dia.';

GRANT SELECT ON catalogo_tabela, catalogo_coluna TO zera_auditor;
