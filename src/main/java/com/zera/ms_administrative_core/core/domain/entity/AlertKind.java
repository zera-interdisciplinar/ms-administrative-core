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
    ITEM_REJECTED,
    /**
     * Item (e o modelo vinculado a ele, quando tambem novo) cadastrado por um colaborador e
     * aguardando aprovacao; vai para o gestor da unidade. Aprovar/reprovar o item aprova/reprova o
     * modelo junto, entao um unico alerta cobre os dois.
     */
    ITEM_PENDING_APPROVAL;

    /**
     * Titulo amigavel para push/UI. O nome bruto do enum nao deve chegar ao usuario final --
     * o switch sem default obriga quem adicionar um valor novo a passar por aqui.
     */
    public String displayTitle() {
        return switch (this) {
            case STORAGE -> "Limite de armazenamento atingido";
            case TIME -> "Prazo se aproximando";
            case WARRANTY_EXPIRATION -> "Garantia perto de vencer";
            case LIFESPAN_EXPIRATION -> "Vida util do item perto de acabar";
            case USAGE_INTENSITY_LIMIT -> "Uso acima do limite configurado";
            case STOCK_QUANTITY_LIMIT -> "Estoque da unidade no limite";
            case TIME_IN_STOCK_LIMIT -> "Item parado no estoque ha tempo demais";
            case STALE_ITEM -> "Item sem movimentacao";
            case RECYCLABLE_TO_LANDFILL -> "Material reciclavel descartado como lixo comum";
            case PREDICTED_FAILURE -> "Quebra prevista em breve";
            case ITEM_APPROVED -> "Seu item foi aprovado";
            case ITEM_REJECTED -> "Seu item foi reprovado";
            case ITEM_PENDING_APPROVAL -> "Item aguardando sua aprovacao";
        };
    }
}
