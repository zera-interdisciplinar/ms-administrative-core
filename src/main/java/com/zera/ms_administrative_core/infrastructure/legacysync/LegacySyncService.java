package com.zera.ms_administrative_core.infrastructure.legacysync;

import java.sql.SQLException;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;

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
    private final LegacyWriter writer;
    private final OrganizationRepository organizationRepository;
    private final UnitRepository unitRepository;
    private final UserRepository userRepository;
    private final TelephoneRepository telephoneRepository;

    public LegacySyncService(LegacyReader reader, LegacyWriter writer,
            OrganizationRepository organizationRepository, UnitRepository unitRepository,
            UserRepository userRepository, TelephoneRepository telephoneRepository) {
        this.reader = reader;
        this.writer = writer;
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
            if (managerSaved) {
                pushToLegacy(counter, row);
            }
        }

        return new SyncReport(counter.saved, counter.skipped, counter.pushed);
    }

    /**
     * Devolve ao legado o que foi editado aqui depois da ultima edicao la. E a metade de escrita da
     * regra "quem editou por ultimo vence": a rodada ja preservou o valor local, e sem este passo o
     * legado seguiria com o valor antigo e voltaria a ganhar assim que alguem o editasse.
     *
     * <p>So vai uma coluna cujo valor local difere do legado, entao tocar o gestor sem mudar nada
     * (ativar, desativar, foto) nao escreve. Falha aqui nunca derruba a rodada: o valor local ja
     * esta salvo e a escrita e refeita na rodada seguinte.
     */
    private void pushToLegacy(Counter counter, LegacyReader.Gestor row) {
        try {
            UUID userId = LegacyIds.manager(row.codigo());
            Optional<User> user = userRepository.findById(userId);
            Optional<Telephone> phone = telephoneRepository.findByUserId(userId);
            boolean userNewer = user.isPresent() && localIsNewer(user.get().getUpdatedAt(), row.atualizadoEm());
            boolean phoneNewer = phone.isPresent() && localIsNewer(phone.get().getUpdatedAt(), row.atualizadoEm());

            String nome = userNewer && !user.get().getName().equals(row.nome()) ? user.get().getName() : null;
            String email = userNewer && !sameEmail(user.get().getEmail(), row.email())
                    ? user.get().getEmail().value() : null;
            String senha = userNewer && !user.get().getPassword().hash().equals(row.senha())
                    ? user.get().getPassword().hash() : null;
            String telefone = phoneNewer && !phone.get().getNumber().value().equals(digitsOf(row.telefone()))
                    ? phone.get().getNumber().value() : null;

            LegacyWriter.GestorChanges changes = new LegacyWriter.GestorChanges(nome, email, senha, telefone);
            if (changes.isEmpty()) {
                return;
            }

            // atualizado_em passa a ser o do local: na rodada seguinte o empate cai no legado, com
            // valores ja iguais, e a escrita nao se repete
            LocalDateTime newest = Stream.of(
                    nome != null || email != null || senha != null ? user.get().getUpdatedAt() : null,
                    telefone != null ? phone.get().getUpdatedAt() : null)
                    .filter(Objects::nonNull)
                    .max(Comparator.naturalOrder())
                    .orElseThrow();
            OffsetDateTime newUpdatedAt = newest.atZone(ZoneId.systemDefault()).toOffsetDateTime();

            if (writer.updateGestor(row.codigo(), changes, newUpdatedAt, row.atualizadoEm())) {
                counter.pushed++;
                log.info("Legacy sync: gestor codigo={} atualizado no legado ({})", row.codigo(),
                        String.join(", ", changes.columns()));
            } else {
                log.warn("Legacy sync: gestor codigo={} mudou no legado desde a leitura; escrita descartada",
                        row.codigo());
            }
        } catch (RuntimeException e) {
            // so a classe e o SQLState: a mensagem de violacao de unicidade traz o e-mail
            Throwable root = NestedExceptionUtils.getMostSpecificCause(e);
            String state = root instanceof SQLException sql ? " SQLState=" + sql.getSQLState() : "";
            log.warn("Legacy sync: gestor codigo={} nao pode ser enviado ao legado: {}{}", row.codigo(),
                    root.getClass().getSimpleName(), state);
        }
    }

    private static boolean sameEmail(Email local, String legacy) {
        return legacy != null && local.value().equals(legacy.trim().toLowerCase(Locale.ROOT));
    }

    private static String digitsOf(String telefone) {
        return telefone == null ? "" : telefone.replaceAll("\\D", "");
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
        UUID id = LegacyIds.manager(row.codigo());
        Optional<User> existing = userRepository.findById(id);
        // nome, e-mail e senha podem ser editados localmente: edicao local mais nova que a do legado nao pode ser
        // revertida na rodada seguinte. Qualquer touch() do usuario conta (ate ativar/desativar), o que
        // so erra para o lado seguro: preserva o local ate o legado ser editado de novo.
        boolean localWins = existing.isPresent()
                && localIsNewer(existing.get().getUpdatedAt(), row.atualizadoEm());

        Email email = localWins ? existing.get().getEmail() : new Email(row.email());
        if (row.codUnidade() == null) {
            throw new SkipRow("gestor sem unidade");
        }
        UUID unitId = LegacyIds.unit(row.codUnidade());
        if (unitRepository.findById(unitId).isEmpty()) {
            throw new SkipRow("unidade pai ausente");
        }
        // protege a coluna de hash caso o legado ainda tenha senha em texto puro; com o local
        // vencendo o hash legado nao e usado, entao nao precisa ser valido
        if (!localWins && !looksLikeBcrypt(row.senha())) {
            throw new SkipRow("senha do legado nao esta em bcrypt");
        }

        if (!localWins) {
            userRepository.findByEmail(email)
                    .filter(other -> !other.getUserId().equals(id))
                    .ifPresent(other -> {
                        throw new SkipRow("email ja usado por outro usuario");
                    });
        }

        String name = localWins ? existing.get().getName() : row.nome();
        // com o local vencendo, o updated_at local precisa sobreviver: gravar o do legado faria a
        // proxima rodada achar o legado "mais novo" e desfazer a edicao
        LocalDateTime updatedAt = localWins ? existing.get().getUpdatedAt() : toLocal(row.atualizadoEm());
        // o legado nao tem status de gestor: ACTIVE so na criacao
        Status status = existing.map(User::getStatus).orElse(Status.ACTIVE);
        // o legado nao tem foto: sem copiar o valor atual, o merge a zeraria a cada rodada
        String imageUrl = existing.map(User::getImageUrl).orElse(null);
        // troca de senha (API ou recuperacao por e-mail) faz touch() no usuario, entao segue a mesma
        // regra de nome/e-mail; sem isso o hash do legado desfazia a troca na rodada seguinte
        HashedPassword password = localWins ? existing.get().getPassword() : new HashedPassword(row.senha());
        userRepository.save(new Manager(id, name, email, password,
                status, unitId, toLocal(row.criadoEm()), updatedAt, imageUrl));
    }

    private void syncTelephone(LegacyReader.Gestor row) {
        UUID userId = LegacyIds.manager(row.codigo());
        Optional<Telephone> existing = telephoneRepository.findByUserId(userId);
        // o telefone tem updated_at proprio (alterar o numero nao toca o usuario), mas o legado so
        // tem o atualizado_em do gestor: e contra ele que a edicao local e comparada
        boolean localWins = existing.isPresent()
                && localIsNewer(existing.get().getUpdatedAt(), row.atualizadoEm());

        TelephoneNumber number = localWins ? existing.get().getNumber() : new TelephoneNumber(row.telefone());
        // telefone de usuario carrega organizacao e unidade, como no cadastro pela API
        UUID unitId = LegacyIds.unit(row.codUnidade());
        Unit unit = unitRepository.findById(unitId).orElseThrow(() -> new SkipRow("unidade pai ausente"));

        UUID id = existing.map(Telephone::getTelephoneId).orElse(LegacyIds.managerTelephone(row.codigo()));
        LocalDateTime createdAt = existing.map(Telephone::getCreatedAt).orElse(toLocal(row.criadoEm()));
        LocalDateTime updatedAt = localWins ? existing.get().getUpdatedAt() : toLocal(row.atualizadoEm());

        telephoneRepository.save(new Telephone(id, number, userId, unit.getOrganizationId(), unitId,
                createdAt, updatedAt));
    }

    // Empate vai para o legado (apos uma rodada os dois lados ficam iguais). Sem atualizado_em no
    // legado nao ha como provar que ele e mais novo, entao o dado local existente e preservado.
    private static boolean localIsNewer(LocalDateTime localUpdatedAt, OffsetDateTime legacyUpdatedAt) {
        return legacyUpdatedAt == null || localUpdatedAt.isAfter(toLocal(legacyUpdatedAt));
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
        int pushed;
    }

    private static final class SkipRow extends RuntimeException {
        SkipRow(String reason) {
            super(reason);
        }
    }
}
