package com.zera.ms_administrative_core.infrastructure.legacysync;

import java.time.OffsetDateTime;
import java.util.List;

public interface LegacyReader {

    List<Organizacao> organizacoes();

    List<Unidade> unidades();

    List<Gestor> gestores();

    record Organizacao(int codigo, String cnpj, String nome, String email,
            OffsetDateTime criadoEm, OffsetDateTime atualizadoEm,
            String plano, String statusAssinatura) {
    }

    record Unidade(int codigo, Integer codOrganizacao,
            OffsetDateTime criadoEm, OffsetDateTime atualizadoEm) {
    }

    record Gestor(int codigo, String nome, String email, String senha, String telefone,
            Integer codUnidade, OffsetDateTime criadoEm, OffsetDateTime atualizadoEm) {
    }
}
