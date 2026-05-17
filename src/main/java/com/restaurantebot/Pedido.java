package com.restaurantebot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Carrinho do cliente: lista de marmitas customizadas + bebidas + dados
 * de entrega e pagamento.
 *
 * Cada cliente (chatId) tem um {@code Pedido} próprio em
 * {@link RestauranteBot#pedidos}, garantindo que pedidos não se misturem.
 *
 * Durante a montagem, {@link #marmitaAtual} guarda a marmita em construção;
 * ela só entra na lista oficial quando {@link #adicionarMarmitaAoCarrinho()}
 * é chamado, ao final da escolha de todas as categorias.
 */
public class Pedido {

    private static final Locale PT_BR = new Locale("pt", "BR");

    private final List<Marmita> marmitas = new ArrayList<>();
    private final List<ItemCardapio> bebidas = new ArrayList<>();
    private Marmita marmitaAtual;

    private String tipoEntrega;
    private String endereco;
    private String nomeCliente;
    private String pagamento;
    private String statusPagamento;

    // ── Marmitas ──────────────────────────────────────────────────────────────
    public Marmita iniciarNovaMarmita() {
        marmitaAtual = new Marmita();
        return marmitaAtual;
    }

    public Marmita getMarmitaAtual() { return marmitaAtual; }

    public void adicionarMarmitaAoCarrinho() {
        if (marmitaAtual != null) {
            marmitas.add(marmitaAtual);
            marmitaAtual = null;
        }
    }

    public List<Marmita> getMarmitas() { return marmitas; }

    public void removerMarmita(int indice) {
        if (indice >= 0 && indice < marmitas.size()) marmitas.remove(indice);
    }

    // ── Bebidas ───────────────────────────────────────────────────────────────
    public void adicionarBebida(ItemCardapio b) { bebidas.add(b); }
    public List<ItemCardapio> getBebidas()      { return bebidas; }

    public void removerBebida(int indice) {
        if (indice >= 0 && indice < bebidas.size()) bebidas.remove(indice);
    }

    // ── Totais ────────────────────────────────────────────────────────────────
    public double calcularTotal() {
        double total = 0;
        for (Marmita m : marmitas)      total += m.calcularSubtotal();
        for (ItemCardapio b : bebidas)  total += b.getValor();
        return total;
    }

    public String getTotalFormatado() {
        return String.format(PT_BR, "R$ %.2f", calcularTotal());
    }

    public boolean vazio() {
        return marmitas.isEmpty() && bebidas.isEmpty();
    }

    // ── Dados de entrega / pagamento ──────────────────────────────────────────
    public String getTipoEntrega()             { return tipoEntrega; }
    public void   setTipoEntrega(String v)     { this.tipoEntrega = v; }

    public String getEndereco()                { return endereco; }
    public void   setEndereco(String v)        { this.endereco = v; }

    public String getNomeCliente()             { return nomeCliente; }
    public void   setNomeCliente(String v)     { this.nomeCliente = v; }

    public String getPagamento()               { return pagamento; }
    public void   setPagamento(String v)       { this.pagamento = v; }

    public String getStatusPagamento()         { return statusPagamento; }
    public void   setStatusPagamento(String v) { this.statusPagamento = v; }

    // ── Resumo para o cliente ─────────────────────────────────────────────────
    /**
     * Resumo exibido ao cliente antes de finalizar o pedido. Inclui cada
     * marmita com seu subtotal, lista de bebidas (se houver) e total geral.
     */
    public String gerarResumoCliente() {
        StringBuilder sb = new StringBuilder();
        sb.append("*Resumo do pedido:*\n\n");
        int i = 1;
        for (Marmita m : marmitas) {
            sb.append(m.resumo(i)).append("\n\n");
            i++;
        }
        if (!bebidas.isEmpty()) {
            sb.append("*Bebidas:*\n");
            for (ItemCardapio b : bebidas) {
                sb.append("- ").append(b.getNome())
                  .append(" - ").append(b.getValorFormatado()).append("\n");
            }
            sb.append("\n");
        }
        sb.append("*Valor total:* ").append(getTotalFormatado());
        return sb.toString();
    }
}
