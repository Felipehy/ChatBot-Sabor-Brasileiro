package com.restaurantebot;

public enum EstadoConversa {
    INICIO,
    AGUARDANDO_PRATO,
    AGUARDANDO_TIPO_ENTREGA,   // botões: Entrega / Retirada
    AGUARDANDO_ENDERECO,       // digita endereço (só entrega)
    AGUARDANDO_NOME_ENTREGA,   // digita nome (entrega)
    AGUARDANDO_NOME_RETIRADA,  // digita nome (retirada)
    AGUARDANDO_PAGAMENTO,      // botões: Dinheiro / Pix / Crédito / Débito
    AGUARDANDO_MAIS_PEDIDO     // botões: Sim / Não
}
