package unifan.chat_bot_vendas.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import unifan.chat_bot_vendas.domain.Produto;
import unifan.chat_bot_vendas.domain.Venda;
import unifan.chat_bot_vendas.repositories.VendaRepository;

import java.util.List;

@Service
public class VendaService {

    private static final Long USER_ID_PADRAO = 1L;

    @Autowired
    private VendaRepository vendaRepository;

    public Venda processarVenda(Produto produto,Integer quantidade){
        return processarVenda(produto, quantidade, null, null);
    }

    public Venda processarVenda(Produto produto, Integer quantidade, String formaPagamento, String dadosPagamento){
        Venda venda = new Venda();
        venda.setProduto(produto);
        venda.setQuantidade(quantidade);
        venda.setUserid(USER_ID_PADRAO);
        venda.setFormaPagamento(formaPagamento);
        venda.setDadosPagamento(dadosPagamento);
        venda = vendaRepository.save(venda);
        return venda;
    }

    public List<Venda> processarVendas(List<unifan.chat_bot_vendas.dto.ItemCarrinho> itens) {
        return processarVendas(itens, null, null);
    }

    public List<Venda> processarVendas(List<unifan.chat_bot_vendas.dto.ItemCarrinho> itens, String formaPagamento, String dadosPagamento) {
        return itens
                .stream()
                .map(item -> processarVenda(item.produto(), item.quantidade(), formaPagamento, dadosPagamento))
                .toList();
    }

    public List<Venda> getVendas(){
        return vendaRepository.findByUseridOrderByIdDesc(USER_ID_PADRAO);
    }
}
