package com.zera.ms_administrative_core.infrastructure.bootstrap;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.zera.ms_administrative_core.core.domain.UserFactory;
import com.zera.ms_administrative_core.core.domain.entity.Organization;
import com.zera.ms_administrative_core.core.domain.entity.Plan;
import com.zera.ms_administrative_core.core.domain.entity.Role;
import com.zera.ms_administrative_core.core.domain.entity.Unit;
import com.zera.ms_administrative_core.core.domain.entity.User;
import com.zera.ms_administrative_core.core.domain.service.PasswordHasher;
import com.zera.ms_administrative_core.core.domain.valueobject.Cnpj;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.domain.valueobject.RawPassword;
import com.zera.ms_administrative_core.core.domain.valueobject.Status;
import com.zera.ms_administrative_core.core.repository.OrganizationRepository;
import com.zera.ms_administrative_core.core.repository.UnitRepository;
import com.zera.ms_administrative_core.core.repository.UserRepository;

/**
 * Cria o primeiro usuario MANAGER (e a arvore organization -> unit que ele exige) no startup,
 * a partir de {@code BOOTSTRAP_ADMIN_*}. Roda em todo boot mas e idempotente pelo email:
 * se ja existir uma conta com o email informado, nada e feito.
 *
 * <p>Substitui a antiga migration V8 — uma migration versionada que faz no-op numa execucao fica
 * marcada como aplicada e nunca mais roda, mesmo quando as variaveis aparecem depois.
 */
@Component
public class InitialManagerSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InitialManagerSeeder.class);

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final UnitRepository unitRepository;
    private final PasswordHasher passwordHasher;

    public InitialManagerSeeder(UserRepository userRepository,
            OrganizationRepository organizationRepository,
            UnitRepository unitRepository,
            PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.unitRepository = unitRepository;
        this.passwordHasher = passwordHasher;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        BootstrapAdminSettings.fromEnv(System::getenv).ifPresentOrElse(this::seed,
                () -> log.info("Bootstrap: BOOTSTRAP_ADMIN_EMAIL/PASSWORD/ORG_CNPJ ausentes — "
                        + "nenhum MANAGER inicial a semear."));
    }

    private void seed(BootstrapAdminSettings settings) {
        Email adminEmail = new Email(settings.adminEmail());
        if (userRepository.existsByEmail(adminEmail)) {
            log.info("Bootstrap: conta {} ja existe — nada a fazer.", settings.adminEmail());
            return;
        }

        Cnpj cnpj = new Cnpj(settings.organizationCnpj());
        Organization organization = organizationRepository.findByCnpj(cnpj)
                .orElseGet(() -> organizationRepository.save(new Organization(
                        UUID.randomUUID(),
                        settings.organizationName(),
                        cnpj,
                        Status.ACTIVE,
                        new Email(settings.organizationEmail()),
                        Plan.valueOf(settings.organizationPlan().toUpperCase()))));

        Unit unit = new Unit(UUID.randomUUID(), settings.unitName(), organization.getOrganizationId());
        unitRepository.save(unit);

        User manager = UserFactory.create(
                Role.MANAGER,
                UUID.randomUUID(),
                settings.adminName(),
                adminEmail,
                passwordHasher.hash(new RawPassword(settings.rawPassword())),
                Status.ACTIVE,
                unit.getUnitId());
        userRepository.save(manager);

        log.info("Bootstrap: MANAGER inicial criado (email={}, organizationId={}, unitId={}).",
                settings.adminEmail(), organization.getOrganizationId(), unit.getUnitId());
    }
}
