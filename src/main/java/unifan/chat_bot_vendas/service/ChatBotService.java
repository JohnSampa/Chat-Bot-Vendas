package unifan.chat_bot_vendas.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import unifan.chat_bot_vendas.domain.Intencao;
import unifan.chat_bot_vendas.domain.Produto;
import unifan.chat_bot_vendas.domain.SessaoChat;
import unifan.chat_bot_vendas.domain.Venda;
import unifan.chat_bot_vendas.domain.enums.TipoAcao;
import unifan.chat_bot_vendas.dto.ChatbotResponse;
import unifan.chat_bot_vendas.dto.enums.TipoResposta;
import unifan.chat_bot_vendas.exceptions.BusinessException;
import unifan.chat_bot_vendas.repositories.SessaoChatRepository;

import java.time.LocalDateTime;
import java.util.List;

import static unifan.chat_bot_vendas.domain.enums.EstadoSessao.AGUARDANDO_PRODUTO;
import static unifan.chat_bot_vendas.domain.enums.EstadoSessao.AGUARDANDO_CONFIRMACAO_PRODUTO;
import static unifan.chat_bot_vendas.domain.enums.EstadoSessao.AGUARDANDO_CONFIRMACAO_COMPRA;
import static unifan.chat_bot_vendas.domain.enums.EstadoSessao.AGUARDANDO_QUANTIDADE;
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

    public ChatbotResponse reiniciarConversa() {
        SessaoChat sessao = sessaoRepository.findByUserid(1L)
                .orElse(new SessaoChat());

        sessao.setEstado(INICIAL);
        sessao.setProduto(null);
        sessao.setQuantidade(null);
        sessao.setTipoProdutoInteresse(null);
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
                if (mensagem.contains("confirmo") || mensagem.contains("sim") || mensagem.contains("confirmar")) {
                    sessao.setEstado(AGUARDANDO_QUANTIDADE);
                    sessao.setUltimaAtualizacao(LocalDateTime.now());
                    sessaoRepository.save(sessao);

                    yield new ChatbotResponse(
                            TipoResposta.PRODUTO_MENSAGEM,
                            "Qual a quantidade deseja",
                            null,
                            sessao.getProduto()
                    );
                } else if (mensagem.contains("cancelar") || mensagem.contains("nao") || mensagem.contains("não")
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
                if (mensagem.contains("confirmo") || mensagem.contains("sim") || mensagem.contains("confirmar")) {
                    Venda venda = vendaService.processarVenda(sessao.getProduto(), sessao.getQuantidade());

                    sessao.setProduto(null);
                    sessao.setQuantidade(null);
                    sessao.setTipoProdutoInteresse(null);
                    sessao.setEstado(INICIAL);
                    sessao.setUltimaAtualizacao(LocalDateTime.now());
                    sessaoRepository.save(sessao);

                    yield new ChatbotResponse(
                            TipoResposta.VENDA_MENSAGEM,
                            "Venda realizada com sucesso!",
                            null,
                            venda
                    );

                } else if (mensagem.contains("cancelar") || mensagem.contains("não")
                        || mensagem.contains("parar") || mensagem.contains("nao")) {
                    sessao.setEstado(INICIAL);
                    sessao.setProduto(null);
                    sessao.setQuantidade(null);
                    sessao.setTipoProdutoInteresse(null);
                    sessao.setUltimaAtualizacao(LocalDateTime.now());
                    sessaoRepository.save(sessao);
                    yield ChatbotResponse.mensagem("Tudo bem, compra cancelada.");
                } else {
                    sessao.setUltimaAtualizacao(LocalDateTime.now());
                    sessaoRepository.save(sessao);
                    yield ChatbotResponse.mensagem("Deseja cancelar a compra ?");
                }
            }
            case  SAIR_COMPRA -> {
                sessao.setEstado(INICIAL);
                sessao.setProduto(null);
                sessao.setQuantidade(null);
                sessao.setTipoProdutoInteresse(null);
                sessao.setUltimaAtualizacao(LocalDateTime.now());
                sessaoRepository.save(sessao);
                yield ChatbotResponse.mensagem("Tudo bem, compra cancelada.");
            }
            default -> {
                sessao.setEstado(INICIAL);
                sessao.setProduto(null);
                sessao.setQuantidade(null);
                sessao.setTipoProdutoInteresse(null);
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
}
