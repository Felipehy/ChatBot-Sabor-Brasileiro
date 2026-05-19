package com.restaurantebot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RestauranteBot extends TelegramLongPollingBot {

    private static final Logger log = LoggerFactory.getLogger(RestauranteBot.class);

    private final Map<Long, EstadoConversa> estados = new HashMap<>();
    private final Map<Long, Pedido> pedidos = new HashMap<>();
    private final Map<Integer, Long> notificacaoParaCliente = new HashMap<>();
    private final Map<Long, Long> sessaoAtiva = new HashMap<>();

    private final CardapioService cardapioService = new CardapioService();

    private static final Set<String> SAUDACOES = Set.of(
            "oi", "ola", "olá", "hey", "eae", "e ai",
            "bom dia", "boa tarde", "boa noite", "menu", "inicio", "início");

    @Override public String getBotToken()    { return Config.BOT_TOKEN; }
    @Override public String getBotUsername() { return Config.BOT_USERNAME; }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasCallbackQuery()) {
            processarCallback(update);
            return;
        }
        if (!update.hasMessage()) return;

        Message message = update.getMessage();
        long chatId = message.getChatId();
        boolean fromAtendente = isAtendenteChat(chatId);

        if (message.hasText()) {
            if (fromAtendente) processarMensagemAtendente(update);
            else               processarTexto(update);
            return;
        }
        if (message.hasPhoto()) {
            if (fromAtendente) repassarFotoAtendenteParaCliente(update);
            else               repassarFotoClienteParaAtendente(update);
        }
    }

    private boolean isAtendenteChat(long chatId) {
        if (Config.CHAT_ID_ATENDENTE == null || Config.CHAT_ID_ATENDENTE.isBlank()) {
            return false;
        }
        try {
            return Long.parseLong(Config.CHAT_ID_ATENDENTE.trim()) == chatId;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void processarTexto(Update update) {
        long chatId = update.getMessage().getChatId();
        String texto = update.getMessage().getText().trim();
        String lower = texto.toLowerCase();
        User from = update.getMessage().getFrom();

        EstadoConversa estado = estados.getOrDefault(chatId, EstadoConversa.INICIO);

        if (estado == EstadoConversa.EM_ATENDIMENTO) {
            if (lower.equals("/sair") || lower.equals("/start")) {
                return;
            }
            encaminharMensagemAoAtendente(chatId, from, texto);
            return;
        }

        if (lower.equals("/start") || SAUDACOES.contains(lower)) {
            enviarMenuPrincipal(chatId);
            return;
        }

        switch (estado) {
            case AGUARDANDO_ENDERECO -> {
                pedidos.get(chatId).setEndereco(texto);
                estados.put(chatId, EstadoConversa.AGUARDANDO_NOME_ENTREGA);
                enviarMensagemSimples(chatId, "Informe o nome de quem vai receber o pedido.");
            }
            case AGUARDANDO_NOME_ENTREGA -> {
                pedidos.get(chatId).setNomeCliente(texto);
                coletarFormaPagamento(chatId);
            }
            case AGUARDANDO_NOME_RETIRADA -> {
                pedidos.get(chatId).setNomeCliente(texto);
                coletarFormaPagamento(chatId);
            }
            case INICIO -> enviarMenuPrincipal(chatId);

            default -> enviarMensagemSimples(chatId,
                    "Por favor, selecione uma das opções pelos botões acima.");
        }
    }

    private void processarCallback(Update update) {
        var query = update.getCallbackQuery();
        long chatId = query.getMessage().getChatId();
        int msgId   = query.getMessage().getMessageId();
        String data = query.getData();
        User from   = query.getFrom();

        try {
            execute(AnswerCallbackQuery.builder().callbackQueryId(query.getId()).build());
        } catch (TelegramApiException ignored) { }

        if (data.startsWith("tam_")) { handleTamanho(chatId, data);          return; }
        if (data.startsWith("opc_")) { handleOpcaoCategoria(chatId, data);   return; }
        if (data.startsWith("beb_")) { handleEscolhaBebida(chatId, data);    return; }
        if (data.startsWith("rm_"))  { handleRemocao(chatId, data);          return; }
        if (data.equals("pag_credito") || data.equals("pag_debito") || data.equals("pag_pix")) {
            finalizarPedido(chatId, from, data);
            return;
        }

        switch (data) {
            case "endereco" -> {
                editarMensagem(chatId, msgId,
                        "*Endereço do restaurante:*\n" + Config.ENDERECO +
                                "\n\n[Ver no Google Maps](" + Config.LINK_MAPS + ")");
                enviarBotaoVoltarMenu(chatId);
            }
            case "horarios" -> {
                editarMensagem(chatId, msgId, Config.HORARIOS);
                enviarBotaoVoltarMenu(chatId);
            }
            case "cardapio" -> {
                editarMensagem(chatId, msgId, "Abrindo o cardápio...");
                enviarCardapioImagem(chatId);
            }
            case "atendente" -> {
                editarMensagem(chatId, msgId,
                        "Você está em atendimento com a equipe.\n" +
                                "Digite sua mensagem que ela será encaminhada.");
                iniciarAtendimentoHumano(chatId, from);
            }
            case "voltar_menu" -> enviarMenuPrincipal(chatId);

            case "inicio_marmita_sim" -> iniciarMontagemMarmita(chatId);
            case "inicio_marmita_nao" -> enviarMenuPrincipal(chatId);

            case "outra_marmita_sim" -> selecionarTamanhoMarmita(chatId);
            case "outra_marmita_nao" -> iniciarFluxoBebidas(chatId);

            case "bebida_inicio_sim" -> exibirBebidasDisponiveis(chatId);
            case "bebida_inicio_nao" -> gerarResumoPedido(chatId);

            case "outra_bebida_sim"  -> exibirBebidasDisponiveis(chatId);
            case "outra_bebida_nao"  -> gerarResumoPedido(chatId);

            case "acao_remover"   -> exibirOpcoesRemocao(chatId);
            case "acao_finalizar" -> iniciarFluxoRecebimento(chatId);
            case "acao_cancelar"  -> cancelarPedido(chatId);

            case "entrega"  -> coletarDadosEntrega(chatId);
            case "retirada" -> coletarDadosRetirada(chatId);

            default -> enviarMenuPrincipal(chatId);
        }
    }

    private void enviarMenuPrincipal(long chatId) {
        limparEstadoCliente(chatId);
        enviarMensagem(chatId,
                "Olá! Seja bem-vindo ao atendimento do restaurante. Como podemos te ajudar hoje?",
                teclado(List.of(
                        List.of(btn("1 - Ver endereço",        "endereco")),
                        List.of(btn("2 - Ver horários",        "horarios")),
                        List.of(btn("3 - Ver cardápio",        "cardapio")),
                        List.of(btn("4 - Falar com atendente", "atendente")))));
    }

    private void enviarBotaoVoltarMenu(long chatId) {
        enviarMensagem(chatId, "Deseja voltar ao menu principal?",
                teclado(List.of(List.of(btn("Voltar ao menu", "voltar_menu")))));
    }

    private void enviarCardapioImagem(long chatId) {
        try {
            SendPhoto photo = SendPhoto.builder()
                    .chatId(String.valueOf(chatId))
                    .photo(cardapioService.buscarImagemCardapio())
                    .caption("Aqui está o cardápio.\n\nDeseja montar uma marmita?")
                    .replyMarkup(teclado(List.of(
                            List.of(btn("Sim, montar marmita", "inicio_marmita_sim")),
                            List.of(btn("Não",                 "inicio_marmita_nao")))))
                    .parseMode("Markdown")
                    .build();
            execute(photo);
            estados.put(chatId, EstadoConversa.AGUARDANDO_INICIAR_MARMITA);
        } catch (TelegramApiException e) {
            log.error("Erro ao enviar cardápio", e);
        }
    }

    private void iniciarMontagemMarmita(long chatId) {
        pedidos.computeIfAbsent(chatId, k -> new Pedido());
        selecionarTamanhoMarmita(chatId);
    }

    private void selecionarTamanhoMarmita(long chatId) {
        Pedido pedido = pedidos.computeIfAbsent(chatId, k -> new Pedido());
        pedido.iniciarNovaMarmita();

        List<ItemCardapio> tamanhos = cardapioService.buscarTamanhosMarmita();
        List<List<InlineKeyboardButton>> linhas = new ArrayList<>();
        for (int i = 0; i < tamanhos.size(); i++) {
            ItemCardapio t = tamanhos.get(i);
            linhas.add(List.of(btn(t.getNome() + " - " + t.getValorFormatado(), "tam_" + i)));
        }
        linhas.add(List.of(btn("Cancelar", "tam_cancelar")));

        estados.put(chatId, EstadoConversa.AGUARDANDO_TAMANHO_MARMITA);
        enviarMensagem(chatId, "Escolha o tamanho da marmita:", teclado(linhas));
    }

    private void handleTamanho(long chatId, String data) {
        if (data.equals("tam_cancelar")) { cancelarPedido(chatId); return; }

        Pedido pedido = pedidos.get(chatId);
        if (pedido == null || pedido.getMarmitaAtual() == null) {
            enviarMenuPrincipal(chatId);
            return;
        }
        int idx = parseIndice(data, "tam_");
        List<ItemCardapio> tamanhos = cardapioService.buscarTamanhosMarmita();
        if (idx < 0 || idx >= tamanhos.size()) {
            enviarMensagemSimples(chatId, "Opção inválida. Tente novamente.");
            selecionarTamanhoMarmita(chatId);
            return;
        }
        ItemCardapio t = tamanhos.get(idx);
        pedido.getMarmitaAtual().definirTamanho(t.getNome(), t.getValor());
        selecionarCategoriaMarmita(chatId);
    }

    private void selecionarCategoriaMarmita(long chatId) {
        Pedido pedido = pedidos.get(chatId);
        if (pedido == null || pedido.getMarmitaAtual() == null) {
            enviarMenuPrincipal(chatId);
            return;
        }
        Categoria cat = pedido.getMarmitaAtual().proximaCategoriaPendente();
        if (cat == null) {
            adicionarMarmitaAoCarrinho(chatId);
            return;
        }

        List<OpcaoCategoria> opcoes = cardapioService.buscarOpcoesCategoria(cat);
        List<List<InlineKeyboardButton>> linhas = new ArrayList<>();
        for (int i = 0; i < opcoes.size(); i++) {
            linhas.add(List.of(btn(opcoes.get(i).labelBotao(), "opc_" + i)));
        }
        linhas.add(List.of(btn("Não quero esse item", "opc_skip")));

        estados.put(chatId, EstadoConversa.AGUARDANDO_OPCAO_CATEGORIA);
        enviarMensagem(chatId, cat.getPrompt(), teclado(linhas));
    }

    private void handleOpcaoCategoria(long chatId, String data) {
        Pedido pedido = pedidos.get(chatId);
        if (pedido == null || pedido.getMarmitaAtual() == null) {
            enviarMenuPrincipal(chatId);
            return;
        }
        Categoria cat = pedido.getMarmitaAtual().proximaCategoriaPendente();
        if (cat == null) { adicionarMarmitaAoCarrinho(chatId); return; }

        salvarOpcaoCategoria(chatId, cat, data);
        selecionarCategoriaMarmita(chatId);
    }

    private void salvarOpcaoCategoria(long chatId, Categoria cat, String data) {
        Marmita atual = pedidos.get(chatId).getMarmitaAtual();
        if (data.equals("opc_skip")) {
            atual.registrarOpcao(cat, OpcaoCategoria.PULAR);
            return;
        }
        int idx = parseIndice(data, "opc_");
        List<OpcaoCategoria> opcoes = cardapioService.buscarOpcoesCategoria(cat);
        if (idx < 0 || idx >= opcoes.size()) {
            atual.registrarOpcao(cat, OpcaoCategoria.PULAR);
            return;
        }
        atual.registrarOpcao(cat, opcoes.get(idx));
    }

    private void adicionarMarmitaAoCarrinho(long chatId) {
        Pedido pedido = pedidos.get(chatId);
        Marmita m = pedido.getMarmitaAtual();
        int numero = pedido.getMarmitas().size() + 1;
        String resumo = "Marmita adicionada ao pedido:\n\n" + m.resumo(numero) + "\n";
        pedido.adicionarMarmitaAoCarrinho();
        perguntarAdicionarOutraMarmita(chatId, resumo);
    }

    private void perguntarAdicionarOutraMarmita(long chatId, String resumoMarmita) {
        estados.put(chatId, EstadoConversa.AGUARDANDO_OUTRA_MARMITA);
        enviarMensagem(chatId, resumoMarmita + "\nDeseja adicionar outra marmita?",
                teclado(List.of(
                        List.of(btn("Sim, adicionar outra marmita", "outra_marmita_sim")),
                        List.of(btn("Não, continuar",               "outra_marmita_nao")))));
    }

    private void iniciarFluxoBebidas(long chatId) {
        estados.put(chatId, EstadoConversa.AGUARDANDO_INICIAR_BEBIDA);
        enviarMensagem(chatId, "Deseja adicionar bebidas ao pedido?",
                teclado(List.of(
                        List.of(btn("Sim, adicionar bebida", "bebida_inicio_sim")),
                        List.of(btn("Não, continuar",        "bebida_inicio_nao")))));
    }

    private void exibirBebidasDisponiveis(long chatId) {
        List<ItemCardapio> bebidas = cardapioService.buscarBebidas();
        List<List<InlineKeyboardButton>> linhas = new ArrayList<>();
        for (int i = 0; i < bebidas.size(); i++) {
            ItemCardapio b = bebidas.get(i);
            linhas.add(List.of(btn(b.getNome() + " - " + b.getValorFormatado(), "beb_" + i)));
        }
        linhas.add(List.of(btn("Não quero bebida", "beb_skip")));

        estados.put(chatId, EstadoConversa.AGUARDANDO_ESCOLHA_BEBIDA);
        enviarMensagem(chatId, "Escolha a bebida:", teclado(linhas));
    }

    private void handleEscolhaBebida(long chatId, String data) {
        Pedido pedido = pedidos.get(chatId);
        if (pedido == null) { enviarMenuPrincipal(chatId); return; }

        if (data.equals("beb_skip")) {
            gerarResumoPedido(chatId);
            return;
        }
        int idx = parseIndice(data, "beb_");
        List<ItemCardapio> bebidas = cardapioService.buscarBebidas();
        if (idx < 0 || idx >= bebidas.size()) {
            enviarMensagemSimples(chatId, "Opção inválida. Tente novamente.");
            exibirBebidasDisponiveis(chatId);
            return;
        }
        adicionarBebidaAoCarrinho(chatId, bebidas.get(idx));
    }

    private void adicionarBebidaAoCarrinho(long chatId, ItemCardapio bebida) {
        pedidos.get(chatId).adicionarBebida(bebida);
        estados.put(chatId, EstadoConversa.AGUARDANDO_OUTRA_BEBIDA);
        enviarMensagem(chatId,
                "*" + bebida.getNome() + "* adicionada ao pedido.\n\nDeseja adicionar outra bebida?",
                teclado(List.of(
                        List.of(btn("Sim, adicionar outra bebida", "outra_bebida_sim")),
                        List.of(btn("Não, continuar",              "outra_bebida_nao")))));
    }

    private void gerarResumoPedido(long chatId) {
        Pedido pedido = pedidos.get(chatId);
        if (pedido == null || pedido.vazio()) {
            enviarMensagemSimples(chatId, "Seu pedido está vazio.");
            perguntarMontarMarmita(chatId);
            return;
        }
        estados.put(chatId, EstadoConversa.AGUARDANDO_ACAO_RESUMO);
        enviarMensagem(chatId, pedido.gerarResumoCliente() + "\n\nO que deseja fazer?",
                teclado(List.of(
                        List.of(btn("Remover item",     "acao_remover")),
                        List.of(btn("Finalizar pedido", "acao_finalizar")),
                        List.of(btn("Cancelar pedido",  "acao_cancelar")))));
    }

    private void perguntarMontarMarmita(long chatId) {
        estados.put(chatId, EstadoConversa.AGUARDANDO_INICIAR_MARMITA);
        enviarMensagem(chatId, "Deseja montar uma marmita?",
                teclado(List.of(
                        List.of(btn("Sim, montar marmita", "inicio_marmita_sim")),
                        List.of(btn("Não",                 "inicio_marmita_nao")))));
    }

    private void exibirOpcoesRemocao(long chatId) {
        Pedido pedido = pedidos.get(chatId);
        if (pedido == null || pedido.vazio()) {
            enviarMensagemSimples(chatId, "Seu pedido está vazio.");
            perguntarMontarMarmita(chatId);
            return;
        }
        List<List<InlineKeyboardButton>> linhas = new ArrayList<>();
        for (int i = 0; i < pedido.getMarmitas().size(); i++) {
            linhas.add(List.of(btn("Remover Marmita " + (i + 1), "rm_marmita_" + i)));
        }
        for (int i = 0; i < pedido.getBebidas().size(); i++) {
            ItemCardapio b = pedido.getBebidas().get(i);
            linhas.add(List.of(btn("Remover " + b.getNome(), "rm_bebida_" + i)));
        }
        linhas.add(List.of(btn("Voltar ao resumo", "rm_voltar")));

        estados.put(chatId, EstadoConversa.AGUARDANDO_REMOCAO_ITEM);
        enviarMensagem(chatId, "Qual item deseja remover?", teclado(linhas));
    }

    private void handleRemocao(long chatId, String data) {
        Pedido pedido = pedidos.get(chatId);
        if (pedido == null) { enviarMenuPrincipal(chatId); return; }

        if (data.equals("rm_voltar")) { gerarResumoPedido(chatId); return; }

        removerItemDoCarrinho(chatId, data);

        if (pedido.vazio()) {
            enviarMensagemSimples(chatId, "Seu pedido está vazio.");
            perguntarMontarMarmita(chatId);
            return;
        }
        gerarResumoPedido(chatId);
    }

    private void removerItemDoCarrinho(long chatId, String data) {
        Pedido pedido = pedidos.get(chatId);
        if (data.startsWith("rm_marmita_")) {
            int idx = parseIndice(data, "rm_marmita_");
            pedido.removerMarmita(idx);
        } else if (data.startsWith("rm_bebida_")) {
            int idx = parseIndice(data, "rm_bebida_");
            pedido.removerBebida(idx);
        }
    }

    private void cancelarPedido(long chatId) {
        limparEstadoCliente(chatId);
        enviarMensagemSimples(chatId, "Pedido cancelado. Voltando ao menu principal.");
        enviarMenuPrincipal(chatId);
    }

    private void iniciarFluxoRecebimento(long chatId) {
        Pedido pedido = pedidos.get(chatId);
        if (pedido == null || pedido.vazio()) {
            enviarMenuPrincipal(chatId);
            return;
        }
        estados.put(chatId, EstadoConversa.AGUARDANDO_TIPO_RECEBIMENTO);
        enviarMensagem(chatId, "Como deseja receber o pedido?",
                teclado(List.of(
                        List.of(btn("Entrega",                "entrega"),
                                btn("Retirada no restaurante", "retirada")))));
    }

    private void coletarDadosEntrega(long chatId) {
        pedidos.get(chatId).setTipoEntrega("ENTREGA");
        estados.put(chatId, EstadoConversa.AGUARDANDO_ENDERECO);
        enviarMensagemSimples(chatId,
                "Informe o endereço completo para entrega.\n" +
                        "_Exemplo: Rua João Silva, 123 - Bairro - Cidade_");
    }

    private void coletarDadosRetirada(long chatId) {
        pedidos.get(chatId).setTipoEntrega("RETIRADA");
        estados.put(chatId, EstadoConversa.AGUARDANDO_NOME_RETIRADA);
        enviarMensagemSimples(chatId, "Informe o nome de quem irá retirar o pedido.");
    }

    private void coletarFormaPagamento(long chatId) {
        estados.put(chatId, EstadoConversa.AGUARDANDO_PAGAMENTO);
        enviarMensagem(chatId, "Informe a forma de pagamento.", tecladoPagamento());
    }

    private InlineKeyboardMarkup tecladoPagamento() {
        return teclado(List.of(
                List.of(btn("1 - Crédito", "pag_credito")),
                List.of(btn("2 - Débito",  "pag_debito")),
                List.of(btn("3 - Pix",     "pag_pix"))));
    }

    private void finalizarPedido(long chatId, User from, String callbackPagamento) {
        Pedido pedido = pedidos.get(chatId);
        if (pedido == null) { enviarMenuPrincipal(chatId); return; }

        boolean entrega = "ENTREGA".equals(pedido.getTipoEntrega());
        String pagamento;
        String statusPagamento;
        String mensagemPagamentoCliente;
        switch (callbackPagamento) {
            case "pag_pix" -> {
                pagamento = "Pix";
                statusPagamento = "Pix fictício gerado";
                mensagemPagamentoCliente = gerarPixFicticio();
            }
            case "pag_credito" -> {
                pagamento = "Crédito";
                statusPagamento = entrega
                        ? "Pagamento no local da entrega"
                        : "Pagamento no local da retirada";
                mensagemPagamentoCliente = entrega
                        ? "O pagamento será realizado no momento da entrega."
                        : "O pagamento será realizado no momento da retirada.";
            }
            default -> {
                pagamento = "Débito";
                statusPagamento = entrega
                        ? "Pagamento no local da entrega"
                        : "Pagamento no local da retirada";
                mensagemPagamentoCliente = entrega
                        ? "O pagamento será realizado no momento da entrega."
                        : "O pagamento será realizado no momento da retirada.";
            }
        }
        pedido.setPagamento(pagamento);
        pedido.setStatusPagamento(statusPagamento);

        enviarMensagemSimples(chatId, mensagemPagamentoCliente);

        enviarResumoParaAtendente(pedido, from, chatId);

        enviarMensagemSimples(chatId,
                "Pedido registrado com sucesso! Um atendente dará continuidade ao seu atendimento.");

        limparEstadoCliente(chatId);
        enviarMenuPrincipal(chatId);
    }

    private String gerarPixFicticio() {
        return "Chave Pix fictícia para pagamento: " + Config.CHAVE_PIX_FICTICIA;
    }

    private void enviarResumoParaAtendente(Pedido pedido, User from, long chatId) {
        Integer msgId = enviarMensagemAtendente(montarResumoFinal(pedido, from, chatId), "Markdown");
        if (msgId != null) notificacaoParaCliente.put(msgId, chatId);
    }

    private String montarResumoFinal(Pedido pedido, User from, long chatId) {
        boolean entrega = "ENTREGA".equals(pedido.getTipoEntrega());
        StringBuilder sb = new StringBuilder();

        sb.append("*Novo pedido gerado*\n\n");
        sb.append("Cliente: ").append(nomeOuFallback(from)).append("\n");
        if (from != null && from.getUserName() != null && !from.getUserName().isBlank()) {
            sb.append("Username: @").append(from.getUserName()).append("\n");
        }
        sb.append("Chat ID: ").append(chatId).append("\n\n");

        int i = 1;
        for (Marmita m : pedido.getMarmitas()) {
            sb.append(m.resumo(i)).append("\n\n");
            i++;
        }
        if (!pedido.getBebidas().isEmpty()) {
            sb.append("*Bebidas:*\n");
            for (ItemCardapio b : pedido.getBebidas()) {
                sb.append("- ").append(b.getNome())
                  .append(" - ").append(b.getValorFormatado()).append("\n");
            }
            sb.append("\n");
        }
        sb.append("*Valor total:* ").append(pedido.getTotalFormatado()).append("\n\n");

        if (entrega) {
            sb.append("Tipo de recebimento: Entrega\n");
            sb.append("Endereço: ").append(pedido.getEndereco()).append("\n");
            sb.append("Nome de quem vai receber: ").append(pedido.getNomeCliente()).append("\n");
        } else {
            sb.append("Tipo de recebimento: Retirada no restaurante\n");
            sb.append("Nome de quem irá retirar: ").append(pedido.getNomeCliente()).append("\n");
        }
        sb.append("Forma de pagamento: ").append(pedido.getPagamento()).append("\n");
        sb.append("Status do pagamento: ").append(pedido.getStatusPagamento());

        return sb.toString();
    }

    private void iniciarAtendimentoHumano(long chatId, User from) {
        if (Config.CHAT_ID_ATENDENTE == null || Config.CHAT_ID_ATENDENTE.isBlank()) {
            enviarMensagemSimples(chatId,
                    "Atendimento humano indisponível no momento. Voltando ao menu.");
            enviarMenuPrincipal(chatId);
            return;
        }
        estados.put(chatId, EstadoConversa.EM_ATENDIMENTO);
        notificarAtendenteSolicitacao(chatId, from);
    }

    private void notificarAtendenteSolicitacao(long chatId, User from) {
        StringBuilder sb = new StringBuilder();
        sb.append("*Solicitação de atendimento humano*\n\n");
        sb.append("Cliente: ").append(nomeOuFallback(from)).append("\n");
        if (from != null && from.getUserName() != null && !from.getUserName().isBlank()) {
            sb.append("Username: @").append(from.getUserName()).append("\n");
        }
        sb.append("Chat ID: ").append(chatId).append("\n\n");
        sb.append("O cliente está em atendimento e pode mandar mensagens aqui.\n");
        sb.append("Para iniciar a sessão de resposta, envie:\n");
        sb.append("`/atender ").append(chatId).append("`\n\n");
        sb.append("Depois disso, qualquer mensagem que você digitar será enviada a ele.\n");
        sb.append("Para encerrar:\n");
        sb.append("`/encerrar ").append(chatId).append("`");

        Integer msgId = enviarMensagemAtendente(sb.toString(), "Markdown");
        if (msgId != null) notificacaoParaCliente.put(msgId, chatId);
    }

    private void encaminharMensagemAoAtendente(long chatId, User from, String texto) {
        StringBuilder header = new StringBuilder();
        header.append("📩 De ").append(nomeOuFallback(from));
        if (from != null && from.getUserName() != null && !from.getUserName().isBlank()) {
            header.append(" (@").append(from.getUserName()).append(")");
        }
        header.append(" — chatId ").append(chatId).append(":\n").append(texto);

        // Sem Markdown: texto do cliente pode conter caracteres especiais que quebram o parser.
        Integer msgId = enviarMensagemAtendente(header.toString(), null);
        if (msgId != null) notificacaoParaCliente.put(msgId, chatId);
    }

    private void processarMensagemAtendente(Update update) {
        Message msg = update.getMessage();
        String texto = msg.getText().trim();
        long atendenteChatId = msg.getChatId();
        User atendente = msg.getFrom();
        long atendenteUserId = atendente != null ? atendente.getId() : 0L;

        if (texto.equals("/atender") || texto.startsWith("/atender ")) {
            tratarComandoAtender(texto, atendenteChatId, atendenteUserId);
            return;
        }
        if (texto.equals("/encerrar") || texto.startsWith("/encerrar ")) {
            tratarComandoEncerrar(texto, atendenteChatId, atendenteUserId);
            return;
        }

        if (msg.getReplyToMessage() != null) {
            Integer replyId = msg.getReplyToMessage().getMessageId();
            Long clienteChatId = notificacaoParaCliente.get(replyId);
            if (clienteChatId != null) {
                relayParaCliente(clienteChatId, texto, atendenteChatId);
                return;
            }
        }

        Long clienteAtivo = sessaoAtiva.get(atendenteUserId);
        if (clienteAtivo != null) {
            relayParaCliente(clienteAtivo, texto, atendenteChatId);
        }
    }

    private void tratarComandoAtender(String texto, long atendenteChatId, long atendenteUserId) {
        String arg = texto.length() > "/atender".length()
                ? texto.substring("/atender".length()).trim()
                : "";
        Long clienteChatId = parseLongOuNulo(arg);
        if (clienteChatId == null) {
            enviarMensagemSimples(atendenteChatId, "Uso: `/atender <chatId>`");
            return;
        }
        sessaoAtiva.put(atendenteUserId, clienteChatId);
        enviarMensagemSimples(atendenteChatId,
                "Sessão iniciada com o cliente `" + clienteChatId + "`.\n" +
                        "Tudo que você digitar aqui agora será enviado ao cliente.\n" +
                        "Quando terminar, envie `/encerrar " + clienteChatId + "`.");
    }

    private void tratarComandoEncerrar(String texto, long atendenteChatId, long atendenteUserId) {
        String arg = texto.length() > "/encerrar".length()
                ? texto.substring("/encerrar".length()).trim()
                : "";
        Long clienteChatId;
        if (arg.isEmpty()) {
            clienteChatId = sessaoAtiva.get(atendenteUserId);
            if (clienteChatId == null) {
                enviarMensagemSimples(atendenteChatId,
                        "Você não tem sessão ativa. Use `/encerrar <chatId>` para encerrar uma específica.");
                return;
            }
        } else {
            clienteChatId = parseLongOuNulo(arg);
            if (clienteChatId == null) {
                enviarMensagemSimples(atendenteChatId, "Uso: `/encerrar <chatId>`");
                return;
            }
        }
        if (clienteChatId.equals(sessaoAtiva.get(atendenteUserId))) {
            sessaoAtiva.remove(atendenteUserId);
        }
        encerrarAtendimentoPeloAtendente(clienteChatId, atendenteChatId);
    }

    private void encerrarAtendimentoPeloAtendente(long clienteChatId, long atendenteChatId) {
        EstadoConversa estado = estados.get(clienteChatId);
        if (estado == EstadoConversa.EM_ATENDIMENTO) {
            enviarMensagemSimples(clienteChatId,
                    "Atendimento encerrado pelo atendente. Voltando ao menu.");
            enviarMenuPrincipal(clienteChatId);
        }
        enviarMensagemSimples(atendenteChatId,
                "Atendimento com cliente " + clienteChatId + " encerrado.");
    }

    private void relayParaCliente(long clienteChatId, String texto, long atendenteChatId) {
        try {
            execute(SendMessage.builder()
                    .chatId(String.valueOf(clienteChatId))
                    .text("👤 Atendente:\n" + texto)
                    .build());
        } catch (TelegramApiException e) {
            log.error("Erro ao entregar mensagem ao cliente {}", clienteChatId, e);
            enviarMensagemSimples(atendenteChatId,
                    "Não foi possível entregar a mensagem ao cliente " + clienteChatId + ".");
        }
    }

    private void repassarFotoAtendenteParaCliente(Update update) {
        Message msg = update.getMessage();
        User atendente = msg.getFrom();
        long atendenteUserId = atendente != null ? atendente.getId() : 0L;
        long atendenteChatId = msg.getChatId();

        Long clienteChatId = null;
        if (msg.getReplyToMessage() != null) {
            Integer replyId = msg.getReplyToMessage().getMessageId();
            clienteChatId = notificacaoParaCliente.get(replyId);
        }
        if (clienteChatId == null) {
            clienteChatId = sessaoAtiva.get(atendenteUserId);
        }
        if (clienteChatId == null) return;

        String fileId = maiorFotoFileId(msg);
        String caption = msg.getCaption();
        String captionFinal = (caption != null && !caption.isBlank())
                ? "👤 Atendente: " + caption
                : "👤 Atendente";

        try {
            execute(SendPhoto.builder()
                    .chatId(String.valueOf(clienteChatId))
                    .photo(new InputFile(fileId))
                    .caption(captionFinal)
                    .build());
        } catch (TelegramApiException e) {
            log.error("Erro ao encaminhar foto ao cliente {}", clienteChatId, e);
            enviarMensagemSimples(atendenteChatId,
                    "Não foi possível enviar a foto ao cliente " + clienteChatId + ".");
        }
    }

    private void repassarFotoClienteParaAtendente(Update update) {
        Message msg = update.getMessage();
        long chatId = msg.getChatId();
        User from = msg.getFrom();
        EstadoConversa estado = estados.getOrDefault(chatId, EstadoConversa.INICIO);
        if (estado != EstadoConversa.EM_ATENDIMENTO) return;
        if (Config.CHAT_ID_ATENDENTE == null || Config.CHAT_ID_ATENDENTE.isBlank()) return;

        String fileId = maiorFotoFileId(msg);
        String caption = msg.getCaption();
        StringBuilder header = new StringBuilder("📩 De ").append(nomeOuFallback(from));
        if (from != null && from.getUserName() != null && !from.getUserName().isBlank()) {
            header.append(" (@").append(from.getUserName()).append(")");
        }
        header.append(" — chatId ").append(chatId);
        if (caption != null && !caption.isBlank()) {
            header.append(":\n").append(caption);
        }

        try {
            Message enviada = execute(SendPhoto.builder()
                    .chatId(Config.CHAT_ID_ATENDENTE)
                    .photo(new InputFile(fileId))
                    .caption(header.toString())
                    .build());
            if (enviada != null) {
                notificacaoParaCliente.put(enviada.getMessageId(), chatId);
            }
        } catch (TelegramApiException e) {
            log.error("Erro ao encaminhar foto ao atendente", e);
        }
    }

    private String maiorFotoFileId(Message msg) {
        var fotos = msg.getPhoto();
        return fotos.get(fotos.size() - 1).getFileId();
    }

    private Integer enviarMensagemAtendente(String texto, String parseMode) {
        if (Config.CHAT_ID_ATENDENTE == null || Config.CHAT_ID_ATENDENTE.isBlank()) {
            log.warn("CHAT_ID_ATENDENTE não configurado — mensagem não enviada ao atendente.");
            return null;
        }
        try {
            SendMessage.SendMessageBuilder b = SendMessage.builder()
                    .chatId(Config.CHAT_ID_ATENDENTE)
                    .text(texto);
            if (parseMode != null) b.parseMode(parseMode);
            Message m = execute(b.build());
            return m != null ? m.getMessageId() : null;
        } catch (TelegramApiException e) {
            log.error("Erro ao enviar mensagem para o atendente", e);
            return null;
        }
    }

    private Long parseLongOuNulo(String s) {
        try { return Long.parseLong(s.trim()); }
        catch (NumberFormatException e) { return null; }
    }

    private void limparEstadoCliente(long chatId) {
        estados.remove(chatId);
        pedidos.remove(chatId);
    }

    private String nomeOuFallback(User from) {
        if (from == null) return "(desconhecido)";
        if (from.getFirstName() != null && !from.getFirstName().isBlank()) return from.getFirstName();
        if (from.getUserName()  != null && !from.getUserName().isBlank())  return from.getUserName();
        return "(desconhecido)";
    }

    private int parseIndice(String data, String prefixo) {
        try {
            return Integer.parseInt(data.substring(prefixo.length()));
        } catch (NumberFormatException e) {
            return -1;
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

    private InlineKeyboardMarkup teclado(List<List<InlineKeyboardButton>> linhas) {
        return InlineKeyboardMarkup.builder().keyboard(linhas).build();
    }

    private InlineKeyboardButton btn(String label, String data) {
        return InlineKeyboardButton.builder().text(label).callbackData(data).build();
    }
}
