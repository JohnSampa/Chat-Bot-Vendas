package unifan.chat_bot_vendas.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import unifan.chat_bot_vendas.domain.Intencao;
import unifan.chat_bot_vendas.domain.Produto;
import unifan.chat_bot_vendas.domain.SessaoChat;
import unifan.chat_bot_vendas.domain.enums.EstadoSessao;
import unifan.chat_bot_vendas.dto.ChatbotResponse;
import unifan.chat_bot_vendas.dto.enums.TipoResposta;
import unifan.chat_bot_vendas.repositories.SessaoChatRepository;

import java.time.LocalDateTime;
import java.util.List;

import static unifan.chat_bot_vendas.domain.enums.EstadoSessao.INICIAL;

@Service
public class ChatBotComandosService {

    @Autowired
    private ProdutoService produtoService;

    @Autowired
    private VendaService vendaService;

    @Autowired
    private SessaoChatRepository sessaoChatRepository;

    @Autowired
    private CarrinhoService carrinhoService;

    public ChatbotResponse executarComandos(Intencao intencao, SessaoChat sessao) {
        return switch (intencao.getTipoAcao()) {
            case INICIAR_COMPRA -> iniciarCompra(sessao);
            case CHECAR_ESTOQUE -> ChatbotResponse.lista(TipoResposta.LISTA_PRODUTOS, produtoService.buscarProdutos());
            case VERIFICAR_VENDAS -> ChatbotResponse.lista(TipoResposta.LISTA_VENDAS, vendaService.getVendas(sessao.getCpfCliente()));
            case CONTINUAR_PEDIDO -> continuarPedido(sessao);
            case ATUALIZAR_PEDIDO -> iniciarAtualizacaoPedido(sessao);
            case DELETAR_PEDIDO -> deletarPedidoAberto(sessao);
            case SAIR_CHAT_VENDA -> sairChatVenda(intencao, sessao);
            case COMPRAR_CAMISA -> iniciarCompraProduto(sessao, "camisa");
            case COMPRAR_CALCA -> iniciarCompraProduto(sessao, "calca");
            case COMPRAR_SHORT_BERMUDA -> iniciarCompraPorTermos(sessao, "short", "bermuda");
            case COMPRAR_BLUSA -> iniciarCompraProduto(sessao, "blusa");
            case COMPRAR_SAPATO -> iniciarCompraProduto(sessao, "sapato");
        };
    }

    public ChatbotResponse continuarPedido(SessaoChat sessao) {
        SessaoChat sessaoPedido = localizarSessaoComPedidoAberto(sessao);
        if (sessaoPedido == null) {
            return ChatbotResponse.mensagem("Nao encontrei pedido em aberto para este CPF/CNPJ. Posso iniciar uma nova compra para voce.");
        }

        ativarSessaoPedido(sessao, sessaoPedido);
        salvarSessao(sessaoPedido);
        return respostaContinuacaoPedido(sessaoPedido);
    }

    private ChatbotResponse iniciarCompra(SessaoChat sessao) {
        sessao.setEstado(EstadoSessao.AGUARDANDO_PRODUTO);
        sessao.setTipoProdutoInteresse(null);
        sessao.setFormaPagamento(null);
        sessao.setDadosPagamento(null);
        carrinhoService.limpar(sessao);
        salvarSessao(sessao);

        return ChatbotResponse.listaMensagem(
                TipoResposta.LISTA_PRODUTOS_MENSAGEM,
                produtoService.buscarProdutos(),
                "Qual o id do produto que deseja comprar?"
        );
    }

    private ChatbotResponse iniciarAtualizacaoPedido(SessaoChat sessao) {
        SessaoChat sessaoPedido = localizarSessaoComPedidoAberto(sessao);
        if (sessaoPedido == null) {
            return ChatbotResponse.mensagem("Nao encontrei pedido em aberto para este CPF/CNPJ. Inicie uma compra ou consulte pedidos finalizados.");
        }

        ativarSessaoPedido(sessao, sessaoPedido);
        sessaoPedido.setEstado(EstadoSessao.AGUARDANDO_ITEM_ATUALIZACAO);
        salvarSessao(sessaoPedido);

        return new ChatbotResponse(
                TipoResposta.PRODUTO_MENSAGEM,
                "Informe o id ou nome do item do pedido que deseja trocar ou remover.",
                null,
                carrinhoService.montarResponse(sessaoPedido)
        );
    }

    private ChatbotResponse deletarPedidoAberto(SessaoChat sessao) {
        SessaoChat sessaoPedido = localizarSessaoComPedidoAberto(sessao);
        if (sessaoPedido == null && sessao.getProduto() == null) {
            return ChatbotResponse.mensagem("Nao encontrei pedido em aberto para cancelar neste CPF/CNPJ.");
        }

        limparSessao(sessaoPedido == null ? sessao : sessaoPedido);
        sessaoChatRepository.save(sessaoPedido == null ? sessao : sessaoPedido);
        return ChatbotResponse.mensagem("Pedido em aberto cancelado para este CPF/CNPJ.");
    }

    private ChatbotResponse sairChatVenda(Intencao intencao, SessaoChat sessao) {
        if (sessao.getEstado() != INICIAL) {
            limparSessao(sessao);
            sessaoChatRepository.save(sessao);
            return ChatbotResponse.mensagem(intencao.getResposta() + "\nCancelando compra...");
        }

        return ChatbotResponse.mensagem(intencao.getResposta());
    }

    private ChatbotResponse iniciarCompraProduto(SessaoChat sessao, String nomeProduto) {
        List<Produto> produtos = produtoService.buscarProdutosPorNomeParcial(nomeProduto);

        if (produtos.isEmpty()) {
            limparSessao(sessao);
            sessaoChatRepository.save(sessao);

            return ChatbotResponse.listaMensagem(
                    TipoResposta.LISTA_PRODUTOS_MENSAGEM,
                    produtoService.buscarProdutos(),
                    "Nao encontrei " + nomeProduto + " no estoque. Qual o id do produto que deseja comprar?"
            );
        }

        sessao.setProduto(null);
        sessao.setQuantidade(null);
        sessao.setTipoProdutoInteresse(nomeProduto);
        sessao.setFormaPagamento(null);
        sessao.setDadosPagamento(null);
        carrinhoService.limpar(sessao);
        sessao.setEstado(EstadoSessao.AGUARDANDO_PRODUTO);
        salvarSessao(sessao);

        return ChatbotResponse.listaMensagem(
                TipoResposta.LISTA_PRODUTOS_MENSAGEM,
                produtos,
                "Encontrei estas opcoes de " + nomeProduto + ". Qual o id do produto que deseja comprar?"
        );
    }

    private ChatbotResponse iniciarCompraPorTermos(SessaoChat sessao, String... termos) {
        List<Produto> produtos = java.util.Arrays.stream(termos)
                .flatMap(termo -> produtoService.buscarProdutosPorNomeParcial(termo).stream())
                .distinct()
                .toList();

        String descricao = String.join(" ou ", termos);
        if (produtos.isEmpty()) {
            limparSessao(sessao);
            sessaoChatRepository.save(sessao);

            return ChatbotResponse.listaMensagem(
                    TipoResposta.LISTA_PRODUTOS_MENSAGEM,
                    produtoService.buscarProdutos(),
                    "Nao encontrei " + descricao + " no estoque. Qual o id do produto que deseja comprar?"
            );
        }

        sessao.setProduto(null);
        sessao.setQuantidade(null);
        sessao.setTipoProdutoInteresse(null);
        sessao.setFormaPagamento(null);
        sessao.setDadosPagamento(null);
        carrinhoService.limpar(sessao);
        sessao.setEstado(EstadoSessao.AGUARDANDO_PRODUTO);
        salvarSessao(sessao);

        return ChatbotResponse.listaMensagem(
                TipoResposta.LISTA_PRODUTOS_MENSAGEM,
                produtos,
                "Encontrei estas opcoes de " + descricao + ". Qual o id do produto que deseja comprar?"
        );
    }

    private void limparSessao(SessaoChat sessao) {
        sessao.setEstado(INICIAL);
        sessao.setProduto(null);
        sessao.setQuantidade(null);
        sessao.setTipoProdutoInteresse(null);
        sessao.setFormaPagamento(null);
        sessao.setDadosPagamento(null);
        sessao.setUltimaAtualizacao(LocalDateTime.now());
        carrinhoService.limpar(sessao);
    }

    private void salvarSessao(SessaoChat sessao) {
        sessao.setUltimaAtualizacao(LocalDateTime.now());
        sessaoChatRepository.save(sessao);
    }

    private SessaoChat localizarSessaoComPedidoAberto(SessaoChat sessaoAtual) {
        if (temPedidoAberto(sessaoAtual)) {
            return sessaoAtual;
        }

        return sessaoChatRepository.findByCpfClienteOrderByIdDesc(sessaoAtual.getCpfCliente())
                .stream()
                .filter(this::temPedidoAberto)
                .findFirst()
                .orElse(null);
    }

    private boolean temPedidoAberto(SessaoChat sessao) {
        return sessao != null
                && (!carrinhoService.getItens(sessao).isEmpty()
                || sessao.getProduto() != null
                || (sessao.getEstado() != null
                && sessao.getEstado() != INICIAL
                && sessao.getEstado() != EstadoSessao.AGUARDANDO_CPF));
    }

    private void ativarSessaoPedido(SessaoChat sessaoAtual, SessaoChat sessaoPedido) {
        if (!sessaoAtual.getId().equals(sessaoPedido.getId())) {
            sessaoAtual.setAtiva(false);
            salvarSessao(sessaoAtual);
        }
        sessaoPedido.setAtiva(true);
        sessaoPedido.setClientSessionId(sessaoAtual.getClientSessionId());
    }

    private ChatbotResponse respostaContinuacaoPedido(SessaoChat sessaoPedido) {
        if (!carrinhoService.getItens(sessaoPedido).isEmpty()) {
            sessaoPedido.setEstado(EstadoSessao.AGUARDANDO_CONFIRMACAO_CARRINHO);
            salvarSessao(sessaoPedido);
            return new ChatbotResponse(
                    TipoResposta.PRODUTO_MENSAGEM,
                    "Encontrei este pedido em aberto. Deseja continuar com estes itens? Responda sim, nao, alterar ou informe outro produto para adicionar.",
                    null,
                    carrinhoService.montarResponse(sessaoPedido)
            );
        }

        if (sessaoPedido.getEstado() == EstadoSessao.AGUARDANDO_TAMANHO && sessaoPedido.getProduto() != null) {
            return new ChatbotResponse(
                    TipoResposta.PRODUTO_MENSAGEM,
                    "Retomei seu pedido. Qual tamanho deseja para este item? Informe PP, P, M, G, GG ou numeracao como 38, 40, 42, 44.",
                    null,
                    sessaoPedido.getProduto()
            );
        }

        if (sessaoPedido.getEstado() == EstadoSessao.AGUARDANDO_QUANTIDADE && sessaoPedido.getProduto() != null) {
            return new ChatbotResponse(
                    TipoResposta.PRODUTO_MENSAGEM,
                    "Retomei seu pedido. Qual quantidade deseja?",
                    null,
                    sessaoPedido.getProduto()
            );
        }

        if (sessaoPedido.getEstado() == EstadoSessao.AGUARDANDO_CONFIRMACAO_COMPRA && sessaoPedido.getProduto() != null) {
            return new ChatbotResponse(
                    TipoResposta.PRODUTO_MENSAGEM,
                    "Retomei seu pedido. Confirma a compra de "
                            + sessaoPedido.getQuantidade()
                            + " unidade(s) no tamanho "
                            + sessaoPedido.getTamanho()
                            + "?",
                    null,
                    sessaoPedido.getProduto()
            );
        }

        if (sessaoPedido.getEstado() == EstadoSessao.AGUARDANDO_PRODUTO) {
            return ChatbotResponse.listaMensagem(
                    TipoResposta.LISTA_PRODUTOS_MENSAGEM,
                    produtoService.buscarProdutosPorTipoInteresse(sessaoPedido.getTipoProdutoInteresse()),
                    "Retomei sua compra. Qual o id do produto que deseja comprar?"
            );
        }

        return ChatbotResponse.mensagem("Retomei seu pedido. Pode me enviar a proxima informacao para continuar.");
    }
}
