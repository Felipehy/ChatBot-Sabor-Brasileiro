package com.restaurantebot;

import java.util.Locale;

public class OpcaoCategoria {

    private static final Locale PT_BR = new Locale("pt", "BR");

    public static final OpcaoCategoria PULAR = new OpcaoCategoria("Não selecionado", 0.0);

    private final String nome;
    private final double valorExtra;

    public OpcaoCategoria(String nome, double valorExtra) {
        this.nome = nome;
        this.valorExtra = valorExtra;
    }

    public String getNome()       { return nome; }
    public double getValorExtra() { return valorExtra; }

    public boolean isPular() { return this == PULAR; }

    public String labelBotao() {
        if (valorExtra > 0) {
            return String.format(PT_BR, "%s (+R$ %.2f)", nome, valorExtra);
        }
        return nome;
    }

    public String labelResumo() {
        if (this == PULAR) return "Não selecionado";
        if (valorExtra > 0) {
            return String.format(PT_BR, "%s (+R$ %.2f)", nome, valorExtra);
        }
        return nome;
    }
}
