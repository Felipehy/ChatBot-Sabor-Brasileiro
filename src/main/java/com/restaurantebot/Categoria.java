package com.restaurantebot;

/**
 * Categorias que compõem uma marmita customizada.
 *
 * Cada constante expõe:
 *  - {@code nome}: nome da categoria (ex.: "Carnes");
 *  - {@code labelResumo}: rótulo usado na linha do resumo do pedido
 *    (ex.: "Carne" no singular para a categoria "Carnes");
 *  - {@code prompt}: texto exibido ao cliente quando os botões da categoria
 *    são apresentados, já com a concordância correta em português.
 *
 * A ordem das constantes define a ordem em que as categorias são
 * apresentadas ao cliente.
 */
public enum Categoria {
    ARROZ     ("Arroz",      "Arroz",      "Escolha o arroz:"),
    FEIJAO    ("Feijão",     "Feijão",     "Escolha o feijão:"),
    MACARRAO  ("Macarrão",   "Macarrão",   "Escolha o macarrão:"),
    LEGUMES   ("Legumes",    "Legumes",    "Escolha os legumes:"),
    CARNES    ("Carnes",     "Carne",      "Escolha a carne:"),
    ADICIONAIS("Adicionais", "Adicionais", "Escolha os adicionais:");

    private final String nome;
    private final String labelResumo;
    private final String prompt;

    Categoria(String nome, String labelResumo, String prompt) {
        this.nome = nome;
        this.labelResumo = labelResumo;
        this.prompt = prompt;
    }

    public String getNome()        { return nome; }
    public String getLabelResumo() { return labelResumo; }
    public String getPrompt()      { return prompt; }
}
