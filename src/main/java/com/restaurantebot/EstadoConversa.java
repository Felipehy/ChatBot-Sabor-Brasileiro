package com.restaurantebot;

/**
 * Estados do fluxo de atendimento. Cada cliente tem o seu próprio estado,
 * mantido no map {@code estados} de {@link RestauranteBot}.
 *
 * Fluxo principal de pedido (montagem de marmita):
 *   AGUARDANDO_INICIAR_MARMITA → AGUARDANDO_TAMANHO_MARMITA →
 *   AGUARDANDO_OPCAO_CATEGORIA (uma vez por categoria) →
 *   AGUARDANDO_OUTRA_MARMITA → AGUARDANDO_INICIAR_BEBIDA →
 *   AGUARDANDO_ESCOLHA_BEBIDA → AGUARDANDO_OUTRA_BEBIDA →
 *   AGUARDANDO_ACAO_RESUMO (remover/finalizar/cancelar) →
 *   AGUARDANDO_TIPO_RECEBIMENTO → fluxo de entrega ou retirada.
 *
 * Os estados de endereço, nome e pagamento ficaram intactos.
 */
public enum EstadoConversa {
    INICIO,
    AGUARDANDO_INICIAR_MARMITA,    // botões: Sim, montar marmita / Não
    AGUARDANDO_TAMANHO_MARMITA,    // botões: pequena / média / grande / cancelar
    AGUARDANDO_OPCAO_CATEGORIA,    // botões: opções da categoria + "Não quero esse item"
    AGUARDANDO_OUTRA_MARMITA,      // botões: adicionar outra marmita / continuar
    AGUARDANDO_INICIAR_BEBIDA,     // botões: adicionar bebida / continuar
    AGUARDANDO_ESCOLHA_BEBIDA,     // botões: bebidas disponíveis + "Não quero bebida"
    AGUARDANDO_OUTRA_BEBIDA,       // botões: outra bebida / continuar
    AGUARDANDO_ACAO_RESUMO,        // botões: remover / finalizar / cancelar
    AGUARDANDO_REMOCAO_ITEM,       // botões: itens removíveis + voltar
    AGUARDANDO_TIPO_RECEBIMENTO,   // botões: Entrega / Retirada
    AGUARDANDO_ENDERECO,           // texto: endereço de entrega
    AGUARDANDO_NOME_ENTREGA,       // texto: nome de quem vai receber
    AGUARDANDO_NOME_RETIRADA,      // texto: nome de quem vai retirar
    AGUARDANDO_PAGAMENTO,          // botões: Crédito / Débito / Pix
    EM_ATENDIMENTO                 // modo relay: mensagens vão ao atendente
}
