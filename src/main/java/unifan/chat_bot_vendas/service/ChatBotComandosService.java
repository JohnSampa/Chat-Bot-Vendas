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

    public ChatbotResponse executarComandos(Intencao intencao, SessaoChat sessao){

        return switch (intencao.getTipoAcao()){
            case INICIAR_COMPRA -> {
                sessao.setEstado(EstadoSessao.AGUARDANDO_PRODUTO);
                sessao.setTipoProdutoInteresse(null);
                sessao.setUltimaAtualizacao(LocalDateTime.now());
                sessaoChatRepository.save(sessao);

                yield ChatbotResponse
                        .listaMensagem(
                                TipoResposta.LISTA_PRODUTOS_MENSAGEM,
                                produtoService.buscarProdutos(),
                                "Qual o id do produto que deseja comprar?"
                        );

            }
            case CHECAR_ESTOQUE -> {
                var produtos = produtoService.buscarProdutos();
                yield ChatbotResponse.lista(TipoResposta.LISTA_PRODUTOS,produtos);
            }
            case  VERIFICAR_VENDAS -> {
                var vedas = vendaService.getVendas();
                yield ChatbotResponse.lista(TipoResposta.LISTA_VENDAS,vedas);
            }
            case SAIR_CHAT_VENDA -> {
                if(sessao.getEstado()!=INICIAL){
                    sessao.setEstado(INICIAL);
                    sessao.setProduto(null);
                    sessao.setQuantidade(null);
                    sessao.setTipoProdutoInteresse(null);
                    sessao.setUltimaAtualizacao(LocalDateTime.now());
                    sessaoChatRepository.save(sessao);
                    yield ChatbotResponse.mensagem(intencao.getResposta()+"\n Cancelando compra...");
                }
                yield ChatbotResponse.mensagem(intencao.getResposta() );
            }
            case COMPRAR_CAMISA -> iniciarCompraProduto(sessao, "camisa");
            case COMPRAR_CALCA -> iniciarCompraProduto(sessao, "calca");
            case COMPRAR_BLUSA -> iniciarCompraProduto(sessao, "blusa");
            case COMPRAR_SAPATO -> iniciarCompraProduto(sessao, "sapato");
            default -> {
                yield new ChatbotResponse(
                        TipoResposta.ERRO,
                        "Desculpa não entendi",
                        null,
                        null
                );
            }
        };
    }

    private ChatbotResponse iniciarCompraProduto(SessaoChat sessao, String nomeProduto) {
        List<Produto> produtos = produtoService.buscarProdutosPorNomeParcial(nomeProduto);

        if (produtos.isEmpty()) {
            sessao.setEstado(INICIAL);
            sessao.setProduto(null);
            sessao.setQuantidade(null);
            sessao.setTipoProdutoInteresse(null);
            sessao.setUltimaAtualizacao(LocalDateTime.now());
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
        sessao.setEstado(EstadoSessao.AGUARDANDO_PRODUTO);
        sessao.setUltimaAtualizacao(LocalDateTime.now());
        sessaoChatRepository.save(sessao);

        return ChatbotResponse.listaMensagem(
                TipoResposta.LISTA_PRODUTOS_MENSAGEM,
                produtos,
                "Encontrei estas opcoes de " + nomeProduto + ". Qual o id do produto que deseja comprar?"
        );
    }
}
