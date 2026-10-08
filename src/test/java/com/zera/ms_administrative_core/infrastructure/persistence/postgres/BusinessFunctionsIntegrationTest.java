package com.zera.ms_administrative_core.infrastructure.persistence.postgres;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import com.zera.ms_administrative_core.core.repository.AnalyticsRepository;

/** Prova as functions de regra de negocio da V13. */
@Transactional
class BusinessFunctionsIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private AnalyticsRepository analytics;

    // ------------------------------------------------------------------ fn_validar_cnpj

    @ParameterizedTest
    @CsvSource({
            "11.222.333/0001-81, true",   // valido, com mascara
            "11222333000181,     true",   // valido, sem mascara
            "11222333000182,     false",  // segundo DV errado
            "11222333000191,     false",  // primeiro DV errado
            "11111111111111,     false",  // digitos repetidos passam no modulo 11 por acidente
            "00000000000000,     false",
            "1122233300018,      false",  // 13 digitos
            "112223330001811,    false",  // 15 digitos
            "'',                 false"
    })
    @DisplayName("fn_validar_cnpj deve validar digitos verificadores")
    void shouldValidateCnpj(String entrada, boolean esperado) {
        Boolean resultado = jdbc.queryForObject("SELECT fn_validar_cnpj(?)", Boolean.class, entrada);

        assertThat(resultado).isEqualTo(esperado);
    }

    @Test
    @DisplayName("fn_validar_cnpj deve tratar nulo como invalido, nao estourar")
    void shouldTreatNullCnpjAsInvalid() {
        assertThat(jdbc.queryForObject("SELECT fn_validar_cnpj(NULL)", Boolean.class)).isFalse();
    }

    /** Os fixtures dos testes geram CNPJ valido; se isso deixar de valer, este teste avisa. */
    @Test
    @DisplayName("Os CNPJs sinteticos dos fixtures devem ser validos")
    void fixtureCnpjShouldBeValid() {
        assertThat(jdbc.queryForObject("SELECT fn_validar_cnpj(?)", Boolean.class, cnpjSintetico()))
                .isTrue();
    }

    // -------------------------------------------------------------- fn_tamanho_equipe

    /**
     * Uma equipe de 3 niveis e o minimo para provar que a funcao RECORRE: contando so subordinados
     * diretos, o gestor raiz daria 1, nao 3.
     */
    @Test
    @DisplayName("fn_tamanho_equipe deve contar a subarvore inteira, nao so os diretos")
    void shouldCountWholeSubtree() {
        Fixture f = novaHierarquia();
        UUID coordenador = UUID.randomUUID();
        UUID func1 = UUID.randomUUID();
        UUID func2 = UUID.randomUUID();
        inserirUsuario(coordenador, f.unidadeId(), f.gestorId(), "MANAGER", "ACTIVE");
        inserirUsuario(func1, f.unidadeId(), coordenador, "EMPLOYEE", "ACTIVE");
        inserirUsuario(func2, f.unidadeId(), coordenador, "EMPLOYEE", "ACTIVE");

        assertThat(tamanhoEquipe(f.gestorId())).isEqualTo(3);
        assertThat(tamanhoEquipe(coordenador)).isEqualTo(2);
        assertThat(tamanhoEquipe(func1)).isZero();
    }

    /**
     * `manager_id` e auto-referencia sem protecao anti-ciclo no schema, entao A->B->A e fisicamente
     * possivel. Sem o controle de caminho na CTE, isto seria recursao infinita -- o servidor, nao o
     * teste, e quem cairia.
     */
    @Test
    @DisplayName("fn_tamanho_equipe deve sobreviver a um ciclo em manager_id")
    void shouldSurviveCycle() {
        Fixture f = novaHierarquia();
        UUID b = UUID.randomUUID();
        inserirUsuario(b, f.unidadeId(), f.gestorId(), "MANAGER", "ACTIVE");
        // fecha o ciclo: gestor passa a ser subordinado do proprio subordinado
        jdbc.update("UPDATE user_account SET manager_id = ? WHERE id = ?", b, f.gestorId());

        assertThat(tamanhoEquipe(f.gestorId())).isNotNull().isLessThanOrEqualTo(2);
    }

    // ---------------------------------------------------- fn_indice_saude_unidade

    @Test
    @DisplayName("Unidade sem alerta deve ter saude 100")
    void shouldScorePerfectWithoutAlerts() {
        Fixture f = novaHierarquia();

        assertThat(saude(f.unidadeId())).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("Alerta HIGH deve penalizar mais que alerta LOW")
    void shouldWeighSeverity() {
        Fixture baixa = novaHierarquia();
        Fixture alta = novaHierarquia();
        LocalDateTime ontem = LocalDateTime.now().minusDays(1).withNano(0);

        inserirAlerta(baixa.unidadeId(), baixa.gestorId(), "LOW", "OPEN", ontem);
        inserirAlerta(alta.unidadeId(), alta.gestorId(), "HIGH", "OPEN", ontem);

        assertThat(saude(alta.unidadeId())).isLessThan(saude(baixa.unidadeId()));
    }

    /**
     * A escala e logaritmica justamente para nao saturar: num volume alto a nota precisa continuar
     * DIFERENCIANDO "ruim" de "catastrofico", e nao colapsar tudo em zero.
     */
    @Test
    @DisplayName("Indice deve continuar discriminando em volume alto, sem saturar em zero")
    void shouldNotSaturateAtHighVolume() {
        Fixture muitos = novaHierarquia();
        Fixture muitissimos = novaHierarquia();
        LocalDateTime ontem = LocalDateTime.now().minusDays(1).withNano(0);

        for (int i = 0; i < 20; i++) {
            inserirAlerta(muitos.unidadeId(), muitos.gestorId(), "HIGH", "OPEN", ontem);
        }
        for (int i = 0; i < 200; i++) {
            inserirAlerta(muitissimos.unidadeId(), muitissimos.gestorId(), "HIGH", "OPEN", ontem);
        }

        BigDecimal notaMuitos = saude(muitos.unidadeId());
        BigDecimal notaMuitissimos = saude(muitissimos.unidadeId());

        assertThat(notaMuitissimos).isLessThan(notaMuitos);
        assertThat(notaMuitos).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Alerta fora do periodo nao deve contar")
    void shouldRespectPeriod() {
        Fixture f = novaHierarquia();
        inserirAlerta(f.unidadeId(), f.gestorId(), "HIGH", "OPEN",
                LocalDateTime.now().minusDays(200).withNano(0));

        assertThat(saude(f.unidadeId())).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("Periodo invertido deve estourar erro explicito")
    void shouldRejectInvalidPeriod() {
        Fixture f = novaHierarquia();

        assertThatThrownBy(() -> jdbc.queryForObject(
                "SELECT fn_indice_saude_unidade(?, ?, ?)", BigDecimal.class,
                f.unidadeId(), LocalDate.now(), LocalDate.now().minusDays(5)))
                .hasMessageContaining("Periodo invalido");
    }

    /**
     * V19: o CHECK de CNPJ precisa rejeitar de verdade uma escrita nova, nao so existir no catalogo.
     * Antes da V19, a funcao so era chamada pelo JUnit.
     */
    @Test
    @DisplayName("Organizacao com CNPJ invalido deve ser rejeitada pelo banco")
    void organizationRejectsInvalidCnpjOnWrite() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO organization (id, name, cnpj, status, email, plan, created_at, updated_at)
                VALUES (?, 'Invalida', '11222333000182', 'ACTIVE', ?, 'FREE', NOW(), NOW())
                """, UUID.randomUUID(), "inv." + UUID.randomUUID() + "@test.local"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    /**
     * Caminho de producao: a API consome fn_tamanho_equipe pelo AnalyticsRepository, nao por SQL
     * solto. Hierarquia de 3 niveis: o gestor raiz deve contar os 3 abaixo dele, nao so os diretos.
     */
    @Test
    @DisplayName("fn_tamanho_equipe pelo adaptador de analytics deve contar a arvore inteira")
    void teamSizeThroughAnalyticsAdapterCountsWholeTree() {
        Fixture f = novaHierarquia();
        UUID coordenador = UUID.randomUUID();
        inserirUsuario(coordenador, f.unidadeId(), f.gestorId(), "MANAGER", "ACTIVE");
        inserirUsuario(UUID.randomUUID(), f.unidadeId(), coordenador, "EMPLOYEE", "ACTIVE");
        inserirUsuario(UUID.randomUUID(), f.unidadeId(), coordenador, "EMPLOYEE", "ACTIVE");

        assertThat(analytics.teamSize(f.gestorId())).isEqualTo(3);
        assertThat(analytics.teamSize(coordenador)).isEqualTo(2);
    }

    private Integer tamanhoEquipe(UUID gestorId) {
        return jdbc.queryForObject("SELECT fn_tamanho_equipe(?)", Integer.class, gestorId);
    }

    private BigDecimal saude(UUID unidadeId) {
        return jdbc.queryForObject("SELECT fn_indice_saude_unidade(?, ?, ?)", BigDecimal.class,
                unidadeId, LocalDate.now().minusDays(90), LocalDate.now());
    }
}
