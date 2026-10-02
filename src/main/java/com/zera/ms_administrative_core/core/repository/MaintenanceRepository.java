package com.zera.ms_administrative_core.core.repository;

import java.time.LocalDate;

/**
 * Porta para as procedures de manutencao do banco.
 *
 * <p>Sao operacoes de CONJUNTO: fecham, revogam ou consolidam milhares de linhas de uma vez. Trazer
 * esse trabalho para o Java significaria carregar as linhas pela rede para decidir o obvio sobre
 * cada uma. A regra fica onde o dado esta; esta porta so a aciona.
 */
public interface MaintenanceRepository {

    /** Fecha alertas OPEN cujo evento e mais antigo que {@code dias}. Retorna quantos fechou. */
    int closeStaleAlerts(int dias);

    /** Revoga refresh tokens vencidos e remove os que passaram da janela de retencao. */
    TokenCleanup revokeExpiredTokens(int diasRetencao);

    /** Reconsolida o rollup de DAU no periodo a partir do log. Retorna quantos dias reescreveu. */
    int consolidateDau(LocalDate de, LocalDate ate);

    record TokenCleanup(int revogados, int removidos) {}
}
