package com.zera.ms_administrative_core.core.domain.entity;

import com.zera.ms_administrative_core.core.domain.valueobject.Cep;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AddressTest {

    private final UUID addressId = UUID.randomUUID();
    private final UUID unitId = UUID.randomUUID();
    private final UUID recyclingBusinessId = UUID.randomUUID();
    private final Cep cep = new Cep("01001-000");

    @Test
    @DisplayName("Should create address with automatic timestamps")
    void shouldCreateWithAutomaticTimestamps() {
        Address address = new Address(addressId, "São Paulo", "SP", "Sé", cep, "100", "Sala 1",
                unitId, recyclingBusinessId);

        assertEquals(addressId, address.getAddressId());
        assertEquals("São Paulo", address.getCity());
        assertEquals("SP", address.getState());
        assertEquals("Sé", address.getNeighborhood());
        assertEquals(cep, address.getCep());
        assertEquals("100", address.getNumber());
        assertEquals("Sala 1", address.getComplement());
        assertEquals(unitId, address.getUnitId());
        assertEquals(recyclingBusinessId, address.getRecyclingBusinessId());
        assertNotNull(address.getCreatedAt());
        assertNotNull(address.getUpdatedAt());
    }

    @Test
    @DisplayName("Should preserve dates when using reconstitution constructor")
    void shouldPreserveDatesOnReconstitution() {
        LocalDateTime createdAt = LocalDateTime.of(2024, 1, 1, 10, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2024, 6, 1, 10, 0);

        Address address = new Address(addressId, "São Paulo", "SP", "Sé", cep, "100", "Sala 1",
                unitId, recyclingBusinessId, createdAt, updatedAt);

        assertEquals(createdAt, address.getCreatedAt());
        assertEquals(updatedAt, address.getUpdatedAt());
    }

    @Test
    @DisplayName("Should update updatedAt when calling touch")
    void shouldTouch() {
        Address address = new Address(addressId, "São Paulo", "SP", "Sé", cep, "100", "Sala 1",
                unitId, recyclingBusinessId,
                LocalDateTime.of(2024, 1, 1, 10, 0), LocalDateTime.of(2024, 1, 1, 10, 0));
        LocalDateTime before = address.getUpdatedAt();

        address.touch();

        assertFalse(address.getUpdatedAt().isBefore(before));
    }
}
