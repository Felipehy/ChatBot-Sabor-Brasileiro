package com.restaurantebot;

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
