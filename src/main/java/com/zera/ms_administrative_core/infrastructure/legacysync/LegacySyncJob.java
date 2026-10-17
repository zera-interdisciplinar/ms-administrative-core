package com.zera.ms_administrative_core.infrastructure.legacysync;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.scheduling.annotation.Scheduled;

public class LegacySyncJob {

    private static final Logger log = LoggerFactory.getLogger(LegacySyncJob.class);

    private final LegacySyncService service;

    public LegacySyncJob(LegacySyncService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${legacy-sync.interval}", initialDelayString = "${legacy-sync.initial-delay}")
    public void run() {
        long start = System.nanoTime();
        try {
            SyncReport report = service.syncAll();
            // a duracao e o dado que diz se o intervalo configurado ainda cabe no volume de dados
            log.info("Legacy sync concluido: {} gravados, {} ignorados, {} enviados ao legado em {}ms",
                    report.saved(), report.skipped(), report.pushed(), elapsedMillis(start));
        } catch (CannotGetJdbcConnectionException e) {
            // legado fora do ar e um estado esperado e se repete a cada rodada: o stack de ~40 linhas
            // so esconde a unica linha util, a causa raiz (timeout, DNS, senha, SSL...)
            Throwable root = NestedExceptionUtils.getMostSpecificCause(e);
            log.error("Legacy sync falhou em {}ms, sem conexao com o legado: {}: {}",
                    elapsedMillis(start), root.getClass().getName(), root.getMessage());
        } catch (RuntimeException e) {
            log.error("Legacy sync falhou em {}ms", elapsedMillis(start), e);
        }
    }

    private static long elapsedMillis(long startNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }
}
