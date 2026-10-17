package com.zera.ms_administrative_core.infrastructure.legacysync;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public interface LegacyWriter {

    /**
     * Atualiza so as colunas de {@code changes} que nao sao nulas e grava {@code newUpdatedAt} em
     * {@code atualizado_em}. A escrita e condicional: so vale se {@code atualizado_em} ainda for o
     * {@code expectedUpdatedAt} que a rodada leu, para nunca sobrescrever uma edicao mais nova feita
     * no proprio legado entre a leitura e a escrita.
     *
     * @return {@code true} se a linha foi alterada; {@code false} se o legado mudou desde a leitura
     */
    boolean updateGestor(int codigo, GestorChanges changes, OffsetDateTime newUpdatedAt,
            OffsetDateTime expectedUpdatedAt);

    /** Valor nulo significa "nao mexer nessa coluna". */
    record GestorChanges(String nome, String email, String senha, String telefone) {

        public boolean isEmpty() {
            return nome == null && email == null && senha == null && telefone == null;
        }

        /** Nomes das colunas alteradas, sem os valores: seguro para log (nao carrega e-mail nem hash). */
        public List<String> columns() {
            List<String> columns = new ArrayList<>();
            if (nome != null) {
                columns.add("nome");
            }
            if (email != null) {
                columns.add("email");
            }
            if (senha != null) {
                columns.add("senha");
            }
            if (telefone != null) {
                columns.add("telefone");
            }
            return columns;
        }
    }
}
