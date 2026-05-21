package unifan.chat_bot_vendas.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import unifan.chat_bot_vendas.domain.Intencao;
import unifan.chat_bot_vendas.domain.Produto;
import unifan.chat_bot_vendas.domain.SessaoChat;
import unifan.chat_bot_vendas.domain.Venda;
import unifan.chat_bot_vendas.domain.enums.TipoAcao;
import unifan.chat_bot_vendas.exceptions.BusinessException;
import unifan.chat_bot_vendas.repositories.SessaoChatRepository;

import java.time.LocalDateTime;

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

    public String processarMensagem(String mensagem) {
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
            return continuarSessao(sessao, msg);
        }

        Intencao intencao = intencaoService.detectarIntencao(msg);

        if (intencao == null) {
            throw new BusinessException("Desculpe não entendi");
        }

        if (intencao.getTipoAcao() != null){
            return chatBotComandosService.executarComandos(intencao,sessao);
        }

        return intencao.getResposta();
    }

    private String normalizar(String text) {
        return text.toLowerCase()
                .replaceAll("[^a-záéíóúãõâêô\\s]", "")
                .trim();
    }

    private String continuarSessao(final SessaoChat sessao, String mensagem) {
        Intencao intencao = intencaoService.findByTipoAcao(SAIR_CHAT_VENDA);

        String exit = chatBotComandosService.executarComandos(intencao,sessao);

        return switch (sessao.getEstado()) {
            case AGUARDANDO_PRODUTO -> {
                Produto produto = produtoService.buscarProdutoPorMensagem(mensagem);
                if (produto == null) {
                    sessao.setEstado(INICIAL);
                    sessao.setUltimaAtualizacao(LocalDateTime.now());
                    sessaoRepository.save(sessao);
                    yield "O produto não foi encontrado, tente novamente";
                }
                sessao.setProduto(produto);
                sessao.setUltimaAtualizacao(LocalDateTime.now());
                sessao.setEstado(AGUARDANDO_QUANTIDADE);
                sessaoRepository.save(sessao);

                yield "Qual a quantidade de " + produto.getNome() + " deseja";
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

                    yield "Confirma a compra de " + qty + "x " + sessao.getProduto().getNome() + " ?";
                } catch (NumberFormatException e) {
                    sessao.setUltimaAtualizacao(LocalDateTime.now());
                    sessaoRepository.save(sessao);
                    yield "Por favor, informe apenas o número de unidades.";
                }
            }
            case AGUARDANDO_CONFIRMACAO_COMPRA -> {
                if (mensagem.contains("confirmo") || mensagem.contains("sim") || mensagem.contains("confirmar")) {
                    Venda venda = vendaService.processarVenda(sessao.getProduto(), sessao.getQuantidade());

                    sessao.setProduto(null);
                    sessao.setQuantidade(null);
                    sessao.setEstado(INICIAL);
                    sessao.setUltimaAtualizacao(LocalDateTime.now());
                    sessaoRepository.save(sessao);

                    yield "Venda realizada com sucesso!\n" + venda.toString();

                } else if (mensagem.contains("cancelar") || mensagem.contains("não")
                        || mensagem.contains("parar") || mensagem.contains("nao")) {
                    sessao.setEstado(INICIAL);
                    sessao.setProduto(null);
                    sessao.setQuantidade(null);
                    sessao.setUltimaAtualizacao(LocalDateTime.now());
                    sessaoRepository.save(sessao);
                    yield "Tudo bem, compra cancelada.";
                } else {
                    sessao.setUltimaAtualizacao(LocalDateTime.now());
                    sessaoRepository.save(sessao);
                    yield "Deseja cancelar a compra ?";
                }
            }
            case  SAIR_COMPRA -> {
                sessao.setEstado(INICIAL);
                sessao.setUltimaAtualizacao(LocalDateTime.now());
                sessaoRepository.save(sessao);
                yield exit;
            }
            default -> {
                sessao.setEstado(INICIAL);
                sessao.setUltimaAtualizacao(LocalDateTime.now());
                sessaoRepository.save(sessao);
                yield "Sua mensagem foi inválida, ou algo deu errado na operação vamos recomeçar";
            }
        };
    }
}
