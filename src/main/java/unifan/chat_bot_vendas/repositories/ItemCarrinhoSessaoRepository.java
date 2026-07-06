package unifan.chat_bot_vendas.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import unifan.chat_bot_vendas.domain.ItemCarrinhoSessao;
import unifan.chat_bot_vendas.domain.SessaoChat;

import java.util.List;
import java.util.Optional;

public interface ItemCarrinhoSessaoRepository extends JpaRepository<ItemCarrinhoSessao, Long> {

    List<ItemCarrinhoSessao> findBySessaoOrderByIdAsc(SessaoChat sessao);

    Optional<ItemCarrinhoSessao> findBySessaoAndProdutoId(SessaoChat sessao, Long produtoId);

    Optional<ItemCarrinhoSessao> findBySessaoAndProdutoIdAndTamanho(SessaoChat sessao, Long produtoId, String tamanho);

    void deleteBySessao(SessaoChat sessao);
}
