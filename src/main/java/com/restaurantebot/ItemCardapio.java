package com.restaurantebot;

import java.util.Locale;

/**
 * Item simples do cardápio com nome e valor — usado para os tamanhos da
 * marmita ({@link CardapioService#buscarTamanhosMarmita()}) e para as
 * bebidas ({@link CardapioService#buscarBebidas()}).
 */
public class ItemCardapio {

    private static final Locale PT_BR = new Locale("pt", "BR");

    private final String nome;
    private final double valor;

    public ItemCardapio(String nome, double valor) {
        this.nome = nome;
        this.valor = valor;
    }

    public String getNome()  { return nome; }
    public double getValor() { return valor; }

    /** Formata o valor em reais no padrão brasileiro (ex.: R$ 18,00). */
    public String getValorFormatado() {
        return String.format(PT_BR, "R$ %.2f", valor);
    }
}
