package com.restaurantebot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class ChatBot extends TelegramLongPollingBot {

    private static final Logger log = LoggerFactory.getLogger(ChatBot.class);

    private static final String BOT_TOKEN = "SEU_TOKEN_AQUI";
    private static final String BOT_USERNAME = "SEU_USERNAME_AQUI";

    private static final Set<String> SAUDACOES = Set.of(
            "oi", "olá", "ola", "hey", "eae", "e aí", "e ai", "bom dia", "boa tarde", "boa noite");

    private static final Map<String, String> RESPOSTAS = Map.of(
            "informacoes", "ℹ️ Aqui estão as informações:\n• Item 1\n• Item 2\n• Item 3",
            "suporte", "🛠️ Para suporte, acesse: https://seusite.com/suporte",
            "atendente", "📞 Um atendente irá te contatar em breve. Aguarde!",
            "encerrar", "👋 Até logo! Se precisar de algo, é só chamar.");

    @Override
    public String getBotToken() {
        return BOT_TOKEN;
    }

    @Override
    public String getBotUsername() {
        return BOT_USERNAME;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasCallbackQuery()) {
            handleCallback(update);
            return;
        }
        if (update.hasMessage() && update.getMessage().hasText()) {
            handleMessage(update);
        }
    }

    private void handleMessage(Update update) {
        String texto = update.getMessage().getText().toLowerCase().trim();
        String chatId = update.getMessage().getChatId().toString();

        if (texto.equals("/start") || SAUDACOES.contains(texto)) {
            enviarMenu(chatId, "Olá! 👋 Como posso te ajudar?\nEscolha uma das opções abaixo:");
        } else {
            enviarMenu(chatId, "Não entendi 😅 Use o menu abaixo:");
        }
    }

    private void handleCallback(Update update) {
        var query = update.getCallbackQuery();
        var chatId = query.getMessage().getChatId().toString();
        var msgId = query.getMessage().getMessageId();
        var data = query.getData();

        String resposta = RESPOSTAS.getOrDefault(data, "Opção não reconhecida.");

        EditMessageText edit = EditMessageText.builder()
                .chatId(chatId)
                .messageId(msgId)
                .text(resposta)
                .build();

        try {
            execute(edit);
        } catch (TelegramApiException e) {
            log.error("Erro ao editar mensagem", e);
        }

        if (!data.equals("encerrar")) {
            enviarMenu(chatId, "Posso ajudar com mais alguma coisa?");
        }
    }

    private void enviarMenu(String chatId, String texto) {
        SendMessage msg = SendMessage.builder()
                .chatId(chatId)
                .text(texto)
                .replyMarkup(menuPrincipal())
                .build();

        try {
            execute(msg);
        } catch (TelegramApiException e) {
            log.error("Erro ao enviar mensagem", e);
        }
    }

    private InlineKeyboardMarkup menuPrincipal() {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(btn("📋 Ver informações", "informacoes")))
                .keyboardRow(List.of(btn("🛠️ Suporte", "suporte")))
                .keyboardRow(List.of(btn("📞 Falar com atendente", "atendente")))
                .keyboardRow(List.of(btn("❌ Encerrar", "encerrar")))
                .build();
    }

    private InlineKeyboardButton btn(String label, String callbackData) {
        return InlineKeyboardButton.builder()
                .text(label)
                .callbackData(callbackData)
                .build();
    }
}
