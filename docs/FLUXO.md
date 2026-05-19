# Prompt atualizado — Chatbot de restaurante (Telegram)

## Contexto geral
Projeto Java/Maven de um chatbot Telegram para restaurante (`com.restaurantebot`), usando `telegrambots` (long polling). O fluxo é todo guiado por **botões inline** — o cliente quase nunca digita texto livre, exceto para endereço/nome em entrega/retirada e durante atendimento humano. Cada cliente tem **estado e carrinho próprios**, indexados por `chatId`, garantindo isolamento.

## 1. Menu principal
Disparado por `/start`, saudações (`oi`, `olá`, `bom dia`, `menu`, etc. — conjunto em `RestauranteBot.SAUDACOES`) ou pelo botão "Voltar ao menu".

Mensagem:
> "Olá! Seja bem-vindo ao atendimento do restaurante. Como podemos te ajudar hoje?"

Botões (inline keyboard, um por linha):
- 1 - Ver endereço (`endereco`)
- 2 - Ver horários (`horarios`)
- 3 - Ver cardápio (`cardapio`)
- 4 - Falar com atendente (`atendente`)

**Importante:** ao voltar ao menu, `limparEstadoCliente(chatId)` é chamado para evitar mistura de fluxos.

## 2. Endereço
Edita a mensagem do menu mostrando `Config.ENDERECO` + link `Config.LINK_MAPS` (Markdown) e envia botão "Voltar ao menu".

## 3. Horários
Edita a mensagem mostrando `Config.HORARIOS` e envia botão "Voltar ao menu".

## 4. Falar com atendente (modo relay bidirecional)

Não é one-shot. É um **modo de chat encaminhado** entre dois Telegrams:

**Lado cliente:**
- Cliente entra em `EstadoConversa.EM_ATENDIMENTO`.
- Toda mensagem (texto e foto) que ele enviar é encaminhada ao chat de `Config.CHAT_ID_ATENDENTE` com cabeçalho: `📩 De <nome> (@<username>) — chatId <chatId>: <mensagem>`.
- `/start` e `/sair` são ignorados nesse modo — só o atendente pode encerrar.

**Lado atendente:**
- Recebe notificação inicial com nome, username, chatId e instruções.
- Dois caminhos para responder:
  1. **Sessão**: `/atender <chatId>` → daí em diante todo texto vai para aquele cliente como `👤 Atendente: <mensagem>`. Encerra com `/encerrar <chatId>` ou `/encerrar` (sem argumento usa sessão ativa).
  2. **Reply**: responder diretamente a uma notificação do bot encaminha aquela mensagem ao cliente correspondente, sem precisar abrir sessão.
- Fotos também são repassadas nas duas direções (`repassarFotoAtendenteParaCliente` / `repassarFotoClienteParaAtendente`).

**Estruturas internas:**
- `notificacaoParaCliente: Map<Integer, Long>` — messageId no chat do atendente → chatId do cliente, usado para resolver replies.
- `sessaoAtiva: Map<Long, Long>` — userId do atendente → chatId do cliente em sessão.

## 5. Ver cardápio
1. `CardapioService.buscarImagemCardapio()` retorna `InputFile`:
   - Se `Config.DRIVE_CARDAPIO_FILE_ID` está preenchido, baixa de `https://drive.google.com/uc?export=download&id=<id>`.
   - Senão, usa fallback local `Config.CARDAPIO_LOCAL_PATH`.
2. Envia a foto com caption: `"Aqui está o cardápio.\n\nDeseja montar uma marmita?"`.
3. Botões: "Sim, montar marmita" / "Não".
4. "Não" → menu principal. "Sim" → fluxo de montagem.

## 6. Montagem de marmita (NÃO é seleção por ID — é guiada por categoria)

Em vez de uma lista de itens prontos, o cliente **monta** cada marmita passo a passo. A `Marmita` atual fica em `Pedido.marmitaAtual` durante a montagem.

**Sequência (`Categoria.proximaCategoriaPendente()`):**
1. **Tamanho** (`buscarTamanhosMarmita`): pequena R$18 / média R$22 / grande R$28 + botão "Cancelar".
2. **Arroz**: branco / temperado + "Não quero esse item".
3. **Feijão**: tropeiro / caldo / baião.
4. **Macarrão**: alho e óleo.
5. **Legumes**: repolho c/cenoura / salada crua / beterraba / batata doce / macaxeira.
6. **Carnes**: frango assado / linguiça / cupim / galinha cozida / milanesa / parmegiana (+R$2).
7. **Adicionais**: farofa / bolinho de arroz / batata frita (+R$5) / ovo frito (+R$2).

Cada categoria mostra suas opções como botões (`opc_<idx>`) + "Não quero esse item" (`opc_skip`). As opções e preços extras vêm do mapa estático `CardapioService.OPCOES`.

Ao terminar, exibe resumo da marmita adicionada e pergunta: **"Deseja adicionar outra marmita?"** (Sim / Não).

## 7. Bebidas (sub-fluxo após marmitas)
Após "Não, continuar" nas marmitas:
1. Pergunta "Deseja adicionar bebidas ao pedido?" (Sim / Não).
2. Se Sim: lista botões com bebidas de `buscarBebidas()` (refrigerante lata R$6 / refri 1L R$10 / suco R$8 / água R$3) + "Não quero bebida".
3. Após adicionar, pergunta "Deseja adicionar outra bebida?" (Sim / Não).
4. Não → resumo.

## 8. Resumo do pedido e ações
`gerarResumoPedido` mostra `Pedido.gerarResumoCliente()` (marmitas numeradas + bebidas + total) e três botões:
- **Remover item** → lista cada marmita e bebida com botão de remoção individual (`rm_marmita_<idx>` / `rm_bebida_<idx>`) + "Voltar ao resumo". Após remover, recalcula e mostra resumo. Se carrinho ficar vazio, volta para "Deseja montar uma marmita?".
- **Finalizar pedido** → fluxo de recebimento.
- **Cancelar pedido** → `limparEstadoCliente` + menu principal.

> **Nota:** não há "Deseja continuar com a compra? Sim/Não". Foi substituído pelo trio remover/finalizar/cancelar.

## 9. Tipo de recebimento
Botões na mesma linha: "Entrega" / "Retirada no restaurante".

## 10. Entrega
1. `AGUARDANDO_ENDERECO`: texto livre. Mensagem com exemplo `"Rua João Silva, 123 - Bairro - Cidade"`.
2. `AGUARDANDO_NOME_ENTREGA`: texto livre.
3. Forma de pagamento (botões).

## 11. Retirada
1. `AGUARDANDO_NOME_RETIRADA`: texto livre.
2. Forma de pagamento (botões).

## 12. Pagamento (`coletarFormaPagamento`)
Botões: 1 - Crédito (`pag_credito`) / 2 - Débito (`pag_debito`) / 3 - Pix (`pag_pix`).

Em `finalizarPedido`:
- **Pix** → envia ao cliente `"Chave Pix fictícia para pagamento: " + Config.CHAVE_PIX_FICTICIA`. Status: `"Pix fictício gerado"`.
- **Crédito/Débito** → mensagem `"O pagamento será realizado no momento da entrega/retirada."`. Status varia conforme tipo.

## 13. Resumo final ao atendente (`montarResumoFinal`)
Enviado em Markdown ao `Config.CHAT_ID_ATENDENTE`:
```
*Novo pedido gerado*

Cliente: <firstName>
Username: @<username>      (só se houver)
Chat ID: <chatId>

<resumo de cada marmita numerada com tamanho + escolhas por categoria + valor>

*Bebidas:*               (só se houver)
- <nome> - R$ X,XX

*Valor total:* R$ X,XX

Tipo de recebimento: Entrega | Retirada no restaurante
Endereço: <...>           (entrega)
Nome de quem vai receber: <...>   (entrega)
Nome de quem irá retirar: <...>   (retirada)
Forma de pagamento: <...>
Status do pagamento: <...>
```

O messageId desse resumo entra em `notificacaoParaCliente` para que o atendente possa responder com reply.

## 14. Mensagem final ao cliente
> "Pedido registrado com sucesso! Um atendente dará continuidade ao seu atendimento."

Depois: `limparEstadoCliente` + envia menu principal.

## 15. Estados de conversa (`EstadoConversa`)
`INICIO`, `AGUARDANDO_INICIAR_MARMITA`, `AGUARDANDO_TAMANHO_MARMITA`, `AGUARDANDO_OPCAO_CATEGORIA`, `AGUARDANDO_OUTRA_MARMITA`, `AGUARDANDO_INICIAR_BEBIDA`, `AGUARDANDO_ESCOLHA_BEBIDA`, `AGUARDANDO_OUTRA_BEBIDA`, `AGUARDANDO_ACAO_RESUMO`, `AGUARDANDO_REMOCAO_ITEM`, `AGUARDANDO_TIPO_RECEBIMENTO`, `AGUARDANDO_ENDERECO`, `AGUARDANDO_NOME_ENTREGA`, `AGUARDANDO_NOME_RETIRADA`, `AGUARDANDO_PAGAMENTO`, `EM_ATENDIMENTO`.

## 16. Pontos de configuração (`Config.java`)
- `BOT_TOKEN`, `BOT_USERNAME` — credenciais do bot.
- `CHAT_ID_ATENDENTE` — chat/grupo que recebe solicitações de atendimento e resumos de pedido. **Se vazio, atendimento humano fica indisponível** e o cliente é avisado.
- `ENDERECO`, `LINK_MAPS`, `HORARIOS` — dados estáticos do restaurante.
- `DRIVE_CARDAPIO_FILE_ID` — ID público do arquivo do Drive (entre `/d/` e `/view`). Vazio = usa fallback local.
- `CARDAPIO_LOCAL_PATH` — caminho do JPEG local (fallback).
- `CHAVE_PIX_FICTICIA` — string exibida ao cliente que escolhe Pix.

## 17. Catálogo (`CardapioService.java`)
Toda mudança de itens, preços ou opções é feita em um único arquivo:
- `buscarTamanhosMarmita()` — tamanhos e preços base.
- mapa estático `OPCOES` — opções de cada `Categoria` e valores extras.
- `buscarBebidas()` — bebidas e preços.
- `buscarImagemCardapio()` — integração Drive + fallback local.

Quando houver API externa de catálogo, a integração entra **só aqui**.

## 18. Diferenças importantes versus o prompt original
1. **Seleção é por botão, não por ID digitado.** Toda escolha de tamanho/categoria/bebida/pagamento usa callback inline (`tam_*`, `opc_*`, `beb_*`, `pag_*`, `rm_*`). Não há validação de "ID digitado" porque não há entrada numérica.
2. **Cardápio é montagem guiada de marmita**, não uma lista linear de produtos. O carrinho contém `Marmita` (com tamanho + escolhas por categoria) + `List<ItemCardapio>` de bebidas.
3. **Atendimento humano é relay bidirecional persistente** (sessões `/atender`/`/encerrar`, reply-to-message, fotos nos dois sentidos), não apenas uma notificação one-shot.
4. **No resumo final**, ações são *remover item / finalizar / cancelar*, não "continuar compra Sim/Não".
5. **Bebidas são um sub-fluxo separado**, oferecido após terminar as marmitas.
6. Sub-fluxos não pré-vistos no prompt: remoção individual de itens, detecção de saudações como gatilho de menu, fallback local quando Drive falha.
