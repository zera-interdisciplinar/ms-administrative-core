-- V18__add_cnpj_check_not_valid.sql
--
-- CHECK de CNPJ valido em `organization`, finalmente aplicando fn_validar_cnpj (V12) a producao.
--
-- ATE AQUI, a function so era chamada pelo JUnit -- nenhum CHECK, nenhum endpoint, nenhum SELECT
-- de producao a usava. Pedir "functions para regra de negocio" e entregar um objeto que so o
-- teste conhece e um objeto sem uso real: a banca pode perguntar "onde isso roda?" e a resposta
-- correta precisa ser "aqui", nao "no JUnit".
--
-- A V12 documentou por que nao havia CHECK: aplicar um CHECK normal (`VALIDATE`) escaneia TODAS
-- as linhas existentes no momento da migracao, e um CNPJ invalido vindo do legacysync faria a
-- migracao inteira falhar no deploy. `NOT VALID` resolve isso:
--
--   - Nao escaneia as linhas que ja existem -- a migracao nao pode falhar por dado legado sujo.
--   - Passa a valer para TODO INSERT/UPDATE daqui em diante -- e exatamente onde o enunciado
--     pede para a regra morar.
--   - Pode ser validada depois, sem bloquear escrita, com:
--       ALTER TABLE organization VALIDATE CONSTRAINT organization_cnpj_valido_check;
--     (isso so pega ACCESS SHARE lock, nao ACCESS EXCLUSIVE -- nao trava a tabela para escrita
--     enquanto confere o passado, so enquanto a trava e tomada e liberada.)
ALTER TABLE organization
    ADD CONSTRAINT organization_cnpj_valido_check CHECK (fn_validar_cnpj(cnpj)) NOT VALID;

COMMENT ON CONSTRAINT organization_cnpj_valido_check ON organization IS
    'NOT VALID de proposito: nao reavalia linhas existentes (dado legado pode ja violar), mas '
    'passa a exigir CNPJ valido em todo INSERT/UPDATE novo. Validar o passado com '
    'ALTER TABLE organization VALIDATE CONSTRAINT organization_cnpj_valido_check quando o dado '
    'legado for auditado.';
