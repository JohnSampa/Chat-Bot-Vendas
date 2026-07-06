# Resumo das mudancas do chatbot de vendas

## Sessao e contexto por CPF

- O backend agora recebe mensagens pelo objeto `ChatbotRequest` com `mensagem`, `cpf`, `clientSessionId` e `novaSessao`.
- O endpoint `POST /chatbot/iniciar` cria uma nova sessao pelo `clientSessionId`; CPF e opcional nesse momento.
- A sessao guarda `cpfCliente`, `clientSessionId`, `iniciadaEm`, `ativa` e `ultimaAtualizacao`.
- Ao iniciar uma nova conversa para o mesmo CPF, a sessao ativa anterior e marcada como inativa.
- O frontend nao solicita mais CPF por modal. Ele envia apenas um `clientSessionId` tecnico; o backend pede CPF dentro do chat somente quando a intencao exige compra, pedido, atualizacao, cancelamento ou consulta.
- Quando o CPF e solicitado, a mensagem original fica pendente na sessao e o fluxo continua automaticamente apos o usuario informar um CPF valido.

## Carrinho como objeto

- Foi criada a entidade `ItemCarrinhoSessao`.
- O carrinho deixou de depender de `carrinhoJson` e agora usa a tabela `item_carrinho_sessao_tb`, relacionada a `sessao_chat` e `produto_tb`.
- O tamanho escolhido pelo cliente agora fica no item do carrinho e no pedido, nao mais no cadastro do produto.
- Os campos JSON temporarios ainda existem para pendencias de ambiguidade durante a conversa.

## Tamanho das pecas

- `Produto` nao possui mais tamanho associado.
- A sessao ganhou o estado `AGUARDANDO_TAMANHO`.
- Depois de escolher o produto, o bot pede o tamanho antes da quantidade.
- Se o usuario informar quantidade enquanto o bot espera tamanho, como "quero 2", a quantidade e registrada e o bot continua pedindo o tamanho.
- Se o carrinho tiver um unico item e o usuario responder "quero 2" na confirmacao, a quantidade do item e atualizada antes da compra.
- Se o carrinho tiver um unico item e o usuario responder "quero duas camisas", o bot entende como ajuste de quantidade do item atual.
- Na confirmacao do carrinho, frases como "quero uma camisa do flamengo tambem" adicionam o novo produto ao pedido atual e iniciam a coleta de tamanho desse item.
- Na confirmacao do carrinho, frases como "adiciona uma camisa do flamengo" tambem sao tratadas como novo item especifico, sem listar todos os produtos.
- Pedidos genericos para adicionar item, como "adicionar novo item" ou "incluir outro produto", preservam o pedido atual e perguntam qual produto adicionar.
- O tamanho aceito pode ser grade por letra (`PP`, `P`, `M`, `G`, `GG`) ou numeracao (`38`, `40`, `42`, `44`).
- `SessaoChat`, `ItemCarrinhoSessao` e `Venda` agora salvam o tamanho escolhido.
- Os arquivos `produtos*.json` foram atualizados para remover o campo `tamanho`.

## Intencoes, setores e pesos

- `Intencao` e `PalavraChave` agora possuem `setor`.
- `PalavraChave` tambem possui `peso`.
- A deteccao de intencao primeiro calcula o setor dominante da mensagem e depois escolhe a intencao mais forte dentro desse setor.
- A normalizacao foi centralizada em `TextoUtils`, removendo acentos, pontuacao duplicada e espacos extras.
- A busca de produto ignora saudacoes/artigos como "oi" e "uma" e prioriza produtos que contenham todos os termos importantes, evitando trocar "Camisa Corinthians" por outro time.

## Validacao e ambiguidade

- Frases nao entendidas retornam `TipoResposta.ERRO` com mensagem orientativa.
- Excecoes de negocio tambem retornam `ERRO`.
- O parser trata "comparar" como erro comum de digitacao para "comprar", permitindo frases como "quero comparar 100 camisa do corinthians".
- Durante uma ambiguidade de produto, o bot agora reconhece uma nova frase de compra, como "quero comprar uma camisa do corinthians", e troca o fluxo em vez de insistir no id anterior.
- Comandos de cancelamento agora cancelam a compra mesmo quando o bot esta aguardando produto, tamanho, quantidade ou resolucao de ambiguidade.
- O fluxo de itens ambiguos continua pedindo ao usuario o produto correto por id.
- O fluxo de atualizacao permite escolher um item do carrinho e trocar por outro produto ou remover o item.

## Pedidos e vendas

- Vendas finalizadas agora guardam `cpfCliente` e `status`.
- O bot reconhece frases como "gostaria de continuar meu pedido", solicita CPF quando necessario e retoma o pedido em aberto encontrado para esse cliente.
- Foram adicionados endpoints CRUD:
  - `POST /chatbot/pedidos/{cpf}`
  - `GET /chatbot/pedidos/{cpf}`
  - `PUT /chatbot/pedidos/{cpf}/{pedidoId}`
  - `DELETE /chatbot/pedidos/{cpf}/{pedidoId}`
- O delete marca o pedido como `CANCELADO`.

## Arquivos auxiliares

- `novas-intencoes-chatbot.json`: array pronto para enviar em `POST /chatbot/intencao`, agora com 24 intencoes e 528 palavras-chave com `setor` e `peso`.
- `produtos-camisas-times-brasileiros.json`: array pronto para enviar em `POST /chatbot/produtos`, com nomes de clubes no campo `nome` para permitir mensagens como "Quero uma camisa do flamengo".
- `produtos-calcas-shorts-bermudas.json`: array pronto para enviar em `POST /chatbot/produtos`, com 45 produtos de calcas, shorts, bermudas e shorts de times.
- Os arquivos antigos de intencoes foram removidos para evitar cargas duplicadas ou desatualizadas.
- `sql-atualizacao-chatbot-robusto.sql`: atualizado com colunas novas, estados novos, tabela de carrinho e indices por CPF. O script agora remove constraints antigas de `sessao_chat.estado` antes de recriar a lista de estados valida.
- `sql-correcao-estado-sessao-cpf.sql`: correcao curta para bancos ja criados que rejeitam `AGUARDANDO_CPF` ou `AGUARDANDO_TAMANHO`.
- `sql-correcao-tipo-acao-continuar-pedido.sql`: correcao curta para bancos que rejeitam a nova acao `CONTINUAR_PEDIDO` em `intencao_tb.tipo_acao`.

## Validacao

- `mvn -DskipTests compile`: executado com Maven do cache local e finalizado com sucesso apos a inclusao da acao de short/bermuda.
- `mvn -DskipTests compile`: executado novamente com sucesso apos mover o tamanho do produto para sessao/carrinho/venda.
- `mvn -DskipTests compile`: executado novamente com sucesso apos mover a solicitacao de CPF para dentro do fluxo do chat.
- `mvn test`: iniciou, mas falhou ao subir o contexto Spring porque `DATABASE_URL` nao esta configurada no ambiente de teste.
