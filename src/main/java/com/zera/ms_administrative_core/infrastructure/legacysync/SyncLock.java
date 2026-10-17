package com.zera.ms_administrative_core.infrastructure.legacysync;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Garante que so um pod por vez execute uma rodada do sync. Sem isso, N replicas rodam N rodadas
 * por intervalo, relendo as mesmas tabelas e disputando as mesmas linhas.
 */
public interface SyncLock {

    /**
     * Executa {@code work} se este pod obteve o lock; devolve vazio, sem executar, se outro pod ja
     * esta sincronizando. O lock e solto ao fim do trabalho, inclusive quando ele lanca excecao.
     */
    <T> Optional<T> tryRun(Supplier<T> work);
}
