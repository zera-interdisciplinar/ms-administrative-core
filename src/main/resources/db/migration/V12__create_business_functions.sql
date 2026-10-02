-- V12__create_business_functions.sql
--
-- Functions de regra de negocio. Criterio para uma regra morar no banco e nao no dominio Java:
-- ela precisa ser respondida em CONJUNTO (varias linhas de uma vez, para relatorio ou BI) ou
-- ser recursiva sobre uma hierarquia. Regra que decide o destino de UMA entidade continua no
-- dominio, onde da para testar sem banco.

-- ---------------------------------------------------------------------------------------------
-- 1) Validacao de CNPJ (digitos verificadores, modulo 11).
--
-- Vive no banco porque tambem precisa validar dado que ENTRA sem passar pelo dominio: o
-- legacysync le do banco do ano anterior e grava direto. IMMUTABLE porque depende so da entrada,
-- o que permite usa-la em indice e em CHECK.
--
-- ATENCAO: nao ha CHECK usando esta funcao nas tabelas existentes. Adicionar um exigiria que
-- TODO CNPJ ja gravado fosse valido, e o dado legado nao garante isso. Ver docs/otimizacao.md.
-- ---------------------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_validar_cnpj(p_cnpj TEXT)
RETURNS BOOLEAN
LANGUAGE plpgsql
IMMUTABLE
AS $$
DECLARE
    v_digitos  TEXT;
    v_soma     INTEGER;
    v_peso     INTEGER;
    v_i        INTEGER;
    v_dv1      INTEGER;
    v_dv2      INTEGER;
BEGIN
    IF p_cnpj IS NULL THEN
        RETURN FALSE;
    END IF;

    v_digitos := regexp_replace(p_cnpj, '\D', '', 'g');

    IF length(v_digitos) <> 14 THEN
        RETURN FALSE;
    END IF;

    -- CNPJ com todos os digitos iguais passa no modulo 11 por acidente aritmetico; e invalido.
    IF v_digitos ~ '^(\d)\1{13}$' THEN
        RETURN FALSE;
    END IF;

    -- Primeiro digito verificador: pesos 5..2 seguidos de 9..2 sobre os 12 primeiros digitos.
    v_soma := 0;
    v_peso := 5;
    FOR v_i IN 1..12 LOOP
        v_soma := v_soma + substr(v_digitos, v_i, 1)::INTEGER * v_peso;
        v_peso := CASE WHEN v_peso = 2 THEN 9 ELSE v_peso - 1 END;
    END LOOP;
    v_dv1 := 11 - (v_soma % 11);
    IF v_dv1 >= 10 THEN
        v_dv1 := 0;
    END IF;

    IF v_dv1 <> substr(v_digitos, 13, 1)::INTEGER THEN
        RETURN FALSE;
    END IF;

    -- Segundo digito: pesos 6..2 seguidos de 9..2 sobre os 13 primeiros.
    v_soma := 0;
    v_peso := 6;
    FOR v_i IN 1..13 LOOP
        v_soma := v_soma + substr(v_digitos, v_i, 1)::INTEGER * v_peso;
        v_peso := CASE WHEN v_peso = 2 THEN 9 ELSE v_peso - 1 END;
    END LOOP;
    v_dv2 := 11 - (v_soma % 11);
    IF v_dv2 >= 10 THEN
        v_dv2 := 0;
    END IF;

    RETURN v_dv2 = substr(v_digitos, 14, 1)::INTEGER;
END;
$$;

COMMENT ON FUNCTION fn_validar_cnpj(TEXT) IS
    'Valida os digitos verificadores de um CNPJ (modulo 11); aceita entrada com ou sem mascara.';

-- ---------------------------------------------------------------------------------------------
-- 2) Tamanho da equipe de um gestor, com CTE RECURSIVA.
--
-- `user_account.manager_id` e auto-referencia, entao a equipe de um gestor pode ter niveis: um
-- gestor que gerencia outro gestor. Contar so os subordinados diretos (o que um COUNT simples
-- faria) responde a pergunta errada. A recursao desce a arvore inteira.
--
-- O `ciclo` guarda o caminho e corta a recursao se um id reaparecer: `manager_id` e nullable e
-- sem CHECK anti-ciclo no schema, entao A->B->A e fisicamente possivel e derrubaria o servidor
-- com recursao infinita.
-- ---------------------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_tamanho_equipe(p_gestor_id UUID)
RETURNS INTEGER
LANGUAGE sql
STABLE
AS $$
    WITH RECURSIVE equipe AS (
        SELECT u.id, ARRAY[u.id] AS caminho
        FROM user_account u
        WHERE u.manager_id = p_gestor_id

        UNION ALL

        SELECT u.id, e.caminho || u.id
        FROM user_account u
        JOIN equipe e ON u.manager_id = e.id
        WHERE NOT u.id = ANY (e.caminho)
    )
    SELECT COUNT(DISTINCT id)::INTEGER FROM equipe;
$$;

COMMENT ON FUNCTION fn_tamanho_equipe(UUID) IS
    'Conta a subarvore inteira de subordinados de um gestor (CTE recursiva, protegida contra ciclo).';

-- ---------------------------------------------------------------------------------------------
-- 3) Indice de saude da unidade.
--
-- Traduz o volume e a gravidade dos alertas de uma unidade num numero de 0 a 100, onde 100 e
-- "nenhum alerta". Os pesos (HIGH=9, MEDIUM=3, LOW=1) sao a escala usada pelo ms-inventory para
-- ordenar severidade; mudar aqui sem mudar la faz o painel discordar do alerta.
--
-- Normaliza por usuario ativo da unidade porque unidade grande gera mais alerta por tamanho, nao
-- por estar pior: sem normalizar, o ranking so ordenaria unidades por numero de funcionarios.
-- ---------------------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_indice_saude_unidade(p_unidade_id UUID, p_de DATE, p_ate DATE)
RETURNS NUMERIC
LANGUAGE plpgsql
STABLE
AS $$
DECLARE
    v_peso      NUMERIC;
    v_usuarios  NUMERIC;
    v_penalidade NUMERIC;
BEGIN
    IF p_de IS NULL OR p_ate IS NULL OR p_ate < p_de THEN
        RAISE EXCEPTION 'Periodo invalido: de=% ate=%', p_de, p_ate
            USING ERRCODE = 'invalid_parameter_value';
    END IF;

    SELECT COALESCE(SUM(CASE a.severity
                            WHEN 'HIGH'   THEN 9
                            WHEN 'MEDIUM' THEN 3
                            ELSE 1
                        END), 0)
    INTO v_peso
    FROM alert a
    WHERE a.unit_id = p_unidade_id
      AND a.occurred_at >= p_de
      AND a.occurred_at < p_ate + 1;

    SELECT GREATEST(COUNT(*), 1)
    INTO v_usuarios
    FROM user_account u
    WHERE u.unit_id = p_unidade_id
      AND u.status = 'ACTIVE';

    -- ESCALA LOGARITMICA, e nao linear. Volume de alerta tem cauda longa: a maioria das unidades
    -- fica numa faixa estreita e umas poucas geram ordens de magnitude mais. Numa escala linear a
    -- constante que separa bem as unidades boas satura em zero todas as ruins, e o indice para de
    -- distinguir "ruim" de "catastrofico" -- verificado na carga de bench, onde TODAS as unidades
    -- davam 0.00. O log mantem discriminacao por varias ordens de magnitude.
    v_penalidade := LEAST(100, 12 * LN(1 + (v_peso / v_usuarios)));

    RETURN ROUND(100 - v_penalidade, 2);
END;
$$;

COMMENT ON FUNCTION fn_indice_saude_unidade(UUID, DATE, DATE) IS
    'Indice 0-100 de saude da unidade no periodo, ponderado por severidade e normalizado por usuario ativo.';
