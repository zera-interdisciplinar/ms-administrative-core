package com.zera.ms_administrative_core.infrastructure.legacysync;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zera.ms_administrative_core.core.domain.entity.Manager;
import com.zera.ms_administrative_core.core.domain.entity.Organization;
import com.zera.ms_administrative_core.core.domain.entity.Plan;
import com.zera.ms_administrative_core.core.domain.entity.Telephone;
import com.zera.ms_administrative_core.core.domain.entity.Unit;
import com.zera.ms_administrative_core.core.domain.entity.User;
import com.zera.ms_administrative_core.core.domain.valueobject.Cnpj;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.domain.valueobject.HashedPassword;
import com.zera.ms_administrative_core.core.domain.valueobject.Status;
import com.zera.ms_administrative_core.core.domain.valueobject.TelephoneNumber;
import com.zera.ms_administrative_core.core.repository.OrganizationRepository;
import com.zera.ms_administrative_core.core.repository.TelephoneRepository;
import com.zera.ms_administrative_core.core.repository.UnitRepository;
import com.zera.ms_administrative_core.core.repository.UserRepository;

public class LegacySyncService {

    private static final Logger log = LoggerFactory.getLogger(LegacySyncService.class);

    private final LegacyReader reader;
    private final OrganizationRepository organizationRepository;
    private final UnitRepository unitRepository;
    private final UserRepository userRepository;
    private final TelephoneRepository telephoneRepository;

    public LegacySyncService(LegacyReader reader, OrganizationRepository organizationRepository,
            UnitRepository unitRepository, UserRepository userRepository,
            TelephoneRepository telephoneRepository) {
        this.reader = reader;
        this.organizationRepository = organizationRepository;
        this.unitRepository = unitRepository;
        this.userRepository = userRepository;
        this.telephoneRepository = telephoneRepository;
    }

    public SyncReport syncAll() {
        Counter counter = new Counter();

        for (LegacyReader.Organizacao row : reader.organizacoes()) {
            run(counter, "organizacao", row.codigo(), () -> syncOrganization(row));
        }
        for (LegacyReader.Unidade row : reader.unidades()) {
            run(counter, "unidade", row.codigo(), () -> syncUnit(row));
        }
        for (LegacyReader.Gestor row : reader.gestores()) {
            boolean managerSaved = run(counter, "gestor", row.codigo(), () -> syncManager(row));
            if (managerSaved && row.telefone() != null && !row.telefone().isBlank()) {
                run(counter, "telefone", row.codigo(), () -> syncTelephone(row));
            }
        }

        return new SyncReport(counter.saved, counter.skipped);
    }

    private void syncOrganization(LegacyReader.Organizacao row) {
        Cnpj cnpj = new Cnpj(row.cnpj());
        Email email = new Email(row.email());
        Organization organization = new Organization(
                LegacyIds.organization(row.codigo()), row.nome(), cnpj,
                statusOf(row.statusAssinatura()), email, planOf(row.plano()),
                toLocal(row.criadoEm()), toLocal(row.atualizadoEm()));
        organizationRepository.save(organization);
    }

    private void syncUnit(LegacyReader.Unidade row) {
        if (row.codOrganizacao() == null) {
            throw new SkipRow("unidade sem organizacao");
        }
        UUID organizationId = LegacyIds.organization(row.codOrganizacao());
        if (organizationRepository.findById(organizationId).isEmpty()) {
            throw new SkipRow("organizacao pai ausente");
        }

        UUID id = LegacyIds.unit(row.codigo());
        // o legado nao tem nome de unidade: gera na criacao e nunca sobrescreve depois
        String name = unitRepository.findById(id).map(Unit::getName).orElse("Unidade " + row.codigo());
        unitRepository.save(new Unit(id, name, organizationId,
                toLocal(row.criadoEm()), toLocal(row.atualizadoEm())));
    }

    private void syncManager(LegacyReader.Gestor row) {
        Email email = new Email(row.email());
        if (row.codUnidade() == null) {
            throw new SkipRow("gestor sem unidade");
        }
        UUID unitId = LegacyIds.unit(row.codUnidade());
        if (unitRepository.findById(unitId).isEmpty()) {
            throw new SkipRow("unidade pai ausente");
        }
        // protege a coluna de hash caso o legado ainda tenha senha em texto puro
        if (!looksLikeBcrypt(row.senha())) {
            throw new SkipRow("senha do legado nao esta em bcrypt");
        }

        UUID id = LegacyIds.manager(row.codigo());
        userRepository.findByEmail(email)
                .filter(other -> !other.getUserId().equals(id))
                .ifPresent(other -> {
                    throw new SkipRow("email ja usado por outro usuario");
                });

        // o legado nao tem status de gestor: ACTIVE so na criacao
        Status status = userRepository.findById(id).map(User::getStatus).orElse(Status.ACTIVE);
        userRepository.save(new Manager(id, row.nome(), email, new HashedPassword(row.senha()),
                status, unitId, toLocal(row.criadoEm()), toLocal(row.atualizadoEm())));
    }

    private void syncTelephone(LegacyReader.Gestor row) {
        TelephoneNumber number = new TelephoneNumber(row.telefone());
        UUID userId = LegacyIds.manager(row.codigo());
        // telefone de usuario carrega organizacao e unidade, como no cadastro pela API
        UUID unitId = LegacyIds.unit(row.codUnidade());
        Unit unit = unitRepository.findById(unitId).orElseThrow(() -> new SkipRow("unidade pai ausente"));

        Optional<Telephone> existing = telephoneRepository.findByUserId(userId);
        UUID id = existing.map(Telephone::getTelephoneId).orElse(LegacyIds.managerTelephone(row.codigo()));
        LocalDateTime createdAt = existing.map(Telephone::getCreatedAt).orElse(toLocal(row.criadoEm()));

        telephoneRepository.save(new Telephone(id, number, userId, unit.getOrganizationId(), unitId,
                createdAt, toLocal(row.atualizadoEm())));
    }

    private static Plan planOf(String plano) {
        if (plano == null) {
            return Plan.FREE;
        }
        String normalized = Normalizer.normalize(plano.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "gratis" -> Plan.FREE;
            case "profissional" -> Plan.PROFISSIONAL;
            case "empresarial" -> Plan.EMPRESARIAL;
            default -> throw new SkipRow("plano desconhecido");
        };
    }

    private static Status statusOf(String statusAssinatura) {
        return "Ativa".equalsIgnoreCase(statusAssinatura) ? Status.ACTIVE : Status.INACTIVE;
    }

    private static boolean looksLikeBcrypt(String senha) {
        return senha != null && senha.length() == 60 && senha.startsWith("$2");
    }

    // TIMESTAMP(0) arredonda ao gravar; truncar aqui evita um UPDATE a cada rodada
    private static LocalDateTime toLocal(OffsetDateTime value) {
        LocalDateTime local = value == null
                ? LocalDateTime.now()
                : LocalDateTime.ofInstant(value.toInstant(), ZoneId.systemDefault());
        return local.truncatedTo(ChronoUnit.SECONDS);
    }

    private boolean run(Counter counter, String kind, int codigo, Runnable work) {
        try {
            work.run();
            counter.saved++;
            return true;
        } catch (SkipRow e) {
            counter.skipped++;
            log.warn("Legacy sync: {} codigo={} ignorado: {}", kind, codigo, e.getMessage());
        } catch (RuntimeException e) {
            counter.skipped++;
            log.warn("Legacy sync: {} codigo={} falhou: {}", kind, codigo, e.getClass().getSimpleName());
        }
        return false;
    }

    private static final class Counter {
        int saved;
        int skipped;
    }

    private static final class SkipRow extends RuntimeException {
        SkipRow(String reason) {
            super(reason);
        }
    }
}
