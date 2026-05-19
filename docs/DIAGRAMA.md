# Diagramas do Bot do Restaurante

Diagramas em Mermaid refletindo o estado atual do código em `src/main/java/com/restaurantebot/`.

## Arquitetura de Classes

```mermaid
classDiagram
    class Main {
        +main(args) void
    }

    class RestauranteBot {
        -Map~Long,EstadoConversa~ estados
        -Map~Long,Pedido~ pedidos
        -Map~Integer,Long~ notificacaoParaCliente
        -Map~Long,Long~ sessaoAtiva
        -CardapioService cardapioService
        +onUpdateReceived(Update) void
        -processarTexto(Update) void
        -processarCallback(Update) void
        -processarMensagemAtendente(Update) void
        -enviarMenuPrincipal(chatId) void
        -enviarCardapioImagem(chatId) void
        -selecionarTamanhoMarmita(chatId) void
        -selecionarCategoriaMarmita(chatId) void
        -iniciarFluxoBebidas(chatId) void
        -gerarResumoPedido(chatId) void
        -finalizarPedido(...) void
        -iniciarAtendimentoHumano(...) void
        -relayParaCliente(...) void
    }

    class Pedido {
        -List~Marmita~ marmitas
        -List~ItemCardapio~ bebidas
        -Marmita marmitaAtual
        -String tipoEntrega
        -String endereco
        -String nomeCliente
        -String pagamento
        -String statusPagamento
        +iniciarNovaMarmita() Marmita
        +adicionarMarmitaAoCarrinho() void
        +adicionarBebida(ItemCardapio) void
        +calcularTotal() double
        +gerarResumoCliente() String
    }

    class Marmita {
        -String tamanhoNome
        -double valorBase
        -Map~Categoria,OpcaoCategoria~ selecoes
        +definirTamanho(nome, valor) void
        +registrarOpcao(cat, opcao) void
        +proximaCategoriaPendente() Categoria
        +calcularSubtotal() double
        +resumo(numero) String
    }

    class Categoria {
        <<enumeration>>
        ARROZ
        FEIJAO
        MACARRAO
        LEGUMES
        CARNES
        ADICIONAIS
    }

    class OpcaoCategoria {
        -String nome
        -double valorExtra
        +PULAR$ OpcaoCategoria
        +labelBotao() String
        +labelResumo() String
    }

    class ItemCardapio {
        -String nome
        -double valor
        +getValorFormatado() String
    }

    class CardapioService {
        +buscarImagemCardapio() InputFile
        +buscarTamanhosMarmita() List
        +buscarOpcoesCategoria(Categoria) List
        +buscarBebidas() List
    }

    class EstadoConversa {
        <<enumeration>>
        INICIO
        AGUARDANDO_INICIAR_MARMITA
        AGUARDANDO_TAMANHO_MARMITA
        AGUARDANDO_OPCAO_CATEGORIA
        AGUARDANDO_OUTRA_MARMITA
        AGUARDANDO_INICIAR_BEBIDA
        AGUARDANDO_ESCOLHA_BEBIDA
        AGUARDANDO_OUTRA_BEBIDA
        AGUARDANDO_ACAO_RESUMO
        AGUARDANDO_REMOCAO_ITEM
        AGUARDANDO_TIPO_RECEBIMENTO
        AGUARDANDO_ENDERECO
        AGUARDANDO_NOME_ENTREGA
        AGUARDANDO_NOME_RETIRADA
        AGUARDANDO_PAGAMENTO
        EM_ATENDIMENTO
    }

    class Config {
        <<static>>
        +BOT_TOKEN String
        +BOT_USERNAME String
        +CHAT_ID_ATENDENTE String
        +ENDERECO String
        +HORARIOS String
        +LINK_MAPS String
        +CHAVE_PIX_FICTICIA String
        +DRIVE_CARDAPIO_FILE_ID String
        +CARDAPIO_LOCAL_PATH String
    }

    class TelegramLongPollingBot {
        <<abstract>>
    }

    Main --> RestauranteBot : registra
    RestauranteBot --|> TelegramLongPollingBot
    RestauranteBot --> Pedido : 1..* por chatId
    RestauranteBot --> EstadoConversa : 1 por chatId
    RestauranteBot --> CardapioService : usa
    RestauranteBot ..> Config : lê
    Pedido --> Marmita : contém *
    Pedido --> ItemCardapio : bebidas *
    Marmita --> Categoria : usa
    Marmita --> OpcaoCategoria : 1 por categoria
    CardapioService --> ItemCardapio : produz
    CardapioService --> OpcaoCategoria : produz
    CardapioService --> Categoria : mapeia
```

## Fluxo de Conversa

```mermaid
flowchart TD
    Start([/start ou saudação]) --> Menu{Menu Principal}
    Menu -->|1 Endereço| Ender[Mostra endereço + Maps] --> Voltar[Voltar ao menu] --> Menu
    Menu -->|2 Horários| Hor[Mostra horários] --> Voltar
    Menu -->|3 Cardápio| Card[Envia imagem cardápio]
    Menu -->|4 Atendente| Atend[EM_ATENDIMENTO<br/>relay com atendente]

    Card --> QMarm{Montar marmita?}
    QMarm -->|Não| Menu
    QMarm -->|Sim| Tam[Escolher tamanho<br/>P / M / G]

    Tam --> Arroz[Arroz] --> Feijao[Feijão] --> Mac[Macarrão]
    Mac --> Leg[Legumes] --> Carne[Carnes] --> Adic[Adicionais]
    Adic --> AddCart[Marmita ao carrinho]
    AddCart --> Outra{Outra marmita?}
    Outra -->|Sim| Tam
    Outra -->|Não| QBeb{Adicionar bebida?}

    QBeb -->|Sim| EscBeb[Escolher bebida] --> AddBeb[Bebida ao carrinho]
    AddBeb --> OutraBeb{Outra bebida?}
    OutraBeb -->|Sim| EscBeb
    OutraBeb -->|Não| Resumo
    QBeb -->|Não| Resumo[Resumo do pedido]

    Resumo -->|Remover| Rem[Lista itens removíveis] --> Resumo
    Resumo -->|Cancelar| Cancel[Pedido cancelado] --> Menu
    Resumo -->|Finalizar| Receb{Tipo de recebimento}

    Receb -->|Entrega| End2[Pedir endereço] --> NomeE[Pedir nome] --> Pag
    Receb -->|Retirada| NomeR[Pedir nome retirada] --> Pag

    Pag{Forma de pagamento} -->|Pix| Pix[Gera chave Pix fictícia]
    Pag -->|Crédito/Débito| Local[Pagamento no local]
    Pix --> Notif
    Local --> Notif[Envia resumo ao atendente]
    Notif --> Conf[Confirma ao cliente] --> Menu

    Atend -.->|Cliente digita| FwdA[Encaminha ao atendente]
    Atend -.->|/encerrar do atendente| Menu
```

## Comunicação com Telegram

```mermaid
sequenceDiagram
    actor Cliente
    participant Bot as RestauranteBot
    participant TG as Telegram API
    participant Drive as Google Drive
    actor Atendente

    Cliente->>TG: /start
    TG->>Bot: Update (text)
    Bot->>TG: SendMessage (menu inline)
    TG-->>Cliente: Botões

    Cliente->>TG: clica "Cardápio"
    TG->>Bot: CallbackQuery
    Bot->>Drive: GET cardapio.jpg (fallback local)
    Drive-->>Bot: imagem
    Bot->>TG: SendPhoto + pergunta marmita
    TG-->>Cliente: foto + botões

    loop Montagem marmita
        Cliente->>Bot: callback (tam_/opc_)
        Bot->>Bot: atualiza Pedido/Marmita
        Bot->>TG: próxima categoria
    end

    Cliente->>Bot: Finalizar + pagamento
    Bot->>TG: SendMessage (resumo + Pix)
    TG-->>Cliente: confirmação
    Bot->>TG: SendMessage ao CHAT_ID_ATENDENTE
    TG-->>Atendente: resumo do pedido

    Note over Atendente,Cliente: Modo relay (atendimento humano)
    Atendente->>TG: /atender <chatId> ou reply
    TG->>Bot: Update
    Bot->>Bot: sessaoAtiva[atendente]=cliente
    Atendente->>Bot: texto/foto
    Bot->>TG: envia ao cliente como "👤 Atendente"
    Cliente->>Bot: texto/foto
    Bot->>TG: encaminha ao atendente com header
```
