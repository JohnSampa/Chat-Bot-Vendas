package unifan.chat_bot_vendas.dto;

import unifan.chat_bot_vendas.domain.Produto;

public record ItemCarrinho(
        Produto produto,
        Integer quantidade,
        Double total
) {
    public ItemCarrinho(Produto produto, Integer quantidade) {
        this(produto, quantidade, produto.getPreco() * quantidade);
    }
}
