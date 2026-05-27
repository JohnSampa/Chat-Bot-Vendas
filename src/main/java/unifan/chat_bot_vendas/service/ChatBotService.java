package unifan.chat_bot_vendas.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import unifan.chat_bot_vendas.domain.Intencao;
import unifan.chat_bot_vendas.domain.Produto;
import unifan.chat_bot_vendas.domain.SessaoChat;
import unifan.chat_bot_vendas.domain.Venda;
import unifan.chat_bot_vendas.domain.enums.TipoAcao;
import unifan.chat_bot_vendas.dto.CarrinhoResponse;
import unifan.chat_bot_vendas.dto.ChatbotResponse;
import unifan.chat_bot_vendas.dto.ItemCarrinho;
import unifan.chat_bot_vendas.dto.ItemPedidoExtraido;
import unifan.chat_bot_vendas.dto.ItemPendente;
import unifan.chat_bot_vendas.dto.enums.TipoResposta;
import unifan.chat_bot_vendas.exceptions.BusinessException;
import unifan.chat_bot_vendas.repositories.SessaoChatRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static unifan.chat_bot_vendas.domain.enums.EstadoSessao.AGUARDANDO_PRODUTO;
import static unifan.chat_bot_vendas.domain.enums.EstadoSessao.AGUARDANDO_CONFIRMACAO_PRODUTO;
import static unifan.chat_bot_vendas.domain.enums.EstadoSessao.AGUARDANDO_CONFIRMACAO_COMPRA;
import static unifan.chat_bot_vendas.domain.enums.EstadoSessao.AGUARDANDO_CONFIRMACAO_CARRINHO;
import static unifan.chat_bot_vendas.domain.enums.EstadoSessao.AGUARDANDO_QUANTIDADE;
import static unifan.chat_bot_vendas.domain.enums.EstadoSessao.AGUARDANDO_RESOLUCAO_ITEM;
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
        SessaoChat sessao = sessaoRepository.findByUserid(1L)
                .orElse(new SessaoChat());

        sessao.setEstado(INICIAL);
        sessao.setProduto(null);
        sessao.setQuantidade(null);
        sessao.setTipoProdutoInteresse(null);
        carrinhoService.limpar(sessao);
        sessao.setUltimaAtualizacao(LocalDateTime.now());

        if (sessao.getUserid() == null) {
            sessao.setUserid(1L);
        }

        sessaoRepository.save(sessao);

        return ChatbotResponse.mensagem("Conversa reiniciada. Como posso ajudar?");
    }

    public ChatbotResponse processarMensagem(String mensagem) {
        String msg = normalizar(mensagem);

        SessaoChat sessao = sessaoRepository.findByUserid(1L)
                .orElse(new SessaoChat());

        if (sessao.getId() == null){
            sessao.setEstado(INICIAL);
            sessao.setUltimaAtualizacao(LocalDateTime.now());
            sessao.setUserid(1L);
            sessaoRepository.save(sessao);
        }

        if (sessao.getEstado() != INICIAL) {
            return continuarSessao(sessao, mensagem.toLowerCase().trim());
        }

        List<ItemPedidoExtraido> itensExtraidos = pedidoParserService.extrairItens(mensagem);
        if (!itensExtraidos.isEmpty()) {
            return iniciarPedidoComItens(sessao, itensExtraidos);
        }

        Intencao intencao = intencaoService.detectarIntencao(msg);

        if (intencao == null) {
            throw new BusinessException("Desculpe não entendi");
        }

        if (intencao.getTipoAcao() != null){
            return chatBotComandosService.executarComandos(intencao,sessao);
        }

        return ChatbotResponse.mensagem(intencao.getResposta());
    }

    private String normalizar(String text) {
        return text.toLowerCase()
                .replaceAll("[^a-záéíóúãõâêô\\s]", "")
                .trim();
    }

    private ChatbotResponse continuarSessao(final SessaoChat sessao, String mensagem) {
        Intencao intencao = intencaoService.detectarIntencao(mensagem);

        if (intencao != null && intencao.getTipoAcao() == SAIR_CHAT_VENDA) {
            return chatBotComandosService.executarComandos(intencao, sessao);
        }

        return switch (sessao.getEstado()) {
            case AGUARDANDO_PRODUTO -> {
                Produto produto = produtoService.buscarProdutoPorIdMensagem(mensagem, sessao.getTipoProdutoInteresse());

                if (produto != null) {
                    yield iniciarQuantidade(sessao, produto);
                }

                List<ItemPedidoExtraido> itensExtraidos = pedidoParserService.extrairItens(mensagem);
                if (!itensExtraidos.isEmpty()) {
                    yield iniciarPedidoComItens(sessao, itensExtraidos);
                }

                produto = produtoService.buscarProdutoPorNomeExato(mensagem, sessao.getTipoProdutoInteresse());

                if (produto != null) {
                    yield iniciarQuantidade(sessao, produto);
                }

                String tipoProdutoInteresse = produtoService.detectarTipoProdutoInteresse(mensagem);

                if (tipoProdutoInteresse != null) {
                    sessao.setProduto(null);
                    sessao.setQuantidade(null);
                    sessao.setTipoProdutoInteresse(tipoProdutoInteresse);
                    sessao.setUltimaAtualizacao(LocalDateTime.now());
                    sessaoRepository.save(sessao);

                    yield ChatbotResponse.listaMensagem(
                            TipoResposta.LISTA_PRODUTOS_MENSAGEM,
                            buscarProdutosContexto(sessao),
                            "Encontrei estas opcoes de " + tipoProdutoInteresse + ". Qual o id do produto que deseja comprar?"
                    );
                }

                produto = produtoService.buscarProdutoMaisProximoPorMensagem(mensagem, sessao.getTipoProdutoInteresse());

                if (produto == null) {
                    sessao.setUltimaAtualizacao(LocalDateTime.now());
                    sessaoRepository.save(sessao);
                    yield ChatbotResponse.listaMensagem(
                            TipoResposta.LISTA_PRODUTOS_MENSAGEM,
                            buscarProdutosContexto(sessao),
                            "Produto nao encontrado. Informe o id de um dos produtos da lista ou digite o nome mais proximo."
                    );
                }

                sessao.setProduto(produto);
                sessao.setUltimaAtualizacao(LocalDateTime.now());
                sessao.setEstado(AGUARDANDO_CONFIRMACAO_PRODUTO);
                sessaoRepository.save(sessao);

                yield new ChatbotResponse(
                        TipoResposta.PRODUTO_MENSAGEM,
                        "Encontrei este produto. E esse o item que deseja comprar? Responda sim ou nao.",
                        null,
                        produto
                );
            }
            case AGUARDANDO_CONFIRMACAO_PRODUTO -> {
                if (isConfirmacao(mensagem)) {
                    sessao.setEstado(AGUARDANDO_QUANTIDADE);
                    sessao.setUltimaAtualizacao(LocalDateTime.now());
                    sessaoRepository.save(sessao);

                    yield new ChatbotResponse(
                            TipoResposta.PRODUTO_MENSAGEM,
                            "Qual a quantidade deseja",
                            null,
                            sessao.getProduto()
                    );
                } else if (isNegacao(mensagem)
                        || mensagem.contains("outro") || mensagem.contains("errado")) {
                    sessao.setProduto(null);
                    sessao.setEstado(AGUARDANDO_PRODUTO);
                    sessao.setUltimaAtualizacao(LocalDateTime.now());
                    sessaoRepository.save(sessao);

                    yield ChatbotResponse.listaMensagem(
                            TipoResposta.LISTA_PRODUTOS_MENSAGEM,
                            buscarProdutosContexto(sessao),
                            "Tudo bem. Informe o id do produto da lista ou digite um nome mais especifico."
                    );
                }

                sessao.setUltimaAtualizacao(LocalDateTime.now());
                sessaoRepository.save(sessao);
                yield new ChatbotResponse(
                        TipoResposta.PRODUTO_MENSAGEM,
                        "Confirme se este e o item que deseja comprar. Responda sim ou nao.",
                        null,
                        sessao.getProduto()
                );
            }
            case AGUARDANDO_RESOLUCAO_ITEM -> resolverItemPendente(sessao, mensagem);
            case AGUARDANDO_CONFIRMACAO_CARRINHO -> {
                if (isConfirmacao(mensagem)) {
                    List<ItemCarrinho> itens = carrinhoService.getItens(sessao);
                    var vendas = vendaService.processarVendas(itens);
                    CarrinhoResponse carrinho = carrinhoService.montarResponse(sessao);

                    limparSessao(sessao);
                    sessaoRepository.save(sessao);

                    yield new ChatbotResponse(
                            TipoResposta.VENDA_MENSAGEM,
                            "Venda realizada com sucesso!",
                            vendas,
                            carrinho
                    );
                } else if (isCancelamento(mensagem) || isNegacaoDireta(mensagem)) {
                    limparSessao(sessao);
                    sessaoRepository.save(sessao);
                    yield ChatbotResponse.mensagem("Tudo bem, compra cancelada.");
                } else if (isAdicionarMais(mensagem)) {
                    sessao.setEstado(AGUARDANDO_PRODUTO);
                    sessao.setProduto(null);
                    sessao.setQuantidade(null);
                    sessao.setTipoProdutoInteresse(null);
                    sessao.setUltimaAtualizacao(LocalDateTime.now());
                    sessaoRepository.save(sessao);

                    yield ChatbotResponse.listaMensagem(
                            TipoResposta.LISTA_PRODUTOS_MENSAGEM,
                            produtoService.buscarProdutos(),
                            "Certo, vamos adicionar mais itens. Qual o id do proximo produto?"
                    );
                }

                sessao.setUltimaAtualizacao(LocalDateTime.now());
                sessaoRepository.save(sessao);
                yield new ChatbotResponse(
                        TipoResposta.PRODUTO_MENSAGEM,
                        "Confirma a compra dos itens do carrinho? Responda sim ou nao.",
                        null,
                        carrinhoService.montarResponse(sessao)
                );
            }
            case AGUARDANDO_QUANTIDADE -> {
                try {
                    int qty = Integer.parseInt(mensagem.replaceAll("[^0-9]", ""));
                    if (qty <= 0)
                        throw new BusinessException("Insira uma quantidade maior que zero");
                    sessao.setEstado(AGUARDANDO_CONFIRMACAO_COMPRA);
                    sessao.setUltimaAtualizacao(LocalDateTime.now());
                    sessao.setQuantidade(qty);
                    sessaoRepository.save(sessao);

                    yield new ChatbotResponse(
                            TipoResposta.PRODUTO_MENSAGEM,
                            "Confirma a compra de " + qty,
                            null,
                            sessao.getProduto()
                    );
                } catch (NumberFormatException e) {
                    sessao.setUltimaAtualizacao(LocalDateTime.now());
                    sessaoRepository.save(sessao);

                    yield new ChatbotResponse(
                            TipoResposta.ERRO,
                            "Por favor, informe apenas o número de unidades.",
                            null,
                            null
                    );
                }
            }
            case AGUARDANDO_CONFIRMACAO_COMPRA -> {
                if (isConfirmacao(mensagem)) {
                    if (!carrinhoService.getItens(sessao).isEmpty()) {
                        carrinhoService.adicionarItem(sessao, sessao.getProduto(), sessao.getQuantidade());
                        List<ItemCarrinho> itens = carrinhoService.getItens(sessao);
                        var vendas = vendaService.processarVendas(itens);
                        CarrinhoResponse carrinho = carrinhoService.montarResponse(sessao);

                        limparSessao(sessao);
                        sessaoRepository.save(sessao);

                        yield new ChatbotResponse(
                                TipoResposta.VENDA_MENSAGEM,
                                "Venda realizada com sucesso!",
                                vendas,
                                carrinho
                        );
                    }

                    Venda venda = vendaService.processarVenda(sessao.getProduto(), sessao.getQuantidade());

                    limparSessao(sessao);
                    sessaoRepository.save(sessao);

                    yield new ChatbotResponse(
                            TipoResposta.VENDA_MENSAGEM,
                            "Venda realizada com sucesso!",
                            null,
                            venda
                    );

                } else if (isAdicionarMais(mensagem)) {
                    carrinhoService.adicionarItem(sessao, sessao.getProduto(), sessao.getQuantidade());
                    sessao.setEstado(AGUARDANDO_PRODUTO);
                    sessao.setProduto(null);
                    sessao.setQuantidade(null);
                    sessao.setTipoProdutoInteresse(null);
                    sessao.setUltimaAtualizacao(LocalDateTime.now());
                    sessaoRepository.save(sessao);

                    yield ChatbotResponse.listaMensagem(
                            TipoResposta.LISTA_PRODUTOS_MENSAGEM,
                            produtoService.buscarProdutos(),
                            "Adicionei esse item ao pedido. Qual o id do proximo produto?"
                    );
                } else if (isCancelamento(mensagem) || isNegacaoDireta(mensagem)) {
                    sessao.setEstado(INICIAL);
                    sessao.setProduto(null);
                    sessao.setQuantidade(null);
                    sessao.setTipoProdutoInteresse(null);
                    carrinhoService.limpar(sessao);
                    sessao.setUltimaAtualizacao(LocalDateTime.now());
                    sessaoRepository.save(sessao);
                    yield ChatbotResponse.mensagem("Tudo bem, compra cancelada.");
                } else {
                    sessao.setUltimaAtualizacao(LocalDateTime.now());
                    sessaoRepository.save(sessao);
                    yield new ChatbotResponse(
                            TipoResposta.PRODUTO_MENSAGEM,
                            "Responda sim para finalizar, comprar mais para adicionar outro produto, ou cancelar para encerrar.",
                            null,
                            sessao.getProduto()
                    );
                }
            }
            case  SAIR_COMPRA -> {
                sessao.setEstado(INICIAL);
                sessao.setProduto(null);
                sessao.setQuantidade(null);
                sessao.setTipoProdutoInteresse(null);
                carrinhoService.limpar(sessao);
                sessao.setUltimaAtualizacao(LocalDateTime.now());
                sessaoRepository.save(sessao);
                yield ChatbotResponse.mensagem("Tudo bem, compra cancelada.");
            }
            default -> {
                sessao.setEstado(INICIAL);
                sessao.setProduto(null);
                sessao.setQuantidade(null);
                sessao.setTipoProdutoInteresse(null);
                carrinhoService.limpar(sessao);
                sessao.setUltimaAtualizacao(LocalDateTime.now());
                sessaoRepository.save(sessao);
                yield ChatbotResponse
                        .mensagem("Sua mensagem foi inválida, ou algo deu errado na operação vamos recomeçar");
            }
        };
    }

    private List<Produto> buscarProdutosContexto(SessaoChat sessao) {
        return produtoService.buscarProdutosPorTipoInteresse(sessao.getTipoProdutoInteresse());
    }

    private ChatbotResponse iniciarQuantidade(SessaoChat sessao, Produto produto) {
        sessao.setProduto(produto);
        sessao.setUltimaAtualizacao(LocalDateTime.now());
        sessao.setEstado(AGUARDANDO_QUANTIDADE);
        sessaoRepository.save(sessao);

        return new ChatbotResponse(
                TipoResposta.PRODUTO_MENSAGEM,
                "Qual a quantidade deseja",
                null,
                produto
        );
    }

    private ChatbotResponse iniciarPedidoComItens(SessaoChat sessao, List<ItemPedidoExtraido> itensExtraidos) {
        limparSessao(sessao);

        List<ItemPendente> pendentes = new java.util.ArrayList<>();

        for (ItemPedidoExtraido item : itensExtraidos) {
            Produto produtoExato = produtoService.buscarProdutoPorNomeExato(item.termo(), null);
            if (produtoExato != null) {
                carrinhoService.adicionarItem(sessao, produtoExato, item.quantidade());
                continue;
            }

            List<Produto> candidatos = produtoService.buscarCandidatosPorTermo(item.termo(), null);
            if (candidatos.size() == 1) {
                carrinhoService.adicionarItem(sessao, candidatos.get(0), item.quantidade());
            } else {
                if (candidatos.isEmpty()) {
                    candidatos = produtoService.buscarProdutos();
                }
                pendentes.add(new ItemPendente(item.termo(), item.quantidade(), candidatos));
            }
        }

        carrinhoService.salvarPendentes(sessao, pendentes);
        ItemPendente pendente = carrinhoService.proximoPendente(sessao);

        if (pendente != null) {
            sessao.setEstado(AGUARDANDO_RESOLUCAO_ITEM);
            sessao.setUltimaAtualizacao(LocalDateTime.now());
            sessaoRepository.save(sessao);
            return respostaItemPendente(pendente, "Para " + pendente.quantidade() + "x " + pendente.termo()
                    + ", escolha o id do produto correto.");
        }

        sessao.setEstado(AGUARDANDO_CONFIRMACAO_CARRINHO);
        sessao.setUltimaAtualizacao(LocalDateTime.now());
        sessaoRepository.save(sessao);

        return new ChatbotResponse(
                TipoResposta.PRODUTO_MENSAGEM,
                "Montei seu pedido. Confirma a compra?",
                null,
                carrinhoService.montarResponse(sessao)
        );
    }

    private ChatbotResponse resolverItemPendente(SessaoChat sessao, String mensagem) {
        ItemPendente pendente = carrinhoService.getItemPendente(sessao);
        if (pendente == null) {
            sessao.setEstado(AGUARDANDO_CONFIRMACAO_CARRINHO);
            sessaoRepository.save(sessao);
            return new ChatbotResponse(
                    TipoResposta.PRODUTO_MENSAGEM,
                    "Confirma a compra dos itens do carrinho?",
                    null,
                    carrinhoService.montarResponse(sessao)
            );
        }

        Produto produtoSelecionado = selecionarProdutoPendente(mensagem, pendente);

        if (produtoSelecionado == null) {
            return respostaItemPendente(pendente, "Nao encontrei esse produto nas opcoes. Informe o id correto.");
        }

        carrinhoService.adicionarItem(sessao, produtoSelecionado, pendente.quantidade());
        ItemPendente proximo = carrinhoService.proximoPendente(sessao);

        if (proximo != null) {
            sessao.setEstado(AGUARDANDO_RESOLUCAO_ITEM);
            sessao.setUltimaAtualizacao(LocalDateTime.now());
            sessaoRepository.save(sessao);
            return respostaItemPendente(proximo, "Agora escolha o produto para "
                    + proximo.quantidade() + "x " + proximo.termo() + ".");
        }

        sessao.setEstado(AGUARDANDO_CONFIRMACAO_CARRINHO);
        sessao.setUltimaAtualizacao(LocalDateTime.now());
        sessaoRepository.save(sessao);

        return new ChatbotResponse(
                TipoResposta.PRODUTO_MENSAGEM,
                "Montei seu pedido. Confirma a compra?",
                null,
                carrinhoService.montarResponse(sessao)
        );
    }

    private ChatbotResponse respostaItemPendente(ItemPendente pendente, String mensagem) {
        if (pendente == null) {
            return ChatbotResponse.mensagem("Nao encontrei o item pendente. Vamos recomecar a compra.");
        }

        if (pendente.opcoes() == null || pendente.opcoes().isEmpty()) {
            return ChatbotResponse.mensagem("Nao encontrei produto para " + pendente.termo()
                    + ". Cadastre o produto ou tente informar um nome mais especifico.");
        }

        return ChatbotResponse.listaMensagem(
                TipoResposta.LISTA_PRODUTOS_MENSAGEM,
                pendente.opcoes(),
                mensagem
        );
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

    private Long extrairId(String mensagem) {
        String idTexto = mensagem == null ? "" : mensagem.replaceAll("[^0-9]", "");
        if (idTexto.isBlank()) {
            return null;
        }

        try {
            return Long.parseLong(idTexto);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean isConfirmacao(String mensagem) {
        return contemPalavra(mensagem, "sim")
                || contemPalavra(mensagem, "confirmo")
                || contemPalavra(mensagem, "confirmar")
                || contemPalavra(mensagem, "pode")
                || contemPalavra(mensagem, "fechar")
                || contemPalavra(mensagem, "finalizar");
    }

    private boolean isCancelamento(String mensagem) {
        return contemPalavra(mensagem, "cancelar")
                || contemPalavra(mensagem, "cancela")
                || contemPalavra(mensagem, "desistir")
                || contemPalavra(mensagem, "desisto")
                || contemPalavra(mensagem, "parar")
                || contemPalavra(mensagem, "sair")
                || contemPalavra(mensagem, "encerrar");
    }

    private boolean isAdicionarMais(String mensagem) {
        return mensagem.contains("comprar mais")
                || mensagem.contains("adicionar mais")
                || mensagem.contains("colocar mais")
                || mensagem.contains("incluir mais")
                || mensagem.contains("mais produto")
                || mensagem.contains("mais item")
                || mensagem.equals("mais");
    }

    private boolean isNegacao(String mensagem) {
        return contemPalavra(mensagem, "nao")
                || contemPalavra(mensagem, "não")
                || mensagem.contains("ainda nao")
                || mensagem.contains("ainda não");
    }

    private boolean isNegacaoDireta(String mensagem) {
        if (mensagem == null) {
            return false;
        }

        String msg = mensagem.trim();
        return msg.equals("nao")
                || msg.equals("não")
                || msg.equals("n")
                || msg.equals("negativo");
    }

    private boolean contemPalavra(String mensagem, String palavra) {
        return mensagem != null && mensagem.matches(".*\\b" + palavra + "\\b.*");
    }

    private void limparSessao(SessaoChat sessao) {
        sessao.setEstado(INICIAL);
        sessao.setProduto(null);
        sessao.setQuantidade(null);
        sessao.setTipoProdutoInteresse(null);
        sessao.setUltimaAtualizacao(LocalDateTime.now());
        carrinhoService.limpar(sessao);
    }
}
