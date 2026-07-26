package com.zera.ms_administrative_core.core.domain.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.zera.ms_administrative_core.core.domain.valueobject.Cnpj;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;

public class RecyclingBusiness {
    private final UUID id;
    private String name;
    private Cnpj cnpj;
    private Email email;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public RecyclingBusiness(UUID id, String name, Cnpj cnpj, Email email) {
        this.id = id;
        this.name = name;
        this.cnpj = cnpj;
        this.email = email;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public RecyclingBusiness(UUID id, String name, Cnpj cnpj, Email email, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.cnpj = cnpj;
        this.email = email;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // getters

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Cnpj getCnpj() {
        return cnpj;
    }
    
    public Email getEmail() {
        return email;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    // ------------------------------------------

    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    public void rename(String name) {
        touch();
        this.name = name;
    }

    public void changeEmail(Email email) {
        touch();
        this.email = email;
    }    
}
