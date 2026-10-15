package com.zera.ms_administrative_core.core.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AlertKindTest {

    @ParameterizedTest
    @EnumSource(AlertKind.class)
    @DisplayName("Todo AlertKind deve ter um titulo amigavel, nao vazio")
    void everyKindShouldHaveADisplayTitle(AlertKind kind) {
        String title = kind.displayTitle();

        assertNotNull(title);
        assertFalse(title.isBlank());
    }
}
