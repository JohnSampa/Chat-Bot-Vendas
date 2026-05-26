package unifan.chat_bot_vendas.dto;

import unifan.chat_bot_vendas.domain.Produto;

import java.util.List;

public record ItemPendente(
        String termo,
        Integer quantidade,
        List<Produto> opcoes
) {
}
