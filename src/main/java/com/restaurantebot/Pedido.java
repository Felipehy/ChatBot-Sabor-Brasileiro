package com.restaurantebot;

import java.util.ArrayList;
import java.util.List;

public class Pedido {

    private final List<String> pratos = new ArrayList<>();
    private String tipoEntrega;   // "ENTREGA" ou "RETIRADA"
    private String endereco;
    private String nomeCliente;
    private String pagamento;

    // ── Pratos ────────────────────────────────────────────────────────────────
    public void adicionarPrato(String prato) { pratos.add(prato); }
    public List<String> getPratos() { return pratos; }

    // ── Getters / Setters ─────────────────────────────────────────────────────
    public String getTipoEntrega()              { return tipoEntrega; }
    public void   setTipoEntrega(String v)      { this.tipoEntrega = v; }

    public String getEndereco()                 { return endereco; }
    public void   setEndereco(String v)         { this.endereco = v; }

    public String getNomeCliente()              { return nomeCliente; }
    public void   setNomeCliente(String v)      { this.nomeCliente = v; }

    public String getPagamento()                { return pagamento; }
    public void   setPagamento(String v)        { this.pagamento = v; }

    // ── Resumo formatado ──────────────────────────────────────────────────────
    public String resumo() {
        StringBuilder sb = new StringBuilder();
        sb.append("🧾 *Resumo do pedido:*\n\n");

        sb.append("🍽️ *Pratos:*\n");
        for (int i = 0; i < pratos.size(); i++) {
            sb.append("  ").append(i + 1).append(". ").append(pratos.get(i)).append("\n");
        }

        if ("ENTREGA".equals(tipoEntrega)) {
            sb.append("\n🚚 *Entrega em:* ").append(endereco).append("\n");
            sb.append("👤 *Cliente:* ").append(nomeCliente).append("\n");
        } else {
            sb.append("\n🏠 *Retirada por:* ").append(nomeCliente).append("\n");
        }

        sb.append("💳 *Pagamento:* ").append(pagamento);
        return sb.toString();
    }
}
