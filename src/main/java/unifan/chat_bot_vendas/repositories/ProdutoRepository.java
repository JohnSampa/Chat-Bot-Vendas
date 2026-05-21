package unifan.chat_bot_vendas.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import unifan.chat_bot_vendas.domain.Produto;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {


}
