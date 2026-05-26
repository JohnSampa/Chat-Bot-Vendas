package unifan.chat_bot_vendas.dto;

import java.util.List;

public record CarrinhoResponse(
        List<ItemCarrinho> itens,
        Double total
) {
}
