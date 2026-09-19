package com.zera.ms_administrative_core.infrastructure.legacysync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

public class LegacySyncJob {

    private static final Logger log = LoggerFactory.getLogger(LegacySyncJob.class);

    private final LegacySyncService service;

    public LegacySyncJob(LegacySyncService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${legacy-sync.interval}", initialDelayString = "${legacy-sync.initial-delay}")
    public void run() {
        try {
            SyncReport report = service.syncAll();
            log.info("Legacy sync concluido: {} gravados, {} ignorados", report.saved(), report.skipped());
        } catch (RuntimeException e) {
            log.error("Legacy sync falhou", e);
        }
    }
}
