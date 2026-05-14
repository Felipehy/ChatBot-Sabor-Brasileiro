package com.restaurantebot;

public class Config {

    // ── Bot ───────────────────────────────────────────────────────────────────
    public static final String BOT_TOKEN    = "8821641828:AAGescQ-KSgyEj60Q2dtM7oPQwHo0Bu7HjI";
    public static final String BOT_USERNAME = "Sabor_Brasileiro_Bot"; // sem @

    // ── Atendente ─────────────────────────────────────────────────────────────
    // Chat ID do grupo ou canal onde os atendentes vão receber os pedidos.
    // Para descobrir o ID: adicione @RawDataBot no grupo e mande uma mensagem.
    public static final String CHAT_ID_ATENDENTE = "";

    // ── Dados do restaurante ──────────────────────────────────────────────────
    public static final String ENDERECO   = "Rua Rio Parnaiba 49, Petrolina";
    public static final String LINK_MAPS  = "https://www.google.com/maps/@-9.3688499,-40.4984852,3a,75y,86.36h,90t/data=!3m7!1e1!3m5!1sRbDoU4OJfx5eRl25KtxMbw!2e0!6shttps:%2F%2Fstreetviewpixels-pa.googleapis.com%2Fv1%2Fthumbnail%3Fcb_client%3Dmaps_sv.tactile%26w%3D900%26h%3D600%26pitch%3D0%26panoid%3DRbDoU4OJfx5eRl25KtxMbw%26yaw%3D86.36!7i16384!8i8192?entry=ttu&g_ep=EgoyMDI2MDUxMS4wIKXMDSoASAFQAw%3D%3D";

    public static final String HORARIOS =
            "🕐 *Horários de atendimento:*\n" +
            "Seg a Sex: 11h às 22h\n" +
            "Sáb: 11h às 23h\n" +
            "Dom: 12h às 21h";

    // ── Cardápio ──────────────────────────────────────────────────────────────
    // Cole aqui o link direto do Google Drive.
    // Como obter: Compartilhe a imagem como "qualquer pessoa com o link"
    // Depois substitua o link para o formato abaixo:
    // https://drive.google.com/uc?export=view&id=SEU_FILE_ID
    public static final String CARDAPIO_URL =
            "https://drive.google.com/uc?export=view&id=SEU_FILE_ID_AQUI";
}
