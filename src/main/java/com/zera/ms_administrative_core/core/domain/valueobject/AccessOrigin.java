package com.zera.ms_administrative_core.core.domain.valueobject;

/**
 * Por que uma sessao foi emitida: login com senha ou renovacao de refresh token.
 *
 * <p>Os nomes batem com os valores aceitos por {@code user_access_log.origem} (V12). BACKFILL nao
 * esta aqui de proposito: e usado apenas pela consolidacao do banco, nunca emitido pela aplicacao.
 */
public enum AccessOrigin {
    LOGIN,
    REFRESH
}
