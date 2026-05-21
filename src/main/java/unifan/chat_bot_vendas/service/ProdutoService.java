package unifan.chat_bot_vendas.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import unifan.chat_bot_vendas.domain.Produto;
import unifan.chat_bot_vendas.exceptions.BusinessException;
import unifan.chat_bot_vendas.repositories.ProdutoRepository;

import java.util.List;
import java.util.Optional;

@Service
public class ProdutoService {

    @Autowired
    private ProdutoRepository produtoRepository;

    public Produto buscarProdutoPorMensagem(String mensagem) {
        List<Produto> produtos = produtoRepository.findAll();
        Optional<Produto> produto = produtos
                .stream()
                .filter(produto1 -> mensagem.contains(produto1.getNome().toLowerCase()))
                .findFirst();

        return produto.orElse(null);
    }

    public List<Produto> buscarProdutos(){
        return produtoRepository.findAll();
    }

    public List<Produto> salveProdutos(List<Produto> produtos){
        produtos.forEach(produto -> produtoRepository.save(produto));
        return produtoRepository.findAll();
    }

}
