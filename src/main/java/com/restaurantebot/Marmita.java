package com.restaurantebot;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * Marmita customizada: tamanho + uma opção por categoria.
 *
 * O fluxo de montagem preenche categoria por categoria; categorias ainda não
 * decididas ficam fora do map. A próxima categoria a ser perguntada ao
 * cliente é a primeira na ordem natural do enum {@link Categoria} que ainda
 * não foi registrada — ver {@link #proximaCategoriaPendente()}.
 */
public class Marmita {

    private static final Locale PT_BR = new Locale("pt", "BR");

    private String tamanhoNome;
    private double valorBase;
    private final Map<Categoria, OpcaoCategoria> selecoes = new EnumMap<>(Categoria.class);

    public void definirTamanho(String nome, double valor) {
        this.tamanhoNome = nome;
        this.valorBase = valor;
    }

    public String getTamanhoNome() { return tamanhoNome; }
    public double getValorBase()   { return valorBase; }

    public void registrarOpcao(Categoria cat, OpcaoCategoria opcao) {
        selecoes.put(cat, opcao);
    }

    public OpcaoCategoria getOpcao(Categoria cat) {
        return selecoes.getOrDefault(cat, OpcaoCategoria.PULAR);
    }

    /** Próxima categoria que ainda não foi escolhida (ou {@code null} se todas já foram). */
    public Categoria proximaCategoriaPendente() {
        for (Categoria c : Categoria.values()) {
            if (!selecoes.containsKey(c)) return c;
        }
        return null;
    }

    /** Soma do valor base do tamanho + todos os adicionais escolhidos. */
    public double calcularSubtotal() {
        double total = valorBase;
        for (OpcaoCategoria o : selecoes.values()) {
            total += o.getValorExtra();
        }
        return total;
    }

    public String getSubtotalFormatado() {
        return String.format(PT_BR, "R$ %.2f", calcularSubtotal());
    }

    /**
     * Bloco textual de resumo para uma marmita (usado tanto pelo resumo do
     * cliente quanto pelo resumo enviado ao atendente).
     */
    public String resumo(int numero) {
        StringBuilder sb = new StringBuilder();
        sb.append("*Marmita ").append(numero).append("*\n");
        sb.append("Tamanho: ").append(tamanhoNome).append("\n");
        for (Categoria c : Categoria.values()) {
            OpcaoCategoria o = getOpcao(c);
            sb.append(c.getLabelResumo()).append(": ").append(o.labelResumo()).append("\n");
        }
        sb.append("Subtotal: ").append(getSubtotalFormatado());
        return sb.toString();
    }
}
