package unifan.chat_bot_vendas.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import unifan.chat_bot_vendas.domain.Intencao;
import unifan.chat_bot_vendas.domain.SessaoChat;
import unifan.chat_bot_vendas.domain.enums.EstadoSessao;
import unifan.chat_bot_vendas.repositories.SessaoChatRepository;

import java.time.LocalDateTime;

import static unifan.chat_bot_vendas.domain.enums.EstadoSessao.INICIAL;
import static unifan.chat_bot_vendas.domain.enums.EstadoSessao.SAIR_COMPRA;

@Service
public class ChatBotComandosService {

    @Autowired
    private ProdutoService produtoService;

    @Autowired
    private VendaService vendaService;

    @Autowired
    private SessaoChatRepository sessaoChatRepository;

    public String executarComandos(Intencao intencao,SessaoChat sessao){

        return switch (intencao.getTipoAcao()){
            case INICIAR_COMPRA -> {
                sessao.setEstado(EstadoSessao.AGUARDANDO_PRODUTO);
                sessao.setUltimaAtualizacao(LocalDateTime.now());
                sessaoChatRepository.save(sessao);


                StringBuilder builder = new StringBuilder();
                produtoService.buscarProdutos()
                        .forEach(produto -> builder.append(produto.toString()).append("\n"));

                builder.append("Qual produto deseja comprar?");
                yield builder.toString();

            }
            case CHECAR_ESTOQUE -> {
                StringBuilder builder = new StringBuilder();
                produtoService.buscarProdutos()
                        .forEach(produto -> builder.append(produto.toString()).append("\n"));
                yield builder.toString();
            }
            case  VERIFICAR_VENDAS -> {
                StringBuilder builder = new StringBuilder();

                vendaService.getVendas()
                        .forEach(venda -> builder.append(venda).append("\n"));

                yield builder.toString();
            }
            case SAIR_CHAT_VENDA -> {
                if(sessao.getEstado()!=INICIAL){
                    sessao.setEstado(SAIR_COMPRA);
                    sessao.setUltimaAtualizacao(LocalDateTime.now());
                    sessaoChatRepository.save(sessao);
                    yield intencao.getResposta()+"\n Cancelando compra...";
                }
                yield intencao.getResposta();
            }
            default -> {
                yield "Erro";
            }
        };
    }
}
