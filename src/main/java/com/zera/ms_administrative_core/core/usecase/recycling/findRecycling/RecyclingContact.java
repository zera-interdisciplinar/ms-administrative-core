package com.zera.ms_administrative_core.core.usecase.recycling.findRecycling;

import java.util.UUID;

/** Contato da ficha vinculada a um placeId. Telefone pode nao existir. */
public record RecyclingContact(UUID recyclingBusinessId, String name, String email, String phone) {
}
