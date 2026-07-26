package com.zera.ms_administrative_core.core.domain.entity;

import com.zera.ms_administrative_core.core.domain.exception.InvalidCnpjException;
import com.zera.ms_administrative_core.core.domain.valueobject.Cnpj;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RecyclingBusinessTest {

    private RecyclingBusiness business;
    private final UUID id = UUID.randomUUID();
    private final Cnpj cnpj = new Cnpj("11.222.333/0001-81");
    private final Email email = new Email("company@email.com");

    @BeforeEach
    void setUp() {
        business = new RecyclingBusiness(id, "Test Company", cnpj, email);
    }

    // --- Construction ---

    @Test
    @DisplayName("Should create RecyclingBusiness with correct data")
    void shouldCreateWithCorrectData() {
        assertEquals(id, business.getId());
        assertEquals("Test Company", business.getName());
        assertEquals(cnpj, business.getCnpj());
        assertEquals(email, business.getEmail());
    }

    @Test
    @DisplayName("Should set createdAt and updatedAt automatically on creation")
    void shouldSetTimestampsOnCreation() {
        assertNotNull(business.getCreatedAt());
        assertNotNull(business.getUpdatedAt());
    }

    @Test
    @DisplayName("Should preserve dates when using reconstitution constructor")
    void shouldPreserveDatesOnReconstitution() {
        LocalDateTime createdAt = LocalDateTime.of(2024, 1, 1, 10, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2024, 6, 1, 10, 0);

        RecyclingBusiness reconstituted = new RecyclingBusiness(
                id, "Test Company", cnpj, email, createdAt, updatedAt);

        assertEquals(createdAt, reconstituted.getCreatedAt());
        assertEquals(updatedAt, reconstituted.getUpdatedAt());
    }

    // --- rename ---

    @Test
    @DisplayName("Should rename correctly")
    void shouldRenameCorrectly() {
        business.rename("New Name");
        assertEquals("New Name", business.getName());
    }

    @Test
    @DisplayName("Should update updatedAt when renaming")
    void shouldTouchOnRename() {
        LocalDateTime before = business.getUpdatedAt();
        business.rename("New Name");
        assertFalse(business.getUpdatedAt().isBefore(before));
    }

    // --- changeEmail ---

    @Test
    @DisplayName("Should change email correctly")
    void shouldChangeEmailCorrectly() {
        Email newEmail = new Email("new@email.com");
        business.changeEmail(newEmail);
        assertEquals(newEmail, business.getEmail());
    }

    @Test
    @DisplayName("Should update updatedAt when changing email")
    void shouldTouchOnChangeEmail() {
        LocalDateTime before = business.getUpdatedAt();
        business.changeEmail(new Email("new@email.com"));
        assertFalse(business.getUpdatedAt().isBefore(before));
    }

    // --- touch ---

    @Test
    @DisplayName("Should update updatedAt when calling touch")
    void shouldUpdateUpdatedAtOnTouch() {
        LocalDateTime before = business.getUpdatedAt();
        business.touch();
        assertFalse(business.getUpdatedAt().isBefore(before));
    }

    // --- Invalid CNPJ ---

    @Test
    @DisplayName("Should throw exception when creating with invalid CNPJ")
    void shouldThrowOnInvalidCnpj() {
        assertThrows(InvalidCnpjException.class,
                () -> new Cnpj("00.000.000/0000-00"));
    }

    @Test
    @DisplayName("Should throw exception when creating with null CNPJ")
    void shouldThrowOnNullCnpj() {
        assertThrows(NullPointerException.class,
                () -> new Cnpj(null));
    }

    // --- Invalid Email ---

    @Test
    @DisplayName("Should throw exception when creating with invalid email")
    void shouldThrowOnInvalidEmail() {
        assertThrows(IllegalArgumentException.class,
                () -> new Email("invalidemail"));
    }
}