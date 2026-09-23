package com.zera.ms_administrative_core.infrastructure.legacysync;

import com.zera.ms_administrative_core.core.domain.entity.Manager;
import com.zera.ms_administrative_core.core.domain.entity.Organization;
import com.zera.ms_administrative_core.core.domain.entity.Plan;
import com.zera.ms_administrative_core.core.domain.entity.Role;
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
import com.zera.ms_administrative_core.infrastructure.legacysync.LegacyReader.Gestor;
import com.zera.ms_administrative_core.infrastructure.legacysync.LegacyReader.Organizacao;
import com.zera.ms_administrative_core.infrastructure.legacysync.LegacyReader.Unidade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegacySyncServiceTest {

    private static final String CNPJ = "11222333000181";
    private static final String MASKED_CNPJ = "11.222.333/0001-81";
    private static final String HASH = "$2a$10$" + "a".repeat(53);

    private static final OffsetDateTime CREATED = OffsetDateTime.of(2023, 5, 17, 10, 20, 30, 123_000_000,
            ZoneOffset.ofHours(-3));
    private static final OffsetDateTime UPDATED = OffsetDateTime.of(2024, 1, 2, 3, 4, 5, 999_000_000,
            ZoneOffset.UTC);

    @Mock
    private LegacyReader reader;
    @Mock
    private OrganizationRepository organizationRepository;
    @Mock
    private UnitRepository unitRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TelephoneRepository telephoneRepository;

    private LegacySyncService service;

    @BeforeEach
    void setUp() {
        service = new LegacySyncService(reader, organizationRepository, unitRepository, userRepository,
                telephoneRepository);
        // the ports that return the saved entity: echo back the argument, whether or not the service uses it
        lenient().when(organizationRepository.save(any(Organization.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(telephoneRepository.save(any(Telephone.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    // ------------------------------------------------------------------
    // fixtures
    // ------------------------------------------------------------------

    private static LocalDateTime expected(OffsetDateTime odt) {
        return LocalDateTime.ofInstant(odt.toInstant(), ZoneId.systemDefault()).truncatedTo(ChronoUnit.SECONDS);
    }

    private static Organizacao organizacao(int codigo, String cnpj, String email, String plano, String status) {
        return new Organizacao(codigo, cnpj, "Org " + codigo, email, CREATED, UPDATED, plano, status);
    }

    private static Organizacao validOrganizacao(int codigo) {
        return organizacao(codigo, CNPJ, "org" + codigo + "@b.com", "Grátis", "Ativa");
    }

    private static Unidade unidade(int codigo, Integer codOrganizacao) {
        return new Unidade(codigo, codOrganizacao, CREATED, UPDATED);
    }

    private static Gestor gestor(int codigo, String email, String senha, String telefone, Integer codUnidade) {
        return new Gestor(codigo, "Gestor " + codigo, email, senha, telefone, codUnidade, CREATED, UPDATED);
    }

    private static Gestor validGestor(int codigo) {
        return gestor(codigo, "g" + codigo + "@b.com", HASH, null, 5);
    }

    private static Organization localOrganization(int codigo) {
        return new Organization(LegacyIds.organization(codigo), "Org", new Cnpj(CNPJ), Status.ACTIVE,
                new Email("org@b.com"), Plan.FREE);
    }

    private static Unit localUnit(int codigo, String name) {
        return new Unit(LegacyIds.unit(codigo), name, LegacyIds.organization(1));
    }

    private static Manager localManager(int codigo, String email, Status status) {
        LocalDateTime now = LocalDateTime.now();
        return new Manager(LegacyIds.manager(codigo), "Existing", new Email(email), new HashedPassword(HASH),
                status, LegacyIds.unit(5), now, now);
    }

    private void givenReader(List<Organizacao> organizacoes, List<Unidade> unidades, List<Gestor> gestores) {
        lenient().when(reader.organizacoes()).thenReturn(organizacoes);
        lenient().when(reader.unidades()).thenReturn(unidades);
        lenient().when(reader.gestores()).thenReturn(gestores);
    }

    private void givenOrganizacoes(Organizacao... rows) {
        givenReader(List.of(rows), List.of(), List.of());
    }

    private void givenUnidades(Unidade... rows) {
        givenReader(List.of(), List.of(rows), List.of());
    }

    private void givenGestores(Gestor... rows) {
        givenReader(List.of(), List.of(), List.of(rows));
    }

    /** The parent organization already exists locally (lenient: a row may be skipped before the lookup). */
    private void givenLocalOrganization(int codigo) {
        lenient().when(organizationRepository.findById(LegacyIds.organization(codigo)))
                .thenReturn(Optional.of(localOrganization(codigo)));
    }

    /** The parent unit already exists locally (lenient: a row may be skipped before the lookup). */
    private void givenLocalUnit(int codigo) {
        lenient().when(unitRepository.findById(LegacyIds.unit(codigo)))
                .thenReturn(Optional.of(localUnit(codigo, "Existing unit")));
    }

    private Organization capturedOrganization() {
        ArgumentCaptor<Organization> captor = ArgumentCaptor.forClass(Organization.class);
        verify(organizationRepository).save(captor.capture());
        return captor.getValue();
    }

    private Unit capturedUnit() {
        ArgumentCaptor<Unit> captor = ArgumentCaptor.forClass(Unit.class);
        verify(unitRepository).save(captor.capture());
        return captor.getValue();
    }

    private User capturedUser() {
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        return captor.getValue();
    }

    private Telephone capturedTelephone() {
        ArgumentCaptor<Telephone> captor = ArgumentCaptor.forClass(Telephone.class);
        verify(telephoneRepository).save(captor.capture());
        return captor.getValue();
    }

    private void verifyNoSavesExcept(boolean organization, boolean unit, boolean user,
            boolean telephone) {
        if (!organization) {
            verify(organizationRepository, never()).save(any());
        }
        if (!unit) {
            verify(unitRepository, never()).save(any());
        }
        if (!user) {
            verify(userRepository, never()).save(any());
        }
        if (!telephone) {
            verify(telephoneRepository, never()).save(any());
        }
    }

    // ------------------------------------------------------------------
    // organizations
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Should save an organization mapping every legacy field")
    void shouldSaveOrganizationFieldByField() {
        givenOrganizacoes(new Organizacao(7, CNPJ, "Acme", "a@b.com", CREATED, UPDATED, "Profissional", "Ativa"));

        SyncReport report = service.syncAll();

        Organization saved = capturedOrganization();
        assertThat(saved.getOrganizationId()).isEqualTo(LegacyIds.organization(7));
        assertThat(saved.getName()).isEqualTo("Acme");
        assertThat(saved.getCnpj()).isEqualTo(new Cnpj(CNPJ));
        assertThat(saved.getEmail()).isEqualTo(new Email("a@b.com"));
        assertThat(saved.getPlan()).isEqualTo(Plan.PROFISSIONAL);
        assertThat(saved.getStatus()).isEqualTo(Status.ACTIVE);
        assertThat(saved.getCreatedAt()).isEqualTo(expected(CREATED));
        assertThat(saved.getUpdatedAt()).isEqualTo(expected(UPDATED));
        assertThat(report).isEqualTo(new SyncReport(1, 0));
        verifyNoSavesExcept(true, false, false, false);
    }

    @ParameterizedTest(name = "plano \"{0}\" -> {1}")
    @DisplayName("Should map the legacy plan text to the local Plan")
    @CsvSource({
            "Grátis,FREE",
            "Profissional,PROFISSIONAL",
            "Empresarial,EMPRESARIAL"
    })
    void shouldMapPlan(String plano, Plan plan) {
        givenOrganizacoes(organizacao(1, CNPJ, "a@b.com", plano, "Ativa"));

        SyncReport report = service.syncAll();

        assertThat(capturedOrganization().getPlan()).isEqualTo(plan);
        assertThat(report).isEqualTo(new SyncReport(1, 0));
    }

    @Test
    @DisplayName("Should map a null plan (organization without subscription) to FREE")
    void shouldMapNullPlanToFree() {
        givenOrganizacoes(organizacao(1, CNPJ, "a@b.com", null, "Ativa"));

        SyncReport report = service.syncAll();

        assertThat(capturedOrganization().getPlan()).isEqualTo(Plan.FREE);
        assertThat(report).isEqualTo(new SyncReport(1, 0));
    }

    @Test
    @DisplayName("Should skip an organization with an unknown plan")
    void shouldSkipOrganizationWithUnknownPlan() {
        givenOrganizacoes(organizacao(1, CNPJ, "a@b.com", "Diamante", "Ativa"));

        SyncReport report = service.syncAll();

        assertThat(report).isEqualTo(new SyncReport(0, 1));
        verifyNoSavesExcept(false, false, false, false);
    }

    @ParameterizedTest(name = "statusAssinatura \"{0}\" -> ACTIVE")
    @DisplayName("Should map \"Ativa\" (case-insensitive) to ACTIVE")
    @ValueSource(strings = { "Ativa", "ATIVA", "ativa" })
    void shouldMapActiveStatus(String statusAssinatura) {
        givenOrganizacoes(organizacao(1, CNPJ, "a@b.com", "Grátis", statusAssinatura));

        service.syncAll();

        assertThat(capturedOrganization().getStatus()).isEqualTo(Status.ACTIVE);
    }

    @ParameterizedTest(name = "statusAssinatura \"{0}\" -> INACTIVE")
    @DisplayName("Should map any subscription status other than \"Ativa\" to INACTIVE")
    @ValueSource(strings = { "Cancelada", "Suspensa", "Inativa", "" })
    void shouldMapOtherStatusToInactive(String statusAssinatura) {
        givenOrganizacoes(organizacao(1, CNPJ, "a@b.com", "Grátis", statusAssinatura));

        SyncReport report = service.syncAll();

        assertThat(capturedOrganization().getStatus()).isEqualTo(Status.INACTIVE);
        assertThat(report).isEqualTo(new SyncReport(1, 0));
    }

    @Test
    @DisplayName("Should map a null subscription status to INACTIVE")
    void shouldMapNullStatusToInactive() {
        givenOrganizacoes(organizacao(1, CNPJ, "a@b.com", "Grátis", null));

        SyncReport report = service.syncAll();

        assertThat(capturedOrganization().getStatus()).isEqualTo(Status.INACTIVE);
        assertThat(report).isEqualTo(new SyncReport(1, 0));
    }

    @Test
    @DisplayName("Should strip the mask of a formatted CNPJ")
    void shouldStripCnpjMask() {
        givenOrganizacoes(organizacao(1, MASKED_CNPJ, "a@b.com", "Grátis", "Ativa"));

        SyncReport report = service.syncAll();

        assertThat(capturedOrganization().getCnpj().value()).isEqualTo(CNPJ);
        assertThat(report).isEqualTo(new SyncReport(1, 0));
    }

    @ParameterizedTest(name = "cnpj \"{0}\" is skipped")
    @DisplayName("Should skip an organization with an invalid CNPJ")
    @ValueSource(strings = { "11222333000182", "123", "00000000000000", "abc" })
    void shouldSkipInvalidCnpj(String cnpj) {
        givenOrganizacoes(organizacao(1, cnpj, "a@b.com", "Grátis", "Ativa"));

        SyncReport report = service.syncAll();

        assertThat(report).isEqualTo(new SyncReport(0, 1));
        verifyNoSavesExcept(false, false, false, false);
    }

    @ParameterizedTest(name = "email \"{0}\" is skipped")
    @DisplayName("Should skip an organization with a null or invalid email")
    @NullSource
    @ValueSource(strings = { "", "not-an-email", "a@b" })
    void shouldSkipOrganizationWithInvalidEmail(String email) {
        givenOrganizacoes(organizacao(1, CNPJ, email, "Grátis", "Ativa"));

        SyncReport report = service.syncAll();

        assertThat(report).isEqualTo(new SyncReport(0, 1));
        verifyNoSavesExcept(false, false, false, false);
    }

    @Test
    @DisplayName("Should keep processing organizations after an invalid one")
    void shouldProcessRemainingOrganizationsAfterInvalidRow() {
        givenOrganizacoes(
                organizacao(1, "11222333000182", "a@b.com", "Grátis", "Ativa"),
                validOrganizacao(2));

        SyncReport report = service.syncAll();

        assertThat(capturedOrganization().getOrganizationId()).isEqualTo(LegacyIds.organization(2));
        assertThat(report).isEqualTo(new SyncReport(1, 1));
    }

    // ------------------------------------------------------------------
    // units
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Should save a new unit deriving its name from the legacy code")
    void shouldSaveUnitWithDerivedNameOnCreate() {
        givenLocalOrganization(3);
        givenUnidades(new Unidade(9, 3, CREATED, UPDATED));

        SyncReport report = service.syncAll();

        Unit saved = capturedUnit();
        assertThat(saved.getUnitId()).isEqualTo(LegacyIds.unit(9));
        assertThat(saved.getOrganizationId()).isEqualTo(LegacyIds.organization(3));
        assertThat(saved.getName()).isEqualTo("Unidade 9");
        assertThat(saved.getCreatedAt()).isEqualTo(expected(CREATED));
        assertThat(saved.getUpdatedAt()).isEqualTo(expected(UPDATED));
        assertThat(report).isEqualTo(new SyncReport(1, 0));
    }

    @Test
    @DisplayName("Should keep the existing name of a unit that is already synced")
    void shouldPreserveUnitNameOnUpdate() {
        givenLocalOrganization(3);
        when(unitRepository.findById(LegacyIds.unit(9)))
                .thenReturn(Optional.of(new Unit(LegacyIds.unit(9), "Matriz Centro", LegacyIds.organization(3))));
        givenUnidades(new Unidade(9, 3, CREATED, UPDATED));

        SyncReport report = service.syncAll();

        Unit saved = capturedUnit();
        assertThat(saved.getUnitId()).isEqualTo(LegacyIds.unit(9));
        assertThat(saved.getOrganizationId()).isEqualTo(LegacyIds.organization(3));
        assertThat(saved.getName()).isEqualTo("Matriz Centro");
        assertThat(report).isEqualTo(new SyncReport(1, 0));
    }

    @Test
    @DisplayName("Should skip a unit whose legacy organization code is null")
    void shouldSkipUnitWithNullOrganization() {
        givenUnidades(unidade(9, null));

        SyncReport report = service.syncAll();

        assertThat(report).isEqualTo(new SyncReport(0, 1));
        verifyNoSavesExcept(false, false, false, false);
    }

    @Test
    @DisplayName("Should skip a unit whose parent organization does not exist locally")
    void shouldSkipUnitWhenParentOrganizationMissing() {
        when(organizationRepository.findById(LegacyIds.organization(3))).thenReturn(Optional.empty());
        givenUnidades(unidade(9, 3));

        SyncReport report = service.syncAll();

        assertThat(report).isEqualTo(new SyncReport(0, 1));
        verifyNoSavesExcept(false, false, false, false);
    }

    // ------------------------------------------------------------------
    // managers
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Should save a new manager mapping every legacy field and defaulting to ACTIVE")
    void shouldSaveManagerFieldByField() {
        givenLocalUnit(5);
        givenGestores(new Gestor(12, "Maria", "A@B.com", HASH, null, 5, CREATED, UPDATED));

        SyncReport report = service.syncAll();

        User saved = capturedUser();
        assertThat(saved).isInstanceOf(Manager.class);
        assertThat(saved.role()).isEqualTo(Role.MANAGER);
        assertThat(saved.getUserId()).isEqualTo(LegacyIds.manager(12));
        assertThat(saved.getName()).isEqualTo("Maria");
        assertThat(saved.getEmail()).isEqualTo(new Email("a@b.com"));
        assertThat(saved.getPassword().hash()).isEqualTo(HASH);
        assertThat(saved.getUnitId()).isEqualTo(LegacyIds.unit(5));
        assertThat(saved.getStatus()).isEqualTo(Status.ACTIVE);
        assertThat(saved.getCreatedAt()).isEqualTo(expected(CREATED));
        assertThat(saved.getUpdatedAt()).isEqualTo(expected(UPDATED));
        assertThat(report).isEqualTo(new SyncReport(1, 0));
    }

    @ParameterizedTest(name = "senha \"{0}\" is skipped")
    @DisplayName("Should skip a manager whose password does not look like bcrypt")
    @NullSource
    @MethodSource("notBcryptPasswords")
    void shouldSkipManagerWithInvalidPassword(String senha) {
        givenLocalUnit(5);
        givenGestores(gestor(1, "a@b.com", senha, "11999998888", 5));

        SyncReport report = service.syncAll();

        assertThat(report).isEqualTo(new SyncReport(0, 1));
        verifyNoSavesExcept(false, false, false, false);
    }

    static Stream<String> notBcryptPasswords() {
        return Stream.of(
                "plaintext-password",
                "",
                "$2a$10$" + "a".repeat(52), // 59 chars
                "$2a$10$" + "a".repeat(54), // 61 chars
                "x".repeat(60), // right length, wrong prefix
                "$1a$10$" + "a".repeat(53) // right length, wrong prefix
        );
    }

    @Test
    @DisplayName("Should skip a manager without a legacy unit code")
    void shouldSkipManagerWithNullUnit() {
        givenGestores(gestor(1, "a@b.com", HASH, "11999998888", null));

        SyncReport report = service.syncAll();

        assertThat(report).isEqualTo(new SyncReport(0, 1));
        verifyNoSavesExcept(false, false, false, false);
    }

    @Test
    @DisplayName("Should skip a manager whose parent unit does not exist locally")
    void shouldSkipManagerWhenParentUnitMissing() {
        when(unitRepository.findById(LegacyIds.unit(5))).thenReturn(Optional.empty());
        givenGestores(gestor(1, "a@b.com", HASH, "11999998888", 5));

        SyncReport report = service.syncAll();

        assertThat(report).isEqualTo(new SyncReport(0, 1));
        verifyNoSavesExcept(false, false, false, false);
    }

    @Test
    @DisplayName("Should skip a manager whose email belongs to a different user")
    void shouldSkipManagerOnEmailConflictWithAnotherUser() {
        givenLocalUnit(5);
        when(userRepository.findByEmail(new Email("a@b.com")))
                .thenReturn(Optional.of(localManager(99, "a@b.com", Status.ACTIVE)));
        givenGestores(gestor(1, "a@b.com", HASH, "11999998888", 5));

        SyncReport report = service.syncAll();

        assertThat(report).isEqualTo(new SyncReport(0, 1));
        verifyNoSavesExcept(false, false, false, false);
    }

    @Test
    @DisplayName("Should not treat the same user re-syncing with the same email as a conflict")
    void shouldNotConflictWhenSameUserResyncs() {
        givenLocalUnit(5);
        when(userRepository.findByEmail(new Email("a@b.com")))
                .thenReturn(Optional.of(localManager(1, "a@b.com", Status.ACTIVE)));
        givenGestores(gestor(1, "a@b.com", HASH, null, 5));

        SyncReport report = service.syncAll();

        assertThat(capturedUser().getUserId()).isEqualTo(LegacyIds.manager(1));
        assertThat(report).isEqualTo(new SyncReport(1, 0));
    }

    @ParameterizedTest(name = "email \"{0}\" is skipped")
    @DisplayName("Should skip a manager with a null or invalid email")
    @NullSource
    @ValueSource(strings = { "", "not-an-email", "a@b" })
    void shouldSkipManagerWithInvalidEmail(String email) {
        givenLocalUnit(5);
        givenGestores(gestor(1, email, HASH, "11999998888", 5));

        SyncReport report = service.syncAll();

        assertThat(report).isEqualTo(new SyncReport(0, 1));
        verifyNoSavesExcept(false, false, false, false);
    }

    @ParameterizedTest(name = "existing status {0} is preserved")
    @DisplayName("Should keep the status of a manager that already exists")
    @EnumSource(value = Status.class, names = { "INACTIVE", "SUSPENDED", "ACTIVE" })
    void shouldPreserveManagerStatusWhenExisting(Status existingStatus) {
        givenLocalUnit(5);
        when(userRepository.findById(LegacyIds.manager(1)))
                .thenReturn(Optional.of(localManager(1, "old@b.com", existingStatus)));
        givenGestores(gestor(1, "a@b.com", HASH, null, 5));

        SyncReport report = service.syncAll();

        User saved = capturedUser();
        assertThat(saved.getUserId()).isEqualTo(LegacyIds.manager(1));
        assertThat(saved.getStatus()).isEqualTo(existingStatus);
        assertThat(report).isEqualTo(new SyncReport(1, 0));
    }

    @Test
    @DisplayName("Should keep processing managers after an invalid one")
    void shouldProcessRemainingManagersAfterInvalidRow() {
        givenLocalUnit(5);
        givenGestores(
                gestor(1, "a@b.com", "plaintext", null, 5),
                validGestor(2));

        SyncReport report = service.syncAll();

        assertThat(capturedUser().getUserId()).isEqualTo(LegacyIds.manager(2));
        assertThat(report).isEqualTo(new SyncReport(1, 1));
    }

    // ------------------------------------------------------------------
    // telephones
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Should create the manager telephone with a deterministic id")
    void shouldCreateTelephoneWithDeterministicId() {
        givenLocalUnit(5);
        givenGestores(new Gestor(12, "Maria", "a@b.com", HASH, "11999998888", 5, CREATED, UPDATED));

        SyncReport report = service.syncAll();

        Telephone saved = capturedTelephone();
        assertThat(saved.getTelephoneId()).isEqualTo(LegacyIds.managerTelephone(12));
        assertThat(saved.getNumber()).isEqualTo(new TelephoneNumber("11999998888"));
        assertThat(saved.getUserId()).isEqualTo(LegacyIds.manager(12));
        assertThat(saved.getOrganizationId()).isEqualTo(LegacyIds.organization(1));
        assertThat(saved.getUnitId()).isEqualTo(LegacyIds.unit(5));
        assertThat(saved.getCreatedAt()).isEqualTo(expected(CREATED));
        assertThat(saved.getUpdatedAt()).isEqualTo(expected(UPDATED));
        assertThat(report).isEqualTo(new SyncReport(2, 0));
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should strip formatting characters from the legacy phone number")
    void shouldNormalizeFormattedPhone() {
        givenLocalUnit(5);
        givenGestores(gestor(1, "a@b.com", HASH, "(11) 99999-8888", 5));

        SyncReport report = service.syncAll();

        assertThat(capturedTelephone().getNumber().value()).isEqualTo("11999998888");
        assertThat(report).isEqualTo(new SyncReport(2, 0));
    }

    @Test
    @DisplayName("Should reuse the id and createdAt of the telephone the manager already has")
    void shouldReuseExistingTelephoneIdAndCreatedAt() {
        UUID existingId = UUID.randomUUID();
        LocalDateTime existingCreatedAt = LocalDateTime.of(2020, 6, 1, 8, 0, 0);
        givenLocalUnit(5);
        when(telephoneRepository.findByUserId(LegacyIds.manager(1)))
                .thenReturn(Optional.of(new Telephone(existingId, new TelephoneNumber("1133334444"),
                        LegacyIds.manager(1), null, null, existingCreatedAt, existingCreatedAt)));
        givenGestores(gestor(1, "a@b.com", HASH, "11999998888", 5));

        SyncReport report = service.syncAll();

        Telephone saved = capturedTelephone();
        assertThat(saved.getTelephoneId()).isEqualTo(existingId);
        assertThat(saved.getTelephoneId()).isNotEqualTo(LegacyIds.managerTelephone(1));
        assertThat(saved.getCreatedAt()).isEqualTo(existingCreatedAt);
        assertThat(saved.getUpdatedAt()).isEqualTo(expected(UPDATED));
        assertThat(saved.getNumber()).isEqualTo(new TelephoneNumber("11999998888"));
        assertThat(saved.getUserId()).isEqualTo(LegacyIds.manager(1));
        // o telefone antigo tinha organizacao/unidade nulas: o re-sync as preenche
        assertThat(saved.getOrganizationId()).isEqualTo(LegacyIds.organization(1));
        assertThat(saved.getUnitId()).isEqualTo(LegacyIds.unit(5));
        assertThat(report).isEqualTo(new SyncReport(2, 0));
    }

    @ParameterizedTest(name = "telefone \"{0}\" is ignored")
    @DisplayName("Should ignore a null or blank phone without counting it as skipped")
    @NullSource
    @ValueSource(strings = { "", "   " })
    void shouldIgnoreNullOrBlankPhone(String telefone) {
        givenLocalUnit(5);
        givenGestores(gestor(1, "a@b.com", HASH, telefone, 5));

        SyncReport report = service.syncAll();

        verify(userRepository).save(any(User.class));
        verify(telephoneRepository, never()).save(any());
        assertThat(report).isEqualTo(new SyncReport(1, 0));
    }

    @ParameterizedTest(name = "telefone \"{0}\" is invalid")
    @DisplayName("Should count an invalid phone as skipped while keeping the saved manager")
    @ValueSource(strings = { "123", "123456789", "123456789012", "abcdefghij" })
    void shouldSkipInvalidPhoneButKeepManager(String telefone) {
        givenLocalUnit(5);
        givenGestores(gestor(1, "a@b.com", HASH, telefone, 5));

        SyncReport report = service.syncAll();

        assertThat(capturedUser().getUserId()).isEqualTo(LegacyIds.manager(1));
        verify(telephoneRepository, never()).save(any());
        assertThat(report).isEqualTo(new SyncReport(1, 1));
    }

    @Test
    @DisplayName("Should not touch the telephone when the manager was skipped")
    void shouldNotSaveTelephoneWhenManagerSkipped() {
        givenLocalUnit(5);
        givenGestores(gestor(1, "a@b.com", "plaintext", "11999998888", 5));

        SyncReport report = service.syncAll();

        verify(userRepository, never()).save(any());
        verify(telephoneRepository, never()).save(any());
        assertThat(report).isEqualTo(new SyncReport(0, 1));
    }

    // ------------------------------------------------------------------
    // resilience: a failing row never stops the run
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Should continue with the next organization when a save throws")
    void shouldContinueAfterOrganizationSaveThrows() {
        doThrow(new RuntimeException("boom")).when(organizationRepository)
                .save(argThat(o -> o.getOrganizationId().equals(LegacyIds.organization(1))));
        givenOrganizacoes(validOrganizacao(1), validOrganizacao(2));

        SyncReport report = service.syncAll();

        verify(organizationRepository).save(argThat(o -> o.getOrganizationId().equals(LegacyIds.organization(2))));
        assertThat(report).isEqualTo(new SyncReport(1, 1));
    }

    @Test
    @DisplayName("Should continue with the next unit when a save throws")
    void shouldContinueAfterUnitSaveThrows() {
        givenLocalOrganization(1);
        doThrow(new RuntimeException("boom")).when(unitRepository)
                .save(argThat(u -> u.getUnitId().equals(LegacyIds.unit(1))));
        givenUnidades(unidade(1, 1), unidade(2, 1));

        SyncReport report = service.syncAll();

        verify(unitRepository).save(argThat(u -> u.getUnitId().equals(LegacyIds.unit(2))));
        assertThat(report).isEqualTo(new SyncReport(1, 1));
    }

    @Test
    @DisplayName("Should continue with the next manager when a save throws and skip the failed manager's phone")
    void shouldContinueAfterManagerSaveThrows() {
        givenLocalUnit(5);
        doThrow(new RuntimeException("boom")).when(userRepository)
                .save(argThat(u -> u.getUserId().equals(LegacyIds.manager(1))));
        givenGestores(
                gestor(1, "g1@b.com", HASH, "11999998888", 5),
                gestor(2, "g2@b.com", HASH, "11988887777", 5));

        SyncReport report = service.syncAll();

        verify(userRepository).save(argThat(u -> u.getUserId().equals(LegacyIds.manager(2))));
        // only the second manager's telephone is saved; the first manager never got saved
        Telephone telephone = capturedTelephone();
        assertThat(telephone.getUserId()).isEqualTo(LegacyIds.manager(2));
        // manager 2 + telephone 2 saved; manager 1 skipped
        assertThat(report).isEqualTo(new SyncReport(2, 1));
    }

    @Test
    @DisplayName("Should keep the manager and count one skip when the telephone save throws")
    void shouldContinueAfterTelephoneSaveThrows() {
        givenLocalUnit(5);
        doThrow(new RuntimeException("boom")).when(telephoneRepository).save(any(Telephone.class));
        givenGestores(gestor(1, "a@b.com", HASH, "11999998888", 5));

        SyncReport report = service.syncAll();

        assertThat(capturedUser().getUserId()).isEqualTo(LegacyIds.manager(1));
        assertThat(report).isEqualTo(new SyncReport(1, 1));
    }

    @Test
    @DisplayName("Should keep syncing units and managers even when every organization fails")
    void shouldRunLaterStagesWhenEarlierRowsFail() {
        doThrow(new RuntimeException("boom")).when(organizationRepository).save(any(Organization.class));
        givenLocalOrganization(1);
        givenLocalUnit(5);
        givenReader(List.of(validOrganizacao(1)), List.of(unidade(5, 1)), List.of(validGestor(1)));

        SyncReport report = service.syncAll();

        verify(unitRepository).save(any(Unit.class));
        verify(userRepository).save(any(User.class));
        assertThat(report).isEqualTo(new SyncReport(2, 1));
    }

    // ------------------------------------------------------------------
    // timestamps
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Should fall back to the current time when legacy timestamps are null")
    void shouldFallBackToNowWhenTimestampsAreNull() {
        givenLocalOrganization(1);
        givenLocalUnit(5);
        givenReader(
                List.of(new Organizacao(1, CNPJ, "Acme", "a@b.com", null, null, "Grátis", "Ativa")),
                List.of(new Unidade(5, 1, null, null)),
                List.of(new Gestor(1, "Maria", "g@b.com", HASH, "11999998888", 5, null, null)));

        SyncReport report = service.syncAll();

        Organization organization = capturedOrganization();
        assertThat(organization.getCreatedAt()).isNotNull();
        assertThat(organization.getUpdatedAt()).isNotNull();
        Unit unit = capturedUnit();
        assertThat(unit.getCreatedAt()).isNotNull();
        assertThat(unit.getUpdatedAt()).isNotNull();
        User user = capturedUser();
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isNotNull();
        Telephone telephone = capturedTelephone();
        assertThat(telephone.getCreatedAt()).isNotNull();
        assertThat(telephone.getUpdatedAt()).isNotNull();
        assertThat(report).isEqualTo(new SyncReport(4, 0));
    }

    @Test
    @DisplayName("Should truncate legacy timestamps to seconds and convert them to the system zone")
    void shouldTruncateAndConvertTimestamps() {
        givenOrganizacoes(validOrganizacao(1));

        service.syncAll();

        Organization saved = capturedOrganization();
        assertThat(saved.getCreatedAt().getNano()).isZero();
        assertThat(saved.getUpdatedAt().getNano()).isZero();
        assertThat(saved.getCreatedAt()).isEqualTo(
                LocalDateTime.ofInstant(CREATED.toInstant(), ZoneId.systemDefault()).withNano(0));
        assertThat(saved.getUpdatedAt()).isEqualTo(
                LocalDateTime.ofInstant(UPDATED.toInstant(), ZoneId.systemDefault()).withNano(0));
    }

    // ------------------------------------------------------------------
    // report, ordering, idempotency
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Should report saved and skipped counts on a mixed run")
    void shouldReportSavedAndSkippedOnMixedRun() {
        lenient().when(organizationRepository.findById(LegacyIds.organization(1)))
                .thenReturn(Optional.of(localOrganization(1)));
        lenient().when(unitRepository.findById(LegacyIds.unit(10)))
                .thenReturn(Optional.of(localUnit(10, "Existing unit")));
        givenReader(
                List.of(
                        validOrganizacao(1), // saved
                        organizacao(2, "11222333000182", "b@b.com", "Grátis", "Ativa")), // skipped: bad cnpj
                List.of(
                        unidade(10, 1), // saved
                        unidade(11, null)), // skipped: no organization
                List.of(
                        gestor(20, "g20@b.com", HASH, "11999998888", 10), // manager + telephone saved
                        gestor(21, "g21@b.com", "plaintext", "11999998888", 10))); // skipped: plaintext password

        SyncReport report = service.syncAll();

        assertThat(report).isEqualTo(new SyncReport(4, 3));
        verify(organizationRepository, times(1)).save(any(Organization.class));
        verify(unitRepository, times(1)).save(any(Unit.class));
        verify(userRepository, times(1)).save(any(User.class));
        verify(telephoneRepository, times(1)).save(any(Telephone.class));
    }

    @Test
    @DisplayName("Should return an empty report when the legacy database has no rows")
    void shouldReturnEmptyReportWhenNothingToSync() {
        givenReader(List.of(), List.of(), List.of());

        SyncReport report = service.syncAll();

        assertThat(report).isEqualTo(new SyncReport(0, 0));
        verifyNoSavesExcept(false, false, false, false);
    }

    @Test
    @DisplayName("Should sync organizations before units and units before managers")
    void shouldSyncInDependencyOrder() {
        lenient().when(organizationRepository.findById(LegacyIds.organization(1)))
                .thenReturn(Optional.of(localOrganization(1)));
        lenient().when(unitRepository.findById(LegacyIds.unit(5)))
                .thenReturn(Optional.of(localUnit(5, "Existing unit")));
        givenReader(List.of(validOrganizacao(1)), List.of(unidade(5, 1)), List.of(validGestor(1)));

        service.syncAll();

        InOrder inOrder = inOrder(organizationRepository, unitRepository, userRepository);
        inOrder.verify(organizationRepository).save(any(Organization.class));
        inOrder.verify(unitRepository).save(any(Unit.class));
        inOrder.verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should save each manager's telephone right after that manager")
    void shouldSaveTelephoneRightAfterEachManager() {
        givenLocalUnit(5);
        givenGestores(
                gestor(1, "g1@b.com", HASH, "11999998888", 5),
                gestor(2, "g2@b.com", HASH, "11988887777", 5));

        SyncReport report = service.syncAll();

        InOrder inOrder = inOrder(userRepository, telephoneRepository);
        inOrder.verify(userRepository).save(argThat(u -> u.getUserId().equals(LegacyIds.manager(1))));
        inOrder.verify(telephoneRepository).save(argThat(t -> t.getUserId().equals(LegacyIds.manager(1))));
        inOrder.verify(userRepository).save(argThat(u -> u.getUserId().equals(LegacyIds.manager(2))));
        inOrder.verify(telephoneRepository).save(argThat(t -> t.getUserId().equals(LegacyIds.manager(2))));
        assertThat(report).isEqualTo(new SyncReport(4, 0));
    }

    @Test
    @DisplayName("Should save the same ids when run twice over the same legacy data")
    void shouldBeIdempotent() {
        lenient().when(organizationRepository.findById(LegacyIds.organization(1)))
                .thenReturn(Optional.of(localOrganization(1)));
        lenient().when(unitRepository.findById(LegacyIds.unit(5)))
                .thenReturn(Optional.of(localUnit(5, "Existing unit")));
        givenReader(
                List.of(validOrganizacao(1)),
                List.of(unidade(5, 1)),
                List.of(gestor(1, "g1@b.com", HASH, "11999998888", 5)));

        SyncReport first = service.syncAll();
        SyncReport second = service.syncAll();

        assertThat(second).isEqualTo(first);

        ArgumentCaptor<Organization> organizations = ArgumentCaptor.forClass(Organization.class);
        verify(organizationRepository, times(2)).save(organizations.capture());
        assertThat(organizations.getAllValues().get(0).getOrganizationId())
                .isEqualTo(organizations.getAllValues().get(1).getOrganizationId());

        ArgumentCaptor<Unit> units = ArgumentCaptor.forClass(Unit.class);
        verify(unitRepository, times(2)).save(units.capture());
        assertThat(units.getAllValues().get(0).getUnitId())
                .isEqualTo(units.getAllValues().get(1).getUnitId());

        ArgumentCaptor<User> users = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(2)).save(users.capture());
        assertThat(users.getAllValues().get(0).getUserId())
                .isEqualTo(users.getAllValues().get(1).getUserId());

        ArgumentCaptor<Telephone> telephones = ArgumentCaptor.forClass(Telephone.class);
        verify(telephoneRepository, times(2)).save(telephones.capture());
        assertThat(telephones.getAllValues().get(0).getTelephoneId())
                .isEqualTo(telephones.getAllValues().get(1).getTelephoneId());
    }
}
