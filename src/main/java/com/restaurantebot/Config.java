package com.restaurantebot;

public class Config {

    // ── Bot ───────────────────────────────────────────────────────────────────
    public static final String BOT_TOKEN = "";
    public static final String BOT_USERNAME = ""; // sem @

    // ── Atendente ─────────────────────────────────────────────────────────────
    // Chat ID do atendente (usuário) OU grupo onde os pedidos serão recebidos.
    // Para descobrir o ID: adicione @RawDataBot no grupo / converse com o bot
    // e copie o "chat.id" mostrado.
    public static final String CHAT_ID_ATENDENTE = "";

    // ── Dados do restaurante ──────────────────────────────────────────────────
    public static final String ENDERECO = "Rua Rio Parnaiba 49, Petrolina";
    public static final String LINK_MAPS = "https://www.google.com/maps/@-9.3688499,-40.4984852,3a,75y,86.36h,90t/data=!3m7!1e1!3m5!1sRbDoU4OJfx5eRl25KtxMbw!2e0!6shttps:%2F%2Fstreetviewpixels-pa.googleapis.com%2Fv1%2Fthumbnail%3Fcb_client%3Dmaps_sv.tactile%26w%3D900%26h%3D600%26pitch%3D0%26panoid%3DRbDoU4OJfx5eRl25KtxMbw%26yaw%3D86.36!7i16384!8i8192?entry=ttu&g_ep=EgoyMDI2MDUxMS4wIKXMDSoASAFQAw%3D%3D";

    public static final String HORARIOS = "🕐 *Horários de atendimento:*\n" +
            "Seg a Sex: 11h às 22h\n" +
            "Sáb: 11h às 23h\n" +
            "Dom: 12h às 21h";

    // ── Cardápio (Google Drive) ───────────────────────────────────────────────
    // Para usar o Google Drive:
    // 1. Compartilhe a imagem como "qualquer pessoa com o link"
    // 2. Copie o ID do arquivo (parte entre /d/ e /view na URL)
    // 3. Cole em DRIVE_CARDAPIO_FILE_ID
    // O CardapioService usa esse ID para montar a URL pública de download.
    public static final String DRIVE_CARDAPIO_FILE_ID = "";

    // Fallback: caminho local da imagem usado caso o ID do Drive não esteja
    // configurado (mantém compatibilidade com o que já existia no projeto).
    public static final String CARDAPIO_LOCAL_PATH =
            "C:/Studys/telegram-bot/telegram/src/main/resources/img/cardapio.jpeg";

    // ── Pagamento ─────────────────────────────────────────────────────────────
    // Chave Pix fictícia exibida ao cliente quando ele escolhe pagar via Pix.
    public static final String CHAVE_PIX_FICTICIA = "restaurante@pix.com";
}
