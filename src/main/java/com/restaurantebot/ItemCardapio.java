package com.restaurantebot;

import java.util.Locale;

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

    public String getValorFormatado() {
        return String.format(PT_BR, "R$ %.2f", valor);
    }
}
