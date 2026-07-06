package unifan.chat_bot_vendas.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import unifan.chat_bot_vendas.domain.Intencao;
import unifan.chat_bot_vendas.domain.Produto;
import unifan.chat_bot_vendas.domain.SessaoChat;
import unifan.chat_bot_vendas.dto.CarrinhoResponse;
import unifan.chat_bot_vendas.dto.ChatbotRequest;
import unifan.chat_bot_vendas.dto.ChatbotResponse;
import unifan.chat_bot_vendas.dto.ItemCarrinho;
import unifan.chat_bot_vendas.dto.ItemPedidoExtraido;
import unifan.chat_bot_vendas.dto.ItemPendente;
import unifan.chat_bot_vendas.dto.ItemTamanhoPendente;
import unifan.chat_bot_vendas.dto.enums.TipoResposta;
import unifan.chat_bot_vendas.exceptions.BusinessException;
import unifan.chat_bot_vendas.repositories.SessaoChatRepository;
import unifan.chat_bot_vendas.utils.TextoUtils;
import unifan.chat_bot_vendas.domain.enums.TipoAcao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static unifan.chat_bot_vendas.domain.enums.EstadoSessao.AGUARDANDO_CONFIRMACAO_CARRINHO;
import static unifan.chat_bot_vendas.domain.enums.EstadoSessao.AGUARDANDO_CONFIRMACAO_COMPRA;
import static unifan.chat_bot_vendas.domain.enums.EstadoSessao.AGUARDANDO_CONFIRMACAO_PRODUTO;
import static unifan.chat_bot_vendas.domain.enums.EstadoSessao.AGUARDANDO_CPF;
import static unifan.chat_bot_vendas.domain.enums.EstadoSessao.AGUARDANDO_DADOS_PAGAMENTO;
import static unifan.chat_bot_vendas.domain.enums.EstadoSessao.AGUARDANDO_FORMA_PAGAMENTO;
import static unifan.chat_bot_vendas.domain.enums.EstadoSessao.AGUARDANDO_ITEM_ATUALIZACAO;
import static unifan.chat_bot_vendas.domain.enums.EstadoSessao.AGUARDANDO_NOVO_PRODUTO_ATUALIZACAO;
import static unifan.chat_bot_vendas.domain.enums.EstadoSessao.AGUARDANDO_PRODUTO;
import static unifan.chat_bot_vendas.domain.enums.EstadoSessao.AGUARDANDO_QUANTIDADE;
import static unifan.chat_bot_vendas.domain.enums.EstadoSessao.AGUARDANDO_RESOLUCAO_ITEM;
import static unifan.chat_bot_vendas.domain.enums.EstadoSessao.AGUARDANDO_TAMANHO;
import static unifan.chat_bot_vendas.domain.enums.EstadoSessao.INICIAL;
import static unifan.chat_bot_vendas.domain.enums.EstadoSessao.SAIR_COMPRA;
import static unifan.chat_bot_vendas.domain.enums.TipoAcao.SAIR_CHAT_VENDA;

@Service
public class ChatBotService {

    @Autowired
    private ProdutoService produtoService;

    @Autowired
    private VendaService vendaService;

    @Autowired
    private IntencaoService intencaoService;

    @Autowired
    private SessaoChatRepository sessaoRepository;

    @Autowired
    private ChatBotComandosService chatBotComandosService;

    @Autowired
    private PedidoParserService pedidoParserService;

    @Autowired
    private CarrinhoService carrinhoService;

    public ChatbotResponse reiniciarConversa() {
        return reiniciarConversa(new ChatbotRequest(null, null, "api-default", true));
    }

    public ChatbotResponse reiniciarConversa(String cpfCliente) {
        return iniciarNovaSessao(new ChatbotRequest(null, cpfCliente, "api-default", true));
    }

    public ChatbotResponse reiniciarConversa(ChatbotRequest request) {
        return iniciarNovaSessao(request);
    }

    public ChatbotResponse iniciarNovaSessao(String cpfCliente) {
        return iniciarNovaSessao(new ChatbotRequest(null, cpfCliente, "api-default", true));
    }

    public ChatbotResponse iniciarNovaSessao(ChatbotRequest request) {
        String cpf = normalizarCpf(request == null ? null : request.cpf());
        String clientSessionId = normalizarClientSessionId(request == null ? null : request.clientSessionId());

        sessaoRepository.findFirstByClientSessionIdAndAtivaTrueOrderByIdDesc(clientSessionId)
                .ifPresent(this::desativarSessao);

        if (cpfValido(cpf)) {
            sessaoRepository.findFirstByCpfClienteAndAtivaTrueOrderByIdDesc(cpf)
                    .ifPresent(this::desativarSessao);
        }

        SessaoChat sessao = sessaoRepository.save(novaSessao(cpfValido(cpf) ? cpf : null, clientSessionId));
        carrinhoService.limpar(sessao);
        sessaoRepository.save(sessao);

        return ChatbotResponse.mensagem("Conversa iniciada. Como posso ajudar?");
    }

    public ChatbotResponse processarMensagem(String mensagem) {
        return processarMensagem(new ChatbotRequest(mensagem, null, "api-default", false));
    }

    public ChatbotResponse processarMensagem(ChatbotRequest request) {
        String mensagem = request == null ? "" : request.mensagem();
        String cpf = normalizarCpf(request == null ? null : request.cpf());
        String clientSessionId = normalizarClientSessionId(request == null ? null : request.clientSessionId());

        if (Boolean.TRUE.equals(request == null ? null : request.novaSessao())) {
            return iniciarNovaSessao(request);
        }

        SessaoChat sessao = obterOuCriarSessao(cpf, clientSessionId);
        if (cpfValido(cpf) && sessao.getCpfCliente() == null) {
            sessao.setCpfCliente(cpf);
            salvarSessao(sessao);
        }

        if (sessao.getEstado() == AGUARDANDO_CPF) {
            return resolverCpf(sessao, mensagem, clientSessionId);
        }

        String msg = normalizar(mensagem);

        if (sessao.getEstado() != INICIAL) {
            return continuarSessao(sessao, mensagem == null ? "" : mensagem.toLowerCase().trim());
        }

        List<ItemPedidoExtraido> itensExtraidos = pedidoParserService.extrairItens(mensagem);
        if (!itensExtraidos.isEmpty()) {
            if (!cpfValido(sessao.getCpfCliente())) {
                return solicitarCpf(sessao, mensagem);
            }
            return iniciarPedidoComItens(sessao, itensExtraidos);
        }

        if (isContinuarPedido(msg)) {
            if (!cpfValido(sessao.getCpfCliente())) {
                return solicitarCpf(sessao, mensagem);
            }
            return chatBotComandosService.continuarPedido(sessao);
        }

        Intencao intencao = intencaoService.detectarIntencao(msg);
        if (intencao == null) {
            return new ChatbotResponse(
                    TipoResposta.ERRO,
                    "Desculpe, nao entendi. Tente informar se deseja comprar, alterar, cancelar ou consultar pedidos.",
                    null,
                    null
            );
        }

        if (precisaCpfParaContexto(intencao) && !cpfValido(sessao.getCpfCliente())) {
            return solicitarCpf(sessao, mensagem);
        }

        if (intencao.getTipoAcao() != null) {
            return chatBotComandosService.executarComandos(intencao, sessao);
        }

        return ChatbotResponse.mensagem(intencao.getResposta());
    }

    private SessaoChat obterOuCriarSessao(String cpf, String clientSessionId) {
        return sessaoRepository.findFirstByClientSessionIdAndAtivaTrueOrderByIdDesc(clientSessionId)
                .or(() -> cpfValido(cpf) ? sessaoRepository.findFirstByCpfClienteAndAtivaTrueOrderByIdDesc(cpf) : java.util.Optional.empty())
                .orElseGet(() -> sessaoRepository.save(novaSessao(cpfValido(cpf) ? cpf : null, clientSessionId)));
    }

    private SessaoChat novaSessao(String cpf, String clientSessionId) {
        SessaoChat sessao = new SessaoChat();
        sessao.setEstado(INICIAL);
        sessao.setProduto(null);
        sessao.setQuantidade(null);
        sessao.setTamanho(null);
        sessao.setTipoProdutoInteresse(null);
        sessao.setFormaPagamento(null);
        sessao.setDadosPagamento(null);
        sessao.setCpfCliente(cpf);
        sessao.setClientSessionId(clientSessionId);
        sessao.setUserid(1L);
        sessao.setIniciadaEm(LocalDateTime.now());
        sessao.setUltimaAtualizacao(LocalDateTime.now());
        sessao.setAtiva(true);
        return sessao;
    }

    private ChatbotResponse solicitarCpf(SessaoChat sessao, String mensagemPendente) {
        sessao.setEstado(AGUARDANDO_CPF);
        sessao.setMensagemPendenteCpf(mensagemPendente);
        salvarSessao(sessao);
        return ChatbotResponse.mensagem("Para continuar com pedido, compra ou consulta, informe o CPF do cliente.");
    }

    private ChatbotResponse resolverCpf(SessaoChat sessao, String mensagem, String clientSessionId) {
        String cpf = normalizarCpf(mensagem);
        if (!cpfValido(cpf)) {
            return new ChatbotResponse(
                    TipoResposta.ERRO,
                    "CPF invalido. Informe um CPF com 11 digitos para continuar.",
                    null,
                    null
            );
        }

        String mensagemPendente = sessao.getMensagemPendenteCpf();
        sessao.setCpfCliente(cpf);
        sessao.setEstado(INICIAL);
        sessao.setMensagemPendenteCpf(null);
        salvarSessao(sessao);

        if (mensagemPendente == null || mensagemPendente.isBlank()) {
            return ChatbotResponse.mensagem("CPF registrado. Como posso ajudar?");
        }

        return processarMensagem(new ChatbotRequest(mensagemPendente, cpf, clientSessionId, false));
    }

    private boolean precisaCpfParaContexto(Intencao intencao) {
        if (intencao == null || intencao.getTipoAcao() == null) {
            return false;
        }

        TipoAcao tipoAcao = intencao.getTipoAcao();
        return tipoAcao == TipoAcao.INICIAR_COMPRA
                || tipoAcao == TipoAcao.COMPRAR_CAMISA
                || tipoAcao == TipoAcao.COMPRAR_CALCA
                || tipoAcao == TipoAcao.COMPRAR_SHORT_BERMUDA
                || tipoAcao == TipoAcao.COMPRAR_BLUSA
                || tipoAcao == TipoAcao.COMPRAR_SAPATO
                || tipoAcao == TipoAcao.VERIFICAR_VENDAS
                || tipoAcao == TipoAcao.CONTINUAR_PEDIDO
                || tipoAcao == TipoAcao.ATUALIZAR_PEDIDO
                || tipoAcao == TipoAcao.DELETAR_PEDIDO;
    }

    private void desativarSessao(SessaoChat sessao) {
        sessao.setAtiva(false);
        sessao.setUltimaAtualizacao(LocalDateTime.now());
        sessaoRepository.save(sessao);
    }

    private ChatbotResponse continuarSessao(final SessaoChat sessao, String mensagem) {
        if (isEstadoConfirmacaoFinal(sessao) && isRespostaNegativaParaConfirmacao(mensagem)) {
            return cancelarCompra(sessao);
        }

        if (isCancelamento(mensagem)) {
            return cancelarCompra(sessao);
        }

        ChatbotResponse respostaAdicionarItem = prepararAdicaoGenericaDeItem(sessao, mensagem);
        if (respostaAdicionarItem != null) {
            return respostaAdicionarItem;
        }

        Intencao intencao = intencaoService.detectarIntencao(mensagem);
        if (intencao != null && intencao.getTipoAcao() == SAIR_CHAT_VENDA) {
            return chatBotComandosService.executarComandos(intencao, sessao);
        }

        return switch (sessao.getEstado()) {
            case AGUARDANDO_PRODUTO -> resolverProduto(sessao, mensagem);
            case AGUARDANDO_CONFIRMACAO_PRODUTO -> resolverConfirmacaoProduto(sessao, mensagem);
            case AGUARDANDO_RESOLUCAO_ITEM -> resolverItemPendente(sessao, mensagem);
            case AGUARDANDO_CONFIRMACAO_CARRINHO -> resolverConfirmacaoCarrinho(sessao, mensagem);
            case AGUARDANDO_TAMANHO -> resolverTamanho(sessao, mensagem);
            case AGUARDANDO_QUANTIDADE -> resolverQuantidade(sessao, mensagem);
            case AGUARDANDO_CONFIRMACAO_COMPRA -> resolverConfirmacaoCompra(sessao, mensagem);
            case AGUARDANDO_ITEM_ATUALIZACAO -> resolverItemAtualizacao(sessao, mensagem);
            case AGUARDANDO_NOVO_PRODUTO_ATUALIZACAO -> resolverNovoProdutoAtualizacao(sessao, mensagem);
            case AGUARDANDO_FORMA_PAGAMENTO -> resolverFormaPagamento(sessao, mensagem);
            case AGUARDANDO_DADOS_PAGAMENTO -> resolverDadosPagamento(sessao, mensagem);
            case SAIR_COMPRA -> {
                limparSessao(sessao);
                sessaoRepository.save(sessao);
                yield ChatbotResponse.mensagem("Tudo bem, compra cancelada.");
            }
            default -> reiniciarEstadoInvalido(sessao);
        };
    }

    private ChatbotResponse resolverProduto(SessaoChat sessao, String mensagem) {
        Produto produto = produtoService.buscarProdutoPorIdMensagem(mensagem, sessao.getTipoProdutoInteresse());
        if (produto != null) {
            return iniciarTamanho(sessao, produto, null);
        }

        List<ItemPedidoExtraido> itensExtraidos = pedidoParserService.extrairItens(mensagem);
        if (!itensExtraidos.isEmpty()) {
            return iniciarPedidoComItens(sessao, itensExtraidos);
        }

        produto = produtoService.buscarProdutoPorNomeExato(mensagem, sessao.getTipoProdutoInteresse());
        if (produto != null) {
            return iniciarTamanho(sessao, produto, null);
        }

        String tipoProdutoInteresse = produtoService.detectarTipoProdutoInteresse(mensagem);
        if (tipoProdutoInteresse != null) {
            sessao.setProduto(null);
            sessao.setQuantidade(null);
            sessao.setTamanho(null);
            sessao.setTipoProdutoInteresse(tipoProdutoInteresse);
            salvarSessao(sessao);

            return ChatbotResponse.listaMensagem(
                    TipoResposta.LISTA_PRODUTOS_MENSAGEM,
                    buscarProdutosContexto(sessao),
                    "Encontrei estas opcoes de " + tipoProdutoInteresse + ". Qual o id do produto que deseja comprar?"
            );
        }

        produto = produtoService.buscarProdutoMaisProximoPorMensagem(mensagem, sessao.getTipoProdutoInteresse());
        if (produto == null) {
            salvarSessao(sessao);
            return ChatbotResponse.listaMensagem(
                    TipoResposta.LISTA_PRODUTOS_MENSAGEM,
                    buscarProdutosContexto(sessao),
                    "Produto nao encontrado. Informe o id de um dos produtos da lista ou digite o nome mais proximo."
            );
        }

        sessao.setProduto(produto);
        sessao.setEstado(AGUARDANDO_CONFIRMACAO_PRODUTO);
        salvarSessao(sessao);

        return new ChatbotResponse(
                TipoResposta.PRODUTO_MENSAGEM,
                "Encontrei este produto. E esse o item que deseja comprar? Responda sim ou nao.",
                null,
                produto
        );
    }

    private ChatbotResponse resolverConfirmacaoProduto(SessaoChat sessao, String mensagem) {
        if (isConfirmacao(mensagem)) {
            return iniciarTamanho(sessao, sessao.getProduto(), null);
        }

        if (isNegacao(mensagem) || mensagem.contains("outro") || mensagem.contains("errado")) {
            sessao.setProduto(null);
            sessao.setTamanho(null);
            sessao.setEstado(AGUARDANDO_PRODUTO);
            salvarSessao(sessao);

            return ChatbotResponse.listaMensagem(
                    TipoResposta.LISTA_PRODUTOS_MENSAGEM,
                    buscarProdutosContexto(sessao),
                    "Tudo bem. Informe o id do produto da lista ou digite um nome mais especifico."
            );
        }

        salvarSessao(sessao);
        return new ChatbotResponse(
                TipoResposta.PRODUTO_MENSAGEM,
                "Confirme se este e o item que deseja comprar. Responda sim ou nao.",
                null,
                sessao.getProduto()
        );
    }

    private ChatbotResponse resolverTamanho(SessaoChat sessao, String mensagem) {
        String tamanho = detectarTamanho(mensagem);
        if (tamanho == null) {
            Integer quantidade = extrairQuantidade(mensagem);
            if (quantidade != null) {
                sessao.setQuantidade(quantidade);
                salvarSessao(sessao);
                return new ChatbotResponse(
                        TipoResposta.PRODUTO_MENSAGEM,
                        "Quantidade " + quantidade + " registrada. Agora informe o tamanho deste item: PP, P, M, G, GG ou numeracao como 38, 40, 42, 44.",
                        null,
                        sessao.getProduto()
                );
            }

            salvarSessao(sessao);
            return new ChatbotResponse(
                    TipoResposta.ERRO,
                    "Informe um tamanho valido, como PP, P, M, G, GG ou numeracao 38, 40, 42, 44.",
                    null,
                    sessao.getProduto()
            );
        }

        sessao.setTamanho(tamanho);

        if (sessao.getQuantidade() != null && sessao.getQuantidade() > 0) {
            carrinhoService.adicionarItem(sessao, sessao.getProduto(), sessao.getQuantidade(), tamanho);
            sessao.setProduto(null);
            sessao.setQuantidade(null);
            sessao.setTamanho(null);
            return continuarItensPendentesAposTamanho(sessao);
        }

        sessao.setEstado(AGUARDANDO_QUANTIDADE);
        salvarSessao(sessao);

        return new ChatbotResponse(
                TipoResposta.PRODUTO_MENSAGEM,
                "Tamanho " + tamanho + " selecionado. Qual a quantidade deseja?",
                null,
                sessao.getProduto()
        );
    }

    private ChatbotResponse resolverConfirmacaoCarrinho(SessaoChat sessao, String mensagem) {
        if (deveAtualizarQuantidadeDoItemAtual(sessao, mensagem)) {
            ChatbotResponse respostaQuantidade = atualizarQuantidadeCarrinho(sessao, mensagem);
            if (respostaQuantidade != null) {
                return respostaQuantidade;
            }
        }

        ChatbotResponse respostaNovoItem = adicionarItensInformados(sessao, mensagem, false);
        if (respostaNovoItem != null) {
            return respostaNovoItem;
        }

        if (isConfirmacao(mensagem)) {
            return iniciarPagamento(sessao);
        }

        if (isCancelamento(mensagem) || isRespostaNegativaParaConfirmacao(mensagem)) {
            return cancelarCompra(sessao);
        }

        if (isAdicionarMais(mensagem)) {
            sessao.setEstado(AGUARDANDO_PRODUTO);
            sessao.setProduto(null);
            sessao.setQuantidade(null);
            sessao.setTamanho(null);
            sessao.setTipoProdutoInteresse(null);
            salvarSessao(sessao);

            return ChatbotResponse.listaMensagem(
                    TipoResposta.LISTA_PRODUTOS_MENSAGEM,
                    produtoService.buscarProdutos(),
                    "Certo, vamos adicionar mais itens. Qual o id do proximo produto?"
            );
        }

        if (isAtualizacaoPedido(mensagem)) {
            sessao.setEstado(AGUARDANDO_ITEM_ATUALIZACAO);
            salvarSessao(sessao);
            return respostaCarrinho(sessao, "Informe o id ou nome do item do pedido que deseja trocar ou remover.");
        }

        ChatbotResponse respostaQuantidade = atualizarQuantidadeCarrinho(sessao, mensagem);
        if (respostaQuantidade != null) {
            return respostaQuantidade;
        }

        salvarSessao(sessao);
        return respostaCarrinho(sessao, "Confirma a compra dos itens do carrinho? Responda sim ou nao.");
    }

    private ChatbotResponse resolverQuantidade(SessaoChat sessao, String mensagem) {
        try {
            int qty = Integer.parseInt(mensagem.replaceAll("[^0-9]", ""));
            if (qty <= 0) {
                throw new BusinessException("Insira uma quantidade maior que zero");
            }
            sessao.setEstado(AGUARDANDO_CONFIRMACAO_COMPRA);
            sessao.setQuantidade(qty);
            salvarSessao(sessao);

            return new ChatbotResponse(
                    TipoResposta.PRODUTO_MENSAGEM,
                    "Confirma a compra de " + qty + " unidade(s) no tamanho " + sessao.getTamanho() + "?",
                    null,
                    sessao.getProduto()
            );
        } catch (NumberFormatException e) {
            salvarSessao(sessao);
            return new ChatbotResponse(
                    TipoResposta.ERRO,
                    "Por favor, informe apenas o numero de unidades.",
                    null,
                    null
            );
        }
    }

    private ChatbotResponse resolverConfirmacaoCompra(SessaoChat sessao, String mensagem) {
        if (deveAtualizarQuantidadeDoProdutoAtual(sessao, mensagem)) {
            Integer quantidade = extrairQuantidade(mensagem);
            sessao.setQuantidade(quantidade);
            salvarSessao(sessao);
            return new ChatbotResponse(
                    TipoResposta.PRODUTO_MENSAGEM,
                    "Atualizei a quantidade para " + quantidade + " unidade(s). Confirma a compra?",
                    null,
                    sessao.getProduto()
            );
        }

        ChatbotResponse respostaNovoItem = adicionarItensInformados(sessao, mensagem, true);
        if (respostaNovoItem != null) {
            return respostaNovoItem;
        }

        if (isConfirmacao(mensagem)) {
            if (!carrinhoService.getItens(sessao).isEmpty()) {
                carrinhoService.adicionarItem(sessao, sessao.getProduto(), sessao.getQuantidade(), sessao.getTamanho());
            }
            return iniciarPagamento(sessao);
        }

        if (isAdicionarMais(mensagem)) {
            carrinhoService.adicionarItem(sessao, sessao.getProduto(), sessao.getQuantidade(), sessao.getTamanho());
            sessao.setEstado(AGUARDANDO_PRODUTO);
            sessao.setProduto(null);
            sessao.setQuantidade(null);
            sessao.setTamanho(null);
            sessao.setTipoProdutoInteresse(null);
            salvarSessao(sessao);

            return ChatbotResponse.listaMensagem(
                    TipoResposta.LISTA_PRODUTOS_MENSAGEM,
                    produtoService.buscarProdutos(),
                    "Adicionei esse item ao pedido. Qual o id do proximo produto?"
            );
        }

        if (isCancelamento(mensagem) || isRespostaNegativaParaConfirmacao(mensagem)) {
            return cancelarCompra(sessao);
        }

        Integer quantidade = extrairQuantidade(mensagem);
        if (quantidade != null) {
            sessao.setQuantidade(quantidade);
            salvarSessao(sessao);
            return new ChatbotResponse(
                    TipoResposta.PRODUTO_MENSAGEM,
                    "Atualizei a quantidade para " + quantidade + " unidade(s). Confirma a compra?",
                    null,
                    sessao.getProduto()
            );
        }

        salvarSessao(sessao);
        return new ChatbotResponse(
                TipoResposta.PRODUTO_MENSAGEM,
                "Responda sim para finalizar, comprar mais para adicionar outro produto, ou cancelar para encerrar.",
                null,
                sessao.getProduto()
        );
    }

    private ChatbotResponse resolverItemAtualizacao(SessaoChat sessao, String mensagem) {
        if (isCancelamento(mensagem)) {
            sessao.setEstado(AGUARDANDO_CONFIRMACAO_CARRINHO);
            salvarSessao(sessao);
            return respostaCarrinho(sessao, "Atualizacao cancelada. Confirma o pedido atual?");
        }

        Produto produto = selecionarProdutoCarrinho(sessao, mensagem);
        if (produto == null) {
            return respostaCarrinho(sessao, "Nao encontrei esse item no pedido. Informe o id ou nome do produto do carrinho.");
        }

        sessao.setProduto(produto);
        sessao.setEstado(AGUARDANDO_NOVO_PRODUTO_ATUALIZACAO);
        salvarSessao(sessao);

        return ChatbotResponse.listaMensagem(
                TipoResposta.LISTA_PRODUTOS_MENSAGEM,
                produtoService.buscarProdutos(),
                "Qual produto deve substituir " + produto.getNome() + "? Informe id/nome ou digite remover."
        );
    }

    private ChatbotResponse resolverNovoProdutoAtualizacao(SessaoChat sessao, String mensagem) {
        Produto produtoAtual = sessao.getProduto();
        if (produtoAtual == null) {
            sessao.setEstado(AGUARDANDO_ITEM_ATUALIZACAO);
            salvarSessao(sessao);
            return respostaCarrinho(sessao, "Informe novamente o item que deseja alterar.");
        }

        if (isRemocaoItem(mensagem)) {
            carrinhoService.removerProduto(sessao, produtoAtual);
            sessao.setProduto(null);
            sessao.setTamanho(null);
            return voltarParaCarrinhoDepoisAtualizacao(sessao, "Item removido do pedido.");
        }

        Produto novoProduto = produtoService.buscarProdutoPorIdMensagem(mensagem);
        if (novoProduto == null) {
            novoProduto = produtoService.buscarProdutoPorNomeExato(mensagem, null);
        }
        if (novoProduto == null) {
            novoProduto = produtoService.buscarProdutoMaisProximoPorMensagem(mensagem, null);
        }
        if (novoProduto == null) {
            return ChatbotResponse.listaMensagem(
                    TipoResposta.LISTA_PRODUTOS_MENSAGEM,
                    produtoService.buscarProdutos(),
                    "Nao encontrei o novo produto. Informe um id ou nome valido."
            );
        }

        carrinhoService.atualizarProduto(sessao, produtoAtual, novoProduto);
        sessao.setProduto(null);
        sessao.setTamanho(null);
        return voltarParaCarrinhoDepoisAtualizacao(sessao, "Pedido atualizado.");
    }

    private ChatbotResponse voltarParaCarrinhoDepoisAtualizacao(SessaoChat sessao, String mensagem) {
        if (carrinhoService.getItens(sessao).isEmpty()) {
            limparSessao(sessao);
            sessaoRepository.save(sessao);
            return ChatbotResponse.mensagem(mensagem + " O pedido ficou vazio e foi cancelado.");
        }

        sessao.setEstado(AGUARDANDO_CONFIRMACAO_CARRINHO);
        salvarSessao(sessao);
        return respostaCarrinho(sessao, mensagem + " Confirma o pedido?");
    }

    private ChatbotResponse iniciarPagamento(SessaoChat sessao) {
        sessao.setEstado(AGUARDANDO_FORMA_PAGAMENTO);
        sessao.setFormaPagamento(null);
        sessao.setDadosPagamento(null);
        salvarSessao(sessao);

        return new ChatbotResponse(
                TipoResposta.PRODUTO_MENSAGEM,
                "Qual a forma de pagamento? Informe pix, cartao ou dinheiro.",
                null,
                carrinhoService.getItens(sessao).isEmpty() ? sessao.getProduto() : carrinhoService.montarResponse(sessao)
        );
    }

    private ChatbotResponse resolverFormaPagamento(SessaoChat sessao, String mensagem) {
        if (isCancelamento(mensagem) || isRespostaNegativaParaConfirmacao(mensagem)) {
            return cancelarCompra(sessao);
        }

        String formaPagamento = detectarFormaPagamento(mensagem);
        if (formaPagamento == null) {
            salvarSessao(sessao);
            return ChatbotResponse.mensagem("Forma de pagamento invalida. Informe pix, cartao ou dinheiro.");
        }

        sessao.setFormaPagamento(formaPagamento);
        sessao.setEstado(AGUARDANDO_DADOS_PAGAMENTO);
        salvarSessao(sessao);

        return ChatbotResponse.mensagem(mensagemDadosPagamento(formaPagamento));
    }

    private ChatbotResponse resolverDadosPagamento(SessaoChat sessao, String mensagem) {
        if (isCancelamento(mensagem) || isRespostaNegativaParaConfirmacao(mensagem)) {
            return cancelarCompra(sessao);
        }

        String dadosPagamento = normalizarDadosPagamento(sessao.getFormaPagamento(), mensagem);
        if (dadosPagamento == null) {
            salvarSessao(sessao);
            return ChatbotResponse.mensagem(mensagemDadosPagamento(sessao.getFormaPagamento()));
        }

        sessao.setDadosPagamento(dadosPagamento);
        salvarSessao(sessao);

        return finalizarVenda(sessao);
    }

    private ChatbotResponse finalizarVenda(SessaoChat sessao) {
        if (!carrinhoService.getItens(sessao).isEmpty()) {
            List<ItemCarrinho> itens = carrinhoService.getItens(sessao);
            var vendas = vendaService.processarVendas(
                    itens,
                    sessao.getFormaPagamento(),
                    sessao.getDadosPagamento(),
                    sessao.getCpfCliente()
            );
            CarrinhoResponse carrinho = carrinhoService.montarResponse(sessao);

            limparSessao(sessao);
            sessaoRepository.save(sessao);

            return new ChatbotResponse(
                    TipoResposta.VENDA_MENSAGEM,
                    "Venda realizada com sucesso! Redirecionando para o financeiro.",
                    vendas,
                    carrinho
            );
        }

        var venda = vendaService.processarVenda(
                sessao.getProduto(),
                sessao.getQuantidade(),
                sessao.getTamanho(),
                sessao.getFormaPagamento(),
                sessao.getDadosPagamento(),
                sessao.getCpfCliente()
        );

        limparSessao(sessao);
        sessaoRepository.save(sessao);

        return new ChatbotResponse(
                TipoResposta.VENDA_MENSAGEM,
                "Venda realizada com sucesso! Redirecionando para o financeiro.",
                null,
                venda
        );
    }

    private ChatbotResponse iniciarTamanho(SessaoChat sessao, Produto produto, Integer quantidade) {
        sessao.setProduto(produto);
        sessao.setQuantidade(quantidade);
        sessao.setTamanho(null);
        sessao.setEstado(AGUARDANDO_TAMANHO);
        salvarSessao(sessao);

        return new ChatbotResponse(
                TipoResposta.PRODUTO_MENSAGEM,
                "Qual tamanho deseja para este item? Informe PP, P, M, G, GG ou numeracao como 38, 40, 42, 44.",
                null,
                produto
        );
    }

    private ChatbotResponse iniciarPedidoComItens(SessaoChat sessao, List<ItemPedidoExtraido> itensExtraidos) {
        return iniciarPedidoComItens(sessao, itensExtraidos, true);
    }

    private ChatbotResponse iniciarPedidoComItens(SessaoChat sessao, List<ItemPedidoExtraido> itensExtraidos, boolean limparAntes) {
        if (limparAntes) {
            limparSessao(sessao);
        }

        List<ItemPendente> pendentes = new java.util.ArrayList<>();
        List<ItemTamanhoPendente> tamanhosPendentes = new java.util.ArrayList<>();
        for (ItemPedidoExtraido item : itensExtraidos) {
            Produto produtoExato = produtoService.buscarProdutoPorNomeExato(item.termo(), null);
            if (produtoExato != null) {
                tamanhosPendentes.add(new ItemTamanhoPendente(produtoExato, item.quantidade()));
                continue;
            }

            List<Produto> candidatos = produtoService.buscarCandidatosPorTermo(item.termo(), null);
            if (candidatos.size() == 1) {
                tamanhosPendentes.add(new ItemTamanhoPendente(candidatos.get(0), item.quantidade()));
            } else {
                pendentes.add(new ItemPendente(
                        item.termo(),
                        item.quantidade(),
                        candidatos.isEmpty() ? produtoService.buscarProdutos() : candidatos
                ));
            }
        }

        carrinhoService.salvarPendentes(sessao, pendentes);
        carrinhoService.salvarTamanhosPendentes(sessao, tamanhosPendentes);
        ItemPendente pendente = carrinhoService.proximoPendente(sessao);

        if (pendente != null) {
            sessao.setEstado(AGUARDANDO_RESOLUCAO_ITEM);
            salvarSessao(sessao);
            return respostaItemPendente(pendente, "Para " + pendente.quantidade() + "x " + pendente.termo()
                    + ", escolha o id do produto correto.");
        }

        ItemTamanhoPendente tamanhoPendente = carrinhoService.proximoTamanhoPendente(sessao);
        if (tamanhoPendente != null) {
            return iniciarTamanho(sessao, tamanhoPendente.produto(), tamanhoPendente.quantidade());
        }

        sessao.setEstado(AGUARDANDO_CONFIRMACAO_CARRINHO);
        salvarSessao(sessao);
        return respostaCarrinho(sessao, "Montei seu pedido. Confirma a compra?");
    }

    private ChatbotResponse adicionarItensInformados(SessaoChat sessao, String mensagem, boolean adicionarItemAtualAntes) {
        List<ItemPedidoExtraido> itensExtraidos = pedidoParserService.extrairItens(mensagem);
        if (itensExtraidos.isEmpty()) {
            return null;
        }

        if (adicionarItemAtualAntes) {
            carrinhoService.adicionarItem(sessao, sessao.getProduto(), sessao.getQuantidade(), sessao.getTamanho());
            sessao.setProduto(null);
            sessao.setQuantidade(null);
            sessao.setTamanho(null);
        }

        return iniciarPedidoComItens(sessao, itensExtraidos, false);
    }

    private ChatbotResponse resolverItemPendente(SessaoChat sessao, String mensagem) {
        ItemPendente pendente = carrinhoService.getItemPendente(sessao);
        if (pendente == null) {
            sessao.setEstado(AGUARDANDO_CONFIRMACAO_CARRINHO);
            salvarSessao(sessao);
            return respostaCarrinho(sessao, "Confirma a compra dos itens do carrinho?");
        }

        List<ItemPedidoExtraido> itensExtraidos = pedidoParserService.extrairItens(mensagem);
        if (!itensExtraidos.isEmpty()) {
            return iniciarPedidoComItens(sessao, itensExtraidos);
        }

        Produto produtoSelecionado = selecionarProdutoPendente(mensagem, pendente);
        if (produtoSelecionado == null) {
            return respostaItemPendente(pendente, "Nao encontrei esse produto nas opcoes. Informe o id correto.");
        }

        return iniciarTamanho(sessao, produtoSelecionado, pendente.quantidade());
    }

    private ChatbotResponse respostaItemPendente(ItemPendente pendente, String mensagem) {
        if (pendente == null) {
            return ChatbotResponse.mensagem("Nao encontrei o item pendente. Vamos recomecar a compra.");
        }

        if (pendente.opcoes() == null || pendente.opcoes().isEmpty()) {
            return ChatbotResponse.mensagem("Nao encontrei produto para " + pendente.termo()
                    + ". Cadastre o produto ou tente informar um nome mais especifico.");
        }

        return ChatbotResponse.listaMensagem(TipoResposta.LISTA_PRODUTOS_MENSAGEM, pendente.opcoes(), mensagem);
    }

    private ChatbotResponse continuarItensPendentesAposTamanho(SessaoChat sessao) {
        ItemPendente proximoProduto = carrinhoService.proximoPendente(sessao);
        if (proximoProduto != null) {
            sessao.setEstado(AGUARDANDO_RESOLUCAO_ITEM);
            salvarSessao(sessao);
            return respostaItemPendente(proximoProduto, "Agora escolha o produto para "
                    + proximoProduto.quantidade() + "x " + proximoProduto.termo() + ".");
        }

        ItemTamanhoPendente proximoTamanho = carrinhoService.proximoTamanhoPendente(sessao);
        if (proximoTamanho != null) {
            return iniciarTamanho(sessao, proximoTamanho.produto(), proximoTamanho.quantidade());
        }

        sessao.setEstado(AGUARDANDO_CONFIRMACAO_CARRINHO);
        salvarSessao(sessao);
        return respostaCarrinho(sessao, "Montei seu pedido com os tamanhos escolhidos. Confirma a compra?");
    }

    private ChatbotResponse respostaCarrinho(SessaoChat sessao, String mensagem) {
        return new ChatbotResponse(TipoResposta.PRODUTO_MENSAGEM, mensagem, null, carrinhoService.montarResponse(sessao));
    }

    private ChatbotResponse prepararAdicaoGenericaDeItem(SessaoChat sessao, String mensagem) {
        if (!isAdicionarMais(mensagem) || !pedidoParserService.extrairItens(mensagem).isEmpty()) {
            return null;
        }

        if (sessao.getEstado() == AGUARDANDO_CONFIRMACAO_COMPRA
                && sessao.getProduto() != null
                && sessao.getQuantidade() != null
                && sessao.getTamanho() != null) {
            carrinhoService.adicionarItem(sessao, sessao.getProduto(), sessao.getQuantidade(), sessao.getTamanho());
        }

        limparItemEmAndamento(sessao);
        limparPendenciasTemporarias(sessao);
        sessao.setEstado(AGUARDANDO_PRODUTO);
        sessao.setTipoProdutoInteresse(null);
        salvarSessao(sessao);

        return ChatbotResponse.mensagem(
                "Certo, qual produto deseja adicionar ao pedido? Informe o nome, como camisa do Flamengo, ou o id do produto."
        );
    }

    private ChatbotResponse atualizarQuantidadeCarrinho(SessaoChat sessao, String mensagem) {
        Integer quantidade = extrairQuantidade(mensagem);
        if (quantidade == null) {
            return null;
        }

        List<ItemCarrinho> itens = carrinhoService.getItens(sessao);
        if (itens.size() != 1) {
            return respostaCarrinho(
                    sessao,
                    "Entendi a quantidade " + quantidade + ", mas ha mais de um item no pedido. Informe o id ou nome do item que deseja alterar."
            );
        }

        if (!carrinhoService.atualizarQuantidadeItemUnico(sessao, quantidade)) {
            return respostaCarrinho(sessao, "Nao consegui atualizar a quantidade. Informe o item que deseja alterar.");
        }

        salvarSessao(sessao);
        return respostaCarrinho(sessao, "Atualizei a quantidade para " + quantidade + " unidade(s). Confirma a compra?");
    }

    private boolean deveAtualizarQuantidadeDoItemAtual(SessaoChat sessao, String mensagem) {
        List<ItemCarrinho> itens = carrinhoService.getItens(sessao);
        if (itens.size() != 1) {
            return false;
        }

        return mensagemIndicaQuantidadeDoProdutoAtual(mensagem, itens.get(0).produto());
    }

    private boolean deveAtualizarQuantidadeDoProdutoAtual(SessaoChat sessao, String mensagem) {
        return mensagemIndicaQuantidadeDoProdutoAtual(mensagem, sessao.getProduto());
    }

    private boolean mensagemIndicaQuantidadeDoProdutoAtual(String mensagem, Produto produtoAtual) {
        if (produtoAtual == null || produtoAtual.getId() == null || extrairQuantidade(mensagem) == null) {
            return false;
        }

        String msg = normalizarResposta(mensagem);
        if (contemPalavra(msg, "tambem")) {
            return false;
        }

        List<ItemPedidoExtraido> itensExtraidos = pedidoParserService.extrairItens(mensagem);
        if (itensExtraidos.isEmpty()) {
            return true;
        }

        return itensExtraidos.stream()
                .allMatch(item -> termoRepresentaProdutoAtualOuGenerico(item.termo(), produtoAtual));
    }

    private boolean termoRepresentaProdutoAtualOuGenerico(String termo, Produto produtoAtual) {
        Produto produtoExato = produtoService.buscarProdutoPorNomeExato(termo, null);
        if (produtoExato != null) {
            return Objects.equals(produtoAtual.getId(), produtoExato.getId());
        }

        List<Produto> candidatos = produtoService.buscarCandidatosPorTermo(termo, null);
        if (candidatos.size() == 1) {
            return Objects.equals(produtoAtual.getId(), candidatos.get(0).getId());
        }

        String termoNormalizado = normalizarResposta(termo);
        String produtoNormalizado = normalizarResposta(produtoAtual.getNome());
        String tipoProduto = produtoService.detectarTipoProdutoInteresse(termo);

        return TextoUtils.contemTermo(produtoNormalizado, termoNormalizado)
                || TextoUtils.contemTermo(termoNormalizado, produtoNormalizado)
                || (tipoProduto != null && TextoUtils.contemTermo(produtoNormalizado, tipoProduto));
    }

    private Produto selecionarProdutoPendente(String mensagem, ItemPendente pendente) {
        if (pendente == null || pendente.opcoes() == null || pendente.opcoes().isEmpty()) {
            return null;
        }

        Long id = extrairId(mensagem);
        if (id != null) {
            Produto produtoOpcao = pendente.opcoes()
                    .stream()
                    .filter(Objects::nonNull)
                    .filter(produto -> Objects.equals(produto.getId(), id))
                    .findFirst()
                    .orElse(null);

            if (produtoOpcao != null) {
                Produto produtoBanco = produtoService.buscarProdutoPorId(id);
                return produtoBanco != null ? produtoBanco : produtoOpcao;
            }
            return null;
        }

        Produto produtoPorNome = produtoService.buscarProdutoPorNomeExato(mensagem, null);
        if (produtoEstaNasOpcoes(produtoPorNome, pendente)) {
            return produtoPorNome;
        }

        return null;
    }

    private boolean produtoEstaNasOpcoes(Produto produto, ItemPendente pendente) {
        if (produto == null || produto.getId() == null || pendente == null || pendente.opcoes() == null) {
            return false;
        }

        return pendente.opcoes()
                .stream()
                .filter(Objects::nonNull)
                .map(Produto::getId)
                .filter(Objects::nonNull)
                .anyMatch(id -> id.equals(produto.getId()));
    }

    private Produto selecionarProdutoCarrinho(SessaoChat sessao, String mensagem) {
        Long id = extrairId(mensagem);
        String msg = normalizar(mensagem);

        return carrinhoService.getItens(sessao)
                .stream()
                .map(ItemCarrinho::produto)
                .filter(Objects::nonNull)
                .filter(produto -> Objects.equals(produto.getId(), id)
                        || TextoUtils.contemTermo(msg, produto.getNome())
                        || msg.contains(normalizar(produto.getNome())))
                .findFirst()
                .orElse(null);
    }

    private String detectarFormaPagamento(String mensagem) {
        String msg = normalizarResposta(mensagem);
        if (contemPalavra(msg, "pix")) {
            return "PIX";
        }
        if (contemPalavra(msg, "cartao") || contemPalavra(msg, "credito") || contemPalavra(msg, "debito")) {
            return "CARTAO";
        }
        if (contemPalavra(msg, "dinheiro") || contemPalavra(msg, "especie")) {
            return "DINHEIRO";
        }
        return null;
    }

    private String mensagemDadosPagamento(String formaPagamento) {
        if ("PIX".equals(formaPagamento)) {
            return "Pagamento via PIX. Informe o nome do pagador ou CPF/CNPJ para identificacao.";
        }
        if ("CARTAO".equals(formaPagamento)) {
            return "Pagamento no cartao. Informe debito ou credito e os ultimos 4 digitos do cartao.";
        }
        if ("DINHEIRO".equals(formaPagamento)) {
            return "Pagamento em dinheiro. Informe sem troco ou o valor entregue pelo cliente.";
        }
        return "Informe os dados do pagamento.";
    }

    private String normalizarDadosPagamento(String formaPagamento, String mensagem) {
        String msg = normalizarResposta(mensagem);
        if (msg.isBlank()) {
            return null;
        }
        if ("PIX".equals(formaPagamento)) {
            return "Identificacao PIX: " + mensagem.trim();
        }
        if ("CARTAO".equals(formaPagamento)) {
            return (msg.contains("credito") || msg.contains("debito")) && msg.matches(".*\\d{4}.*")
                    ? "Cartao: " + mensagem.trim()
                    : null;
        }
        if ("DINHEIRO".equals(formaPagamento)) {
            return msg.contains("sem troco") || msg.contains("nao precisa troco") || msg.matches(".*\\d+.*")
                    ? "Dinheiro: " + mensagem.trim()
                    : null;
        }
        return mensagem.trim();
    }

    private List<Produto> buscarProdutosContexto(SessaoChat sessao) {
        return produtoService.buscarProdutosPorTipoInteresse(sessao.getTipoProdutoInteresse());
    }

    private Long extrairId(String mensagem) {
        String idTexto = TextoUtils.somenteDigitos(mensagem);
        if (idTexto.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(idTexto);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer extrairQuantidade(String mensagem) {
        String msg = normalizarResposta(mensagem);
        if (msg.isBlank() || contemPalavra(msg, "id")) {
            return null;
        }

        String quantidadeTexto = TextoUtils.somenteDigitos(msg);
        if (!quantidadeTexto.isBlank() && quantidadeTexto.length() <= 3) {
            try {
                int quantidade = Integer.parseInt(quantidadeTexto);
                return quantidade > 0 ? quantidade : null;
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        if (contemPalavra(msg, "um") || contemPalavra(msg, "uma")) {
            return 1;
        }
        if (contemPalavra(msg, "dois") || contemPalavra(msg, "duas")) {
            return 2;
        }
        if (contemPalavra(msg, "tres")) {
            return 3;
        }
        if (contemPalavra(msg, "quatro")) {
            return 4;
        }
        if (contemPalavra(msg, "cinco")) {
            return 5;
        }

        return null;
    }

    private boolean isConfirmacao(String mensagem) {
        String msg = normalizarResposta(mensagem);
        return contemPalavra(msg, "sim")
                || contemPalavra(msg, "confirmo")
                || contemPalavra(msg, "confirmar")
                || contemPalavra(msg, "pode")
                || contemPalavra(msg, "fechar")
                || contemPalavra(msg, "finalizar");
    }

    private boolean isCancelamento(String mensagem) {
        String msg = normalizarResposta(mensagem);
        return contemPalavra(msg, "cancelar")
                || contemPalavra(msg, "cancela")
                || contemPalavra(msg, "desistir")
                || contemPalavra(msg, "desisto")
                || contemPalavra(msg, "parar")
                || contemPalavra(msg, "sair")
                || contemPalavra(msg, "encerrar");
    }

    private boolean isAdicionarMais(String mensagem) {
        String msg = normalizarResposta(mensagem);
        return msg.contains("comprar mais")
                || msg.contains("adicionar mais")
                || contemPalavra(msg, "adiciona")
                || contemPalavra(msg, "adicionar")
                || msg.contains("colocar mais")
                || contemPalavra(msg, "coloca")
                || contemPalavra(msg, "colocar")
                || msg.contains("incluir mais")
                || contemPalavra(msg, "inclui")
                || contemPalavra(msg, "incluir")
                || contemPalavra(msg, "tambem")
                || msg.contains("mais produto")
                || msg.contains("mais item")
                || msg.equals("mais");
    }

    private boolean isAtualizacaoPedido(String mensagem) {
        String msg = normalizarResposta(mensagem);
        return contemPalavra(msg, "trocar")
                || contemPalavra(msg, "alterar")
                || contemPalavra(msg, "atualizar")
                || contemPalavra(msg, "remover")
                || contemPalavra(msg, "retirar");
    }

    private boolean isContinuarPedido(String mensagem) {
        String msg = normalizarResposta(mensagem);
        return (contemPalavra(msg, "continuar")
                || contemPalavra(msg, "retomar")
                || contemPalavra(msg, "seguir")
                || contemPalavra(msg, "prosseguir"))
                && (contemPalavra(msg, "pedido")
                || contemPalavra(msg, "compra")
                || contemPalavra(msg, "carrinho"));
    }

    private boolean isRemocaoItem(String mensagem) {
        String msg = normalizarResposta(mensagem);
        return contemPalavra(msg, "remover")
                || contemPalavra(msg, "retirar")
                || contemPalavra(msg, "excluir")
                || contemPalavra(msg, "deletar");
    }

    private boolean isNegacao(String mensagem) {
        String msg = normalizarResposta(mensagem);
        return contemPalavra(msg, "nao") || msg.contains("ainda nao");
    }

    private boolean isNegacaoDireta(String mensagem) {
        String msg = normalizarResposta(mensagem);
        return msg.equals("nao")
                || msg.equals("n")
                || msg.equals("negativo")
                || msg.equals("nao obrigado")
                || msg.equals("nao quero")
                || msg.equals("quero nao");
    }

    private boolean isRespostaNegativaParaConfirmacao(String mensagem) {
        String msg = normalizarResposta(mensagem);
        return isNegacaoDireta(msg) || contemPalavra(msg, "nao") || contemPalavra(msg, "negativo");
    }

    private boolean isEstadoConfirmacaoFinal(SessaoChat sessao) {
        return sessao != null
                && (sessao.getEstado() == AGUARDANDO_CONFIRMACAO_CARRINHO
                || sessao.getEstado() == AGUARDANDO_CONFIRMACAO_COMPRA);
    }

    private ChatbotResponse cancelarCompra(SessaoChat sessao) {
        limparSessao(sessao);
        sessaoRepository.save(sessao);
        return ChatbotResponse.mensagem("Tudo bem, compra cancelada.");
    }

    private ChatbotResponse reiniciarEstadoInvalido(SessaoChat sessao) {
        limparSessao(sessao);
        sessaoRepository.save(sessao);
        return ChatbotResponse.mensagem("Sua mensagem foi invalida, ou algo deu errado na operacao. Vamos recomecar.");
    }

    private boolean contemPalavra(String mensagem, String palavra) {
        return mensagem != null && mensagem.matches(".*\\b" + palavra + "\\b.*");
    }

    private String detectarTamanho(String mensagem) {
        String msg = normalizarResposta(mensagem);
        if (msg.isBlank()) {
            return null;
        }

        String[] tokens = msg.split("\\s+");
        for (String token : tokens) {
            if (token.matches("pp|p|m|g|gg|xg|xgg")) {
                return token.toUpperCase();
            }
            if (token.matches("\\d{2,3}")) {
                return token;
            }
        }

        return null;
    }

    private String normalizarResposta(String mensagem) {
        return TextoUtils.normalizar(mensagem);
    }

    private String normalizar(String text) {
        return TextoUtils.normalizar(text);
    }

    private String normalizarCpf(String cpfCliente) {
        return TextoUtils.somenteDigitos(cpfCliente);
    }

    private String normalizarClientSessionId(String clientSessionId) {
        if (clientSessionId == null || clientSessionId.isBlank()) {
            return "api-default";
        }

        return clientSessionId.trim();
    }

    private boolean cpfValido(String cpf) {
        return cpf != null && cpf.length() == 11;
    }

    private void validarCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            throw new BusinessException("Informe um CPF valido com 11 digitos para identificar o cliente");
        }
    }

    private void salvarSessao(SessaoChat sessao) {
        sessao.setUltimaAtualizacao(LocalDateTime.now());
        sessaoRepository.save(sessao);
    }

    private void limparSessao(SessaoChat sessao) {
        sessao.setEstado(INICIAL);
        sessao.setProduto(null);
        sessao.setQuantidade(null);
        sessao.setTamanho(null);
        sessao.setTipoProdutoInteresse(null);
        sessao.setFormaPagamento(null);
        sessao.setDadosPagamento(null);
        sessao.setMensagemPendenteCpf(null);
        sessao.setUltimaAtualizacao(LocalDateTime.now());
        carrinhoService.limpar(sessao);
    }

    private void limparItemEmAndamento(SessaoChat sessao) {
        sessao.setProduto(null);
        sessao.setQuantidade(null);
        sessao.setTamanho(null);
        sessao.setFormaPagamento(null);
        sessao.setDadosPagamento(null);
    }

    private void limparPendenciasTemporarias(SessaoChat sessao) {
        sessao.setItensPendentesJson(null);
        sessao.setItemPendenteJson(null);
        sessao.setItensTamanhoPendentesJson(null);
        sessao.setItemTamanhoPendenteJson(null);
    }
}
