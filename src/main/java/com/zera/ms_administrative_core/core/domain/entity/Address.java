package com.zera.ms_administrative_core.core.domain.entity;

import com.zera.ms_administrative_core.core.domain.valueobject.Cep;

import java.time.LocalDateTime;
import java.util.UUID;

public class Address {
    private UUID addressId;
    private String city;
    private String state;
    private String neighborhood;
    private Cep cep;
    private String number;
    private String complement;
    private UUID unitId;
    private UUID recyclingBusinessId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Address(UUID addressId, String city, String state, String neighborhood, Cep cep, String number, String complement, UUID unitId, UUID recyclingBusinessId, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.addressId = addressId;
        this.city = city;
        this.state = state;
        this.neighborhood = neighborhood;
        this.cep = cep;
        this.number = number;
        this.complement = complement;
        this.unitId = unitId;
        this.recyclingBusinessId = recyclingBusinessId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Address(UUID addressId, String city, String state, String neighborhood, Cep cep, String number, String complement, UUID unitId, UUID recyclingBusinessId) {
        this.addressId = addressId;
        this.city = city;
        this.state = state;
        this.neighborhood = neighborhood;
        this.cep = cep;
        this.number = number;
        this.complement = complement;
        this.unitId = unitId;
        this.recyclingBusinessId = recyclingBusinessId;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getAddressId() { return addressId; }

    public String getCity() { return city; }

    public String getState() { return state; }

    public String getNeighborhood() { return neighborhood; }

    public Cep getCep() { return cep; }

    public String getNumber() { return number; }

    public String getComplement() { return complement; }

    public UUID getUnitId() { return unitId; }

    public UUID getRecyclingBusinessId() { return recyclingBusinessId; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}
