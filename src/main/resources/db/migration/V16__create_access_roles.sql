-- V16__create_access_roles.sql
--
-- Niveis de acesso aplicados pelo banco. A coluna `nivel_acesso` do catalogo (V17) descreve a
-- regra; estas roles sao o que a FAZ VALER. Catalogo sem role e documentacao; role sem catalogo e
-- permissao sem explicacao. Os dois juntos sao governanca.
--
-- POR QUE A ROLE DE BI NAO PRECISA DE ACESSO A `public`: uma view no Postgres e executada com os
-- privilegios de QUEM A CRIOU, nao de quem a consulta. Como as views de `bi` foram criadas pelo
-- dono do schema, a role zera_bi_leitor le `bi.fato_alerta` sem ter (e sem poder ganhar) qualquer
-- acesso a tabela `alert`. E a diferenca pratica entre "a ferramenta de BI ve os numeros" e "a
-- ferramenta de BI ve o hash de senha de todo mundo".
--
-- Roles sao objetos do CLUSTER, nao do banco: por isso a criacao e condicional, senao a migracao
-- quebra em qualquer ambiente onde a role ja exista (um segundo banco no mesmo servidor, um
-- restore, um Postgres compartilhado).
--
-- NOLOGIN de proposito: sao roles de PAPEL, nao contas. O DBA cria a conta real e concede o papel
--   CREATE ROLE bi_metabase LOGIN PASSWORD '...'; GRANT zera_bi_leitor TO bi_metabase;
-- assim a senha nunca entra numa migracao versionada.

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'zera_bi_leitor') THEN
        CREATE ROLE zera_bi_leitor NOLOGIN;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'zera_auditor') THEN
        CREATE ROLE zera_auditor NOLOGIN;
    END IF;
END
$$;

-- A promessa aqui e deliberadamente ESTREITA. Uma versao anterior deste comentario dizia "nunca
-- alcanca dado pessoal", o que era FALSO: as views de `bi` expoem nome de pessoa, nome de unidade e
-- cidade -- dado pessoal pelo proprio catalogo (V17). Garantia de privacidade que promete demais e
-- pior que garantia nenhuma, porque alguem decide com base nela.
COMMENT ON ROLE zera_bi_leitor IS
    'Papel de leitura da camada analitica (schema bi). NAO alcanca credencial, hash de senha, CNPJ, '
    'e-mail, telefone nem endereco. AINDA VE nome de pessoa, nome de unidade e cidade: a ferramenta '
    'de BI esta dentro do perimetro de LGPD, nao fora dele.';
COMMENT ON ROLE zera_auditor IS
    'Papel de auditoria. Le a trilha audit_log e o catalogo de dados; nao ve dado operacional.';

-- --- BI: somente leitura, somente o schema analitico -----------------------------------------
GRANT USAGE ON SCHEMA bi TO zera_bi_leitor;
GRANT SELECT ON ALL TABLES IN SCHEMA bi TO zera_bi_leitor;

-- Toda view criada em `bi` daqui em diante ja nasce legivel pelo BI. Sem isto, cada view nova
-- exigiria um GRANT manual que ninguem lembra de fazer -- e o painel quebra em producao.
ALTER DEFAULT PRIVILEGES IN SCHEMA bi GRANT SELECT ON TABLES TO zera_bi_leitor;

-- --- Auditor: trilha de auditoria e metadados ------------------------------------------------
GRANT USAGE ON SCHEMA public TO zera_auditor;
GRANT SELECT ON audit_log, audit_log_user_account, audit_log_alert, audit_log_organization
    TO zera_auditor;
GRANT SELECT ON job_execucao TO zera_auditor;

-- Nenhum dos dois papeis recebe INSERT/UPDATE/DELETE em lugar nenhum: leitura analitica e leitura
-- de auditoria sao, por definicao, somente leitura. Escrita no dominio e exclusividade da conta da
-- aplicacao.
