package unifan.chat_bot_vendas.dto;

import unifan.chat_bot_vendas.domain.Produto;

public record ItemTamanhoPendente(
        Produto produto,
        Integer quantidade
) {
}
