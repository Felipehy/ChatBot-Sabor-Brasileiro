package com.restaurantebot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageCaption;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RestauranteBot extends TelegramLongPollingBot {

    private static final Logger log = LoggerFactory.getLogger(RestauranteBot.class);

    private final Map<Long, EstadoConversa> estados = new HashMap<>();
    private final Map<Long, Pedido> pedidos = new HashMap<>();

    private static final Set<String> SAUDACOES = Set.of(
            "oi", "ola", "hey", "eae", "e ai",
            "bom dia", "boa tarde", "boa noite", "menu", "inicio");

    @Override
    public String getBotToken() {
        return Config.BOT_TOKEN;
    }

    @Override
    public String getBotUsername() {
        return Config.BOT_USERNAME;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasCallbackQuery()) {
            processarCallback(update);
        } else if (update.hasMessage() && update.getMessage().hasText()) {
            processarTexto(update);
        }
    }

    private void processarTexto(Update update) {
        long chatId = update.getMessage().getChatId();
        String texto = update.getMessage().getText().trim();
        String lower = texto.toLowerCase();

        EstadoConversa estado = estados.getOrDefault(chatId, EstadoConversa.INICIO);

        if (lower.equals("/start") || SAUDACOES.contains(lower)) {
            enviarMenuPrincipal(chatId);
            return;
        }

        switch (estado) {
            case AGUARDANDO_PRATO -> {
                pedidos.get(chatId).adicionarPrato(texto);
                estados.put(chatId, EstadoConversa.AGUARDANDO_TIPO_ENTREGA);
                enviarMensagem(chatId,
                        "Prato anotado.\n\nComo deseja receber o pedido?",
                        teclado(List.of(
                                List.of(btn("Entrega", "entrega"),
                                        btn("Retirada", "retirada")))));
            }
            case AGUARDANDO_ENDERECO -> {
                pedidos.get(chatId).setEndereco(texto);
                estados.put(chatId, EstadoConversa.AGUARDANDO_NOME_ENTREGA);
                enviarMensagemSimples(chatId, "Nome de quem vai receber o pedido:");
            }
            case AGUARDANDO_NOME_ENTREGA -> {
                pedidos.get(chatId).setNomeCliente(texto);
                estados.put(chatId, EstadoConversa.AGUARDANDO_PAGAMENTO);
                enviarMensagem(chatId, "Forma de pagamento:", tecladoPagamento());
            }
            case AGUARDANDO_NOME_RETIRADA -> {
                pedidos.get(chatId).setNomeCliente(texto);
                estados.put(chatId, EstadoConversa.AGUARDANDO_PAGAMENTO);
                enviarMensagem(chatId, "Forma de pagamento:", tecladoPagamento());
            }
            default -> enviarMenuPrincipal(chatId);
        }
    }

    private void processarCallback(Update update) {
        var query = update.getCallbackQuery();
        long chatId = query.getMessage().getChatId();
        int msgId = query.getMessage().getMessageId();
        String data = query.getData();

        try {
            execute(AnswerCallbackQuery.builder().callbackQueryId(query.getId()).build());
        } catch (TelegramApiException ignored) {
        }

        switch (data) {
            case "endereco" -> {
                editarMensagem(chatId, msgId,
                        "*Endereco do restaurante:*\n" + Config.ENDERECO +
                                "\n\n[Ver no Google Maps](" + Config.LINK_MAPS + ")");
                enviarMenuPrincipal(chatId);
            }
            case "horarios" -> {
                editarMensagem(chatId, msgId, Config.HORARIOS);
                enviarMenuPrincipal(chatId);
            }
            case "cardapio" -> {
                editarMensagem(chatId, msgId, "Abrindo o cardapio...");
                enviarCardapio(chatId);
            }
            case "atendente" -> {
                editarMensagem(chatId, msgId,
                        "Certo. Um atendente dara continuidade ao seu atendimento.");
                notificarAtendente(chatId, query.getFrom().getFirstName(), "solicitou atendimento humano.");
            }
            case "sim_pedido" -> {
                estados.put(chatId, EstadoConversa.AGUARDANDO_PRATO);
                pedidos.put(chatId, new Pedido());
                editarLegenda(chatId,
                        "Qual prato voce deseja pedir?\n_Exemplo: Marmitex grande de frango_");
            }
            case "nao_pedido" -> {
                editarLegenda(chatId, "Tudo bem. Se precisar de algo, estou aqui.");
                enviarMenuPrincipal(chatId);
            }
            case "entrega" -> {
                pedidos.get(chatId).setTipoEntrega("ENTREGA");
                estados.put(chatId, EstadoConversa.AGUARDANDO_ENDERECO);
                editarMensagem(chatId, msgId,
                        "Informe o endereco completo para entrega:\n_Exemplo: Rua Joao Silva, 123_");
            }
            case "retirada" -> {
                pedidos.get(chatId).setTipoEntrega("RETIRADA");
                estados.put(chatId, EstadoConversa.AGUARDANDO_NOME_RETIRADA);
                editarMensagem(chatId, msgId, "Nome de quem ira retirar o pedido:");
            }
            case "pag_dinheiro", "pag_pix", "pag_credito", "pag_debito" -> {
                String pagamento = switch (data) {
                    case "pag_dinheiro" -> "Dinheiro";
                    case "pag_pix" -> "Pix";
                    case "pag_credito" -> "Credito";
                    default -> "Debito";
                };
                pedidos.get(chatId).setPagamento(pagamento);
                estados.put(chatId, EstadoConversa.AGUARDANDO_MAIS_PEDIDO);
                editarMensagem(chatId, msgId, "Pagamento registrado.");
                enviarMensagem(chatId, "Deseja adicionar mais um prato ao pedido?",
                        teclado(List.of(
                                List.of(btn("Sim", "mais_sim"),
                                        btn("Nao, finalizar", "mais_nao")))));
            }
            case "mais_sim" -> {
                estados.put(chatId, EstadoConversa.AGUARDANDO_PRATO);
                editarMensagem(chatId, msgId,
                        "Qual prato deseja adicionar?\n_Exemplo: Marmitex grande de frango_");
            }
            case "mais_nao" -> {
                Pedido pedido = pedidos.get(chatId);
                String resumo = pedido.resumo();

                editarMensagem(chatId, msgId, "Pedido concluido.\n\n" + resumo);
                notificarAtendente(chatId, query.getFrom().getFirstName(), resumo);

                estados.put(chatId, EstadoConversa.INICIO);
                pedidos.remove(chatId);

                enviarMensagem(chatId, "Pode aguardar, em breve entraremos em contato.",
                        teclado(List.of(
                                List.of(btn("Voltar ao menu", "voltar_menu")))));
            }
            case "voltar_menu" -> {
                editarMensagem(chatId, msgId, "Redirecionando ao menu...");
                enviarMenuPrincipal(chatId);
            }
        }
    }

    private void enviarMenuPrincipal(long chatId) {
        estados.put(chatId, EstadoConversa.INICIO);
        enviarMensagem(chatId,
                "*Bem-vindo ao atendimento do restaurante!*\n\nComo podemos te ajudar hoje?",
                teclado(List.of(
                        List.of(btn("Endereco", "endereco"),
                                btn("Horarios", "horarios")),
                        List.of(btn("Cardapio do dia", "cardapio")),
                        List.of(btn("Falar com atendente", "atendente")))));
    }

    private void enviarCardapio(long chatId) {
        try {
            File imagem = new File(Config.CARDAPIO_URL);
            SendPhoto photo = SendPhoto.builder()
                    .chatId(String.valueOf(chatId))
                    .photo(new InputFile(imagem))
                    .caption("Aqui esta o cardapio de hoje.\n\nDeseja fazer um pedido?")
                    .replyMarkup(teclado(List.of(
                            List.of(btn("Sim, fazer pedido", "sim_pedido"),
                                    btn("Nao", "nao_pedido")))))
                    .parseMode("Markdown")
                    .build();
            execute(photo);
        } catch (TelegramApiException e) {
            log.error("Erro ao enviar cardapio", e);
        }
    }

    private void notificarAtendente(long chatIdCliente, String nomeCliente, String conteudo) {
        String msg = "*Novo atendimento!*\n\n" +
                "*Cliente:* " + nomeCliente + "\n" +
                "*Chat ID:* `" + chatIdCliente + "`\n\n" + conteudo;
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
            log.warn("Nao foi possivel editar mensagem: {}", e.getMessage());
        }
    }

    private void editarLegenda(long chatId, String texto) {
        try {
            execute(SendMessage.builder()
                    .chatId(String.valueOf(chatId))
                    .text(texto)
                    .parseMode("Markdown")
                    .build());
        } catch (TelegramApiException e) {
            log.warn("Nao foi possivel editar legenda: {}", e.getMessage());
        }
    }

    private InlineKeyboardMarkup teclado(List<List<InlineKeyboardButton>> linhas) {
        return InlineKeyboardMarkup.builder().keyboard(linhas).build();
    }

    private InlineKeyboardButton btn(String label, String data) {
        return InlineKeyboardButton.builder().text(label).callbackData(data).build();
    }

    private InlineKeyboardMarkup tecladoPagamento() {
        return teclado(List.of(
                List.of(btn("Dinheiro", "pag_dinheiro"),
                        btn("Pix", "pag_pix")),
                List.of(btn("Credito", "pag_credito"),
                        btn("Debito", "pag_debito"))));
    }
}
