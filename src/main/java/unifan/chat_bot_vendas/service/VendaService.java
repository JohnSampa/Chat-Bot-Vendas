package unifan.chat_bot_vendas.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import unifan.chat_bot_vendas.domain.Produto;
import unifan.chat_bot_vendas.domain.Venda;
import unifan.chat_bot_vendas.repositories.VendaRepository;

import java.util.List;

@Service
public class VendaService {

    @Autowired
    private VendaRepository vendaRepository;

    public Venda processarVenda(Produto produto,Integer quantidade){
        Venda venda = new Venda();
        venda.setProduto(produto);
        venda.setQuantidade(quantidade);
        venda = vendaRepository.save(venda);
        return venda;
    }

    public List<Venda> processarVendas(List<unifan.chat_bot_vendas.dto.ItemCarrinho> itens) {
        return itens
                .stream()
                .map(item -> processarVenda(item.produto(), item.quantidade()))
                .toList();
    }

    public List<Venda> getVendas(){
        return vendaRepository.findAll();
    }
}
