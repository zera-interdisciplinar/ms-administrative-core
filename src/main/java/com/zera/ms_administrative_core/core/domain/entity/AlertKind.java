package com.zera.ms_administrative_core.core.domain.entity;

/**
 * Tipo do alerta ou da notificacao. {@code STORAGE} e {@code TIME} sao os tipos originais e
 * continuam aceitos pelos alertas ja gravados; os demais vieram com a v1 do ms-inventory, onde
 * cada um corresponde a uma regra configurada pela unidade.
 */
public enum AlertKind {
    STORAGE,
    TIME,

    /** Garantia do item chegando ao fim. */
    WARRANTY_EXPIRATION,
    /** Vida util esperada do item chegando ao fim. */
    LIFESPAN_EXPIRATION,
    /** Intensidade de uso do item acima do limite configurado. */
    USAGE_INTENSITY_LIMIT,
    /** Ocupacao do estoque da unidade acima do limite configurado. */
    STOCK_QUANTITY_LIMIT,
    /** Item ha tempo demais em estoque. */
    TIME_IN_STOCK_LIMIT,
    /** Item sem movimentacao ha tempo demais. */
    STALE_ITEM,
    /** Material reciclavel enviado ao aterro no descarte. */
    RECYCLABLE_TO_LANDFILL,
    /** Data prevista de quebra se aproximando, calculada pelo sistema preditivo. */
    PREDICTED_FAILURE,

    /** Cadastro de item aprovado pelo gestor; vai para quem cadastrou. */
    ITEM_APPROVED,
    /** Cadastro de item reprovado pelo gestor, com o motivo na descricao. */
    ITEM_REJECTED
}
