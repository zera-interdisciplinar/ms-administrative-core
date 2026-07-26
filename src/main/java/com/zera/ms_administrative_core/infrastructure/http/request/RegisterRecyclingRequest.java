package com.zera.ms_administrative_core.infrastructure.http.request;

public record RegisterRecyclingRequest(
    String name,
    String cnpj,
    String email
) {
}