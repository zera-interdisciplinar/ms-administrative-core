package com.zera.ms_administrative_core.infrastructure.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.zera.ms_administrative_core.core.domain.entity.Organization;
import com.zera.ms_administrative_core.core.domain.entity.Plan;
import com.zera.ms_administrative_core.core.domain.entity.Role;
import com.zera.ms_administrative_core.core.domain.entity.Unit;
import com.zera.ms_administrative_core.core.domain.entity.User;
import com.zera.ms_administrative_core.core.domain.valueobject.Cnpj;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.domain.valueobject.Status;
import com.zera.ms_administrative_core.core.repository.OrganizationRepository;
import com.zera.ms_administrative_core.core.repository.UnitRepository;
import com.zera.ms_administrative_core.support.FixedPasswordHasher;
import com.zera.ms_administrative_core.support.InMemoryUserRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InitialManagerSeederTest {

    private static final String VALID_CNPJ = "11222333000181";

    private InMemoryUserRepository users;
    @Mock private OrganizationRepository organizations;
    @Mock private UnitRepository units;

    private InitialManagerSeeder seeder;

    @BeforeEach
    void setUp() {
        users = new InMemoryUserRepository();
        seeder = new InitialManagerSeeder(users, organizations, units, new FixedPasswordHasher());
        when(organizations.save(any(Organization.class))).thenAnswer(i -> i.getArgument(0));
    }

    private void runWith(java.util.Map<String, String> env) {
        try {
            var m = InitialManagerSeeder.class.getDeclaredMethod("seed", BootstrapAdminSettings.class);
            m.setAccessible(true);
            m.invoke(seeder, BootstrapAdminSettings.fromEnv(env::get).orElseThrow());
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw (RuntimeException) e.getCause();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private java.util.Map<String, String> env() {
        var e = new java.util.HashMap<String, String>();
        e.put("BOOTSTRAP_ADMIN_EMAIL", "admin@zera.com");
        e.put("BOOTSTRAP_ADMIN_PASSWORD", "secret");
        e.put("BOOTSTRAP_ADMIN_ORG_CNPJ", VALID_CNPJ);
        return e;
    }

    @Test
    void noEnvVarsIsANoOp() {
        seeder.run(null); // System.getenv sem as BOOTSTRAP_ADMIN_* no ambiente de teste

        assertThat(users.findByEmail(new Email("admin@zera.com"))).isEmpty();
        verifyNoInteractions(units);
    }

    @Test
    void createsOrgUnitAndManager() {
        when(organizations.findByCnpj(new Cnpj(VALID_CNPJ))).thenReturn(Optional.empty());

        runWith(env());

        ArgumentCaptor<Organization> org = ArgumentCaptor.forClass(Organization.class);
        verify(organizations).save(org.capture());
        assertThat(org.getValue().getStatus()).isEqualTo(Status.ACTIVE);
        assertThat(org.getValue().getPlan()).isEqualTo(Plan.FREE);

        ArgumentCaptor<Unit> unit = ArgumentCaptor.forClass(Unit.class);
        verify(units).save(unit.capture());

        User manager = users.findByEmail(new Email("admin@zera.com")).orElseThrow();
        assertThat(manager.role()).isEqualTo(Role.MANAGER);
        assertThat(manager.getStatus()).isEqualTo(Status.ACTIVE);
        assertThat(manager.getUnitId()).isEqualTo(unit.getValue().getUnitId());
    }

    @Test
    void reusesExistingOrganizationWithSameCnpj() {
        Organization existing = new Organization(java.util.UUID.randomUUID(), "Ja existe",
                new Cnpj(VALID_CNPJ), Status.ACTIVE, new Email("x@z.com"), Plan.PRO);
        when(organizations.findByCnpj(new Cnpj(VALID_CNPJ))).thenReturn(Optional.of(existing));

        runWith(env());

        verify(organizations, never()).save(any());
        assertThat(users.findByEmail(new Email("admin@zera.com")).orElseThrow().getUnitId()).isNotNull();
    }

    @Test
    void idempotentWhenAdminEmailAlreadyExists() {
        Organization existing = new Organization(java.util.UUID.randomUUID(), "Org",
                new Cnpj(VALID_CNPJ), Status.ACTIVE, new Email("x@z.com"), Plan.FREE);
        when(organizations.findByCnpj(any())).thenReturn(Optional.of(existing));
        runWith(env());
        int before = users.findAll(null, null, 0, 100).size();

        runWith(env());

        assertThat(users.findAll(null, null, 0, 100)).hasSize(before);
        verify(units).save(any()); // uma unica vez, na primeira execucao
    }
}
