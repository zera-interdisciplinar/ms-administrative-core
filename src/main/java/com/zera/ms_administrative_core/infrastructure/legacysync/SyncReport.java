package com.zera.ms_administrative_core.infrastructure.legacysync;

/**
 * @param saved   linhas gravadas no nosso banco
 * @param skipped linhas ignoradas (invalidas ou com pai ausente)
 * @param pushed  gestores cuja edicao local foi enviada ao legado
 */
public record SyncReport(int saved, int skipped, int pushed) {

    public SyncReport(int saved, int skipped) {
        this(saved, skipped, 0);
    }
}
