package unifan.chat_bot_vendas.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import unifan.chat_bot_vendas.domain.SessaoChat;

import java.util.Optional;

public interface SessaoChatRepository extends JpaRepository<SessaoChat, Long> {

    Optional<SessaoChat> findByUserid(Long id);
}
