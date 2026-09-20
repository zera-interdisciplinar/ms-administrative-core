package com.zera.ms_administrative_core.infrastructure.legacysync;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class LegacyIds {

    private LegacyIds() {
    }

    public static UUID organization(int codigo) {
        return of("organizacao", codigo);
    }

    public static UUID unit(int codigo) {
        return of("unidade", codigo);
    }

    public static UUID manager(int codigo) {
        return of("gestor", codigo);
    }

    public static UUID managerTelephone(int gestorCodigo) {
        return of("telefone-gestor", gestorCodigo);
    }

    private static UUID of(String table, int codigo) {
        return UUID.nameUUIDFromBytes(("legacy:" + table + ":" + codigo).getBytes(StandardCharsets.UTF_8));
    }
}
