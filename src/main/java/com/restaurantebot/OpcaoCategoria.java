package com.restaurantebot;

import java.util.Locale;

/**
 * Opção escolhida (ou ignorada) pelo cliente dentro de uma categoria
 * da marmita.
 *
 * Carrega o nome da opção e um valor extra opcional somado ao subtotal
 * da marmita (ex.: "Frango à parmegiana" custa +R$ 2,00). A constante
 * {@link #PULAR} representa a escolha "Não quero esse item".
 */
public class OpcaoCategoria {

    private static final Locale PT_BR = new Locale("pt", "BR");

    /** Sentinela: categoria explicitamente pulada pelo cliente. */
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

    /** Texto do botão exibido ao cliente: "Ovo frito (+R$ 2,00)". */
    public String labelBotao() {
        if (valorExtra > 0) {
            return String.format(PT_BR, "%s (+R$ %.2f)", nome, valorExtra);
        }
        return nome;
    }

    /** Texto exibido na linha do resumo da marmita. */
    public String labelResumo() {
        if (this == PULAR) return "Não selecionado";
        if (valorExtra > 0) {
            return String.format(PT_BR, "%s (+R$ %.2f)", nome, valorExtra);
        }
        return nome;
    }
}
