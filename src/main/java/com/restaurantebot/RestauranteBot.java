package com.restaurantebot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RestauranteBot extends TelegramLongPollingBot {

    private static final Logger log = LoggerFactory.getLogger(RestauranteBot.class);

    // ── Estado por usuário ────────────────────────────────────────────────────
    private final Map<Long, EstadoConversa> estados = new HashMap<>();
    private final Map<Long, Pedido>         pedidos = new HashMap<>();

    private static final Set<String> SAUDACOES = Set.of(
            "oi", "olá", "ola", "hey", "eae", "e aí", "e ai",
            "bom dia", "boa tarde", "boa noite", "menu", "início", "inicio"
    );

    @Override public String getBotToken()    { return Config.BOT_TOKEN; }
    @Override public String getBotUsername() { return Config.BOT_USERNAME; }

    // ── Entrada principal ─────────────────────────────────────────────────────
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasCallbackQuery()) {
            processarCallback(update);
        } else if (update.hasMessage() && update.getMessage().hasText()) {
            processarTexto(update);
        }
    }

    // =========================================================================
    // TEXTO
    // =========================================================================
    private void processarTexto(Update update) {
        long   chatId = update.getMessage().getChatId();
        String texto  = update.getMessage().getText().trim();
        String lower  = texto.toLowerCase();

        EstadoConversa estado = estados.getOrDefault(chatId, EstadoConversa.INICIO);

        // Qualquer saudação ou /start sempre volta ao menu principal
        if (lower.equals("/start") || SAUDACOES.contains(lower)) {
            enviarMenuPrincipal(chatId);
            return;
        }

        switch (estado) {

            case AGUARDANDO_PRATO -> {
                pedidos.get(chatId).adicionarPrato(texto);
                estados.put(chatId, EstadoConversa.AGUARDANDO_TIPO_ENTREGA);
                enviarMensagem(chatId,
                        "✅ Prato anotado!\n\nComo deseja receber o pedido?",
                        teclado(List.of(
                                List.of(btn("🚚 Entrega", "entrega"),
                                        btn("🏠 Retirada", "retirada"))
                        )));
            }

            case AGUARDANDO_ENDERECO -> {
                pedidos.get(chatId).setEndereco(texto);
                estados.put(chatId, EstadoConversa.AGUARDANDO_NOME_ENTREGA);
                enviarMensagemSimples(chatId, "👤 Nome de quem vai receber o pedido:");
            }

            case AGUARDANDO_NOME_ENTREGA -> {
                pedidos.get(chatId).setNomeCliente(texto);
                estados.put(chatId, EstadoConversa.AGUARDANDO_PAGAMENTO);
                enviarMensagem(chatId, "💳 Forma de pagamento:",
                        tecladoPagamento());
            }

            case AGUARDANDO_NOME_RETIRADA -> {
                pedidos.get(chatId).setNomeCliente(texto);
                estados.put(chatId, EstadoConversa.AGUARDANDO_PAGAMENTO);
                enviarMensagem(chatId, "💳 Forma de pagamento:",
                        tecladoPagamento());
            }

            default -> enviarMenuPrincipal(chatId);
        }
    }

    // =========================================================================
    // CALLBACK (botões)
    // =========================================================================
    private void processarCallback(Update update) {
        var    query  = update.getCallbackQuery();
        long   chatId = query.getMessage().getChatId();
        int    msgId  = query.getMessage().getMessageId();
        String data   = query.getData();

        // Responde ao Telegram para remover o "loading" do botão
        try { execute(org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery
                .builder().callbackQueryId(query.getId()).build()); }
        catch (TelegramApiException ignored) {}

        EstadoConversa estado = estados.getOrDefault(chatId, EstadoConversa.INICIO);

        switch (data) {

            // ── Menu principal ────────────────────────────────────────────────
            case "endereco" -> {
                editarMensagem(chatId, msgId,
                        "📍 *Endereço do restaurante:*\n" + Config.ENDERECO +
                        "\n\n🗺️ [Ver no Google Maps](" + Config.LINK_MAPS + ")");
                enviarMenuPrincipal(chatId);
            }

            case "horarios" -> {
                editarMensagem(chatId, msgId, Config.HORARIOS);
                enviarMenuPrincipal(chatId);
            }

            case "cardapio" -> {
                editarMensagem(chatId, msgId, "📋 Abrindo o cardápio...");
                enviarCardapio(chatId);
            }

            case "atendente" -> {
                editarMensagem(chatId, msgId,
                        "👤 Certo! Um atendente dará continuidade ao seu atendimento.");
                notificarAtendente(chatId, query.getFrom().getFirstName(), "solicitou atendimento humano.");
            }

            // ── Após cardápio ─────────────────────────────────────────────────
            case "sim_pedido" -> {
                estados.put(chatId, EstadoConversa.AGUARDANDO_PRATO);
                pedidos.put(chatId, new Pedido());
                editarMensagem(chatId, msgId,
                        "🍽️ Qual prato você deseja pedir?\n_Exemplo: Marmitex grande de frango_");
            }

            case "nao_pedido" -> {
                editarMensagem(chatId, msgId, "Tudo bem! Se precisar de algo, estou aqui. 😊");
                enviarMenuPrincipal(chatId);
            }

            // ── Tipo de entrega ───────────────────────────────────────────────
            case "entrega" -> {
                pedidos.get(chatId).setTipoEntrega("ENTREGA");
                estados.put(chatId, EstadoConversa.AGUARDANDO_ENDERECO);
                editarMensagem(chatId, msgId,
                        "📦 Informe o endereço completo para entrega:\n_Exemplo: Rua João Silva, 123_");
            }

            case "retirada" -> {
                pedidos.get(chatId).setTipoEntrega("RETIRADA");
                estados.put(chatId, EstadoConversa.AGUARDANDO_NOME_RETIRADA);
                editarMensagem(chatId, msgId,
                        "👤 Nome de quem irá retirar o pedido:");
            }

            // ── Pagamento ─────────────────────────────────────────────────────
            case "pag_dinheiro", "pag_pix", "pag_credito", "pag_debito" -> {
                String pagamento = switch (data) {
                    case "pag_dinheiro" -> "Dinheiro 💵";
                    case "pag_pix"      -> "Pix 📱";
                    case "pag_credito"  -> "Crédito 💳";
                    default             -> "Débito 💳";
                };
                pedidos.get(chatId).setPagamento(pagamento);
                estados.put(chatId, EstadoConversa.AGUARDANDO_MAIS_PEDIDO);
                editarMensagem(chatId, msgId, "✅ Pagamento registrado!");
                enviarMensagem(chatId, "Deseja adicionar mais um prato ao pedido?",
                        teclado(List.of(
                                List.of(btn("✅ Sim", "mais_sim"),
                                        btn("❌ Não, finalizar", "mais_nao"))
                        )));
            }

            // ── Mais pedido ───────────────────────────────────────────────────
            case "mais_sim" -> {
                estados.put(chatId, EstadoConversa.AGUARDANDO_PRATO);
                editarMensagem(chatId, msgId,
                        "🍽️ Qual prato deseja adicionar?\n_Exemplo: Marmitex grande de frango_");
            }

            case "mais_nao" -> {
                Pedido pedido = pedidos.get(chatId);
                String resumo = pedido.resumo();

                // Envia resumo para o cliente
                editarMensagem(chatId, msgId, "✅ Pedido concluído!\n\n" + resumo);

                // Envia para o grupo dos atendentes
                notificarAtendente(chatId, query.getFrom().getFirstName(), resumo);

                // Limpa o estado
                estados.put(chatId, EstadoConversa.INICIO);
                pedidos.remove(chatId);

                enviarMensagem(chatId, "Pode aguardar, em breve entraremos em contato! 😊",
                        teclado(List.of(
                                List.of(btn("🏠 Voltar ao menu", "voltar_menu"))
                        )));
            }

            case "voltar_menu" -> {
                editarMensagem(chatId, msgId, "Redirecionando ao menu...");
                enviarMenuPrincipal(chatId);
            }
        }
    }

    // =========================================================================
    // ENVIO DE MENSAGENS
    // =========================================================================
    private void enviarMenuPrincipal(long chatId) {
        estados.put(chatId, EstadoConversa.INICIO);
        enviarMensagem(chatId,
                "🍽️ *Bem-vindo ao atendimento do restaurante!*\n\nComo podemos te ajudar hoje?",
                teclado(List.of(
                        List.of(btn("📍 Endereço",            "endereco"),
                                btn("🕐 Horários",            "horarios")),
                        List.of(btn("📋 Cardápio do dia",     "cardapio")),
                        List.of(btn("👤 Falar com atendente", "atendente"))
                )));
    }

    private void enviarCardapio(long chatId) {
        try {
            SendPhoto photo = SendPhoto.builder()
                    .chatId(String.valueOf(chatId))
                    .photo(new InputFile(Config.CARDAPIO_URL))
                    .caption("Aqui está o cardápio de hoje! 😋\n\nDeseja fazer um pedido?")
                    .replyMarkup(teclado(List.of(
                            List.of(btn("✅ Sim, fazer pedido", "sim_pedido"),
                                    btn("❌ Não", "nao_pedido"))
                    )))
                    .parseMode("Markdown")
                    .build();
            execute(photo);
        } catch (TelegramApiException e) {
            log.error("Erro ao enviar cardápio", e);
        }
    }

    private void notificarAtendente(long chatIdCliente, String nomeCliente, String conteudo) {
        String msg = "🔔 *Novo atendimento!*\n\n" +
                     "👤 *Cliente:* " + nomeCliente + "\n" +
                     "🆔 *Chat ID:* `" + chatIdCliente + "`\n\n" + conteudo;
        try {
            SendMessage send = SendMessage.builder()
                    .chatId(Config.CHAT_ID_ATENDENTE)
                    .text(msg)
                    .parseMode("Markdown")
                    .build();
            execute(send);
        } catch (TelegramApiException e) {
            log.error("Erro ao notificar atendente", e);
        }
    }

    private void enviarMensagem(long chatId, String texto, InlineKeyboardMarkup teclado) {
        try {
            execute(SendMessage.builder()
                    .chatId(String.valueOf(chatId))
                    .text(texto)
                    .parseMode("Markdown")
                    .replyMarkup(teclado)
                    .build());
        } catch (TelegramApiException e) {
            log.error("Erro ao enviar mensagem", e);
        }
    }

    private void enviarMensagemSimples(long chatId, String texto) {
        try {
            execute(SendMessage.builder()
                    .chatId(String.valueOf(chatId))
                    .text(texto)
                    .parseMode("Markdown")
                    .build());
        } catch (TelegramApiException e) {
            log.error("Erro ao enviar mensagem simples", e);
        }
    }

    private void editarMensagem(long chatId, int msgId, String texto) {
        try {
            execute(EditMessageText.builder()
                    .chatId(String.valueOf(chatId))
                    .messageId(msgId)
                    .text(texto)
                    .parseMode("Markdown")
                    .build());
        } catch (TelegramApiException e) {
            log.warn("Não foi possível editar mensagem: {}", e.getMessage());
        }
    }

    // =========================================================================
    // HELPERS DE TECLADO
    // =========================================================================
    private InlineKeyboardMarkup teclado(List<List<InlineKeyboardButton>> linhas) {
        return InlineKeyboardMarkup.builder().keyboard(linhas).build();
    }

    private InlineKeyboardButton btn(String label, String data) {
        return InlineKeyboardButton.builder().text(label).callbackData(data).build();
    }

    private InlineKeyboardMarkup tecladoPagamento() {
        return teclado(List.of(
                List.of(btn("💵 Dinheiro", "pag_dinheiro"),
                        btn("📱 Pix",      "pag_pix")),
                List.of(btn("💳 Crédito",  "pag_credito"),
                        btn("💳 Débito",   "pag_debito"))
        ));
    }
}
