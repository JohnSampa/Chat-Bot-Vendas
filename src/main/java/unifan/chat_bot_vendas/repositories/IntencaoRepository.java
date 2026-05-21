package unifan.chat_bot_vendas.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import unifan.chat_bot_vendas.domain.Intencao;
import unifan.chat_bot_vendas.domain.enums.TipoAcao;

import java.util.Optional;

public interface IntencaoRepository extends JpaRepository<Intencao, String> {

    Intencao findByTipoAcao(TipoAcao tipoAcao);
}
