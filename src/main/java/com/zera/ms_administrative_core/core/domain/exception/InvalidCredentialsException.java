package com.zera.ms_administrative_core.core.domain.exception;

/**
 * Lancada quando o login falha: email desconhecido, senha incorreta ou conta nao ativa.
 * A mensagem e deliberadamente generica para nao permitir enumeracao de contas.
 */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Credenciais invalidas");
    }
}
