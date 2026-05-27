package unifan.chat_bot_vendas.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import unifan.chat_bot_vendas.domain.Venda;

import java.util.List;

public interface VendaRepository extends JpaRepository<Venda, Long> {

    List<Venda> findByUseridOrderByIdDesc(Long userid);
}
