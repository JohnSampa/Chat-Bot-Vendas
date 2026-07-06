package unifan.chat_bot_vendas.dto;

import unifan.chat_bot_vendas.domain.Produto;

public record ItemCarrinho(
        Produto produto,
        Integer quantidade,
        String tamanho,
        Double total
) {
    public ItemCarrinho(Produto produto, Integer quantidade) {
        this(produto, quantidade, null);
    }

    public ItemCarrinho(Produto produto, Integer quantidade, String tamanho) {
        this(produto, quantidade, tamanho, calcularTotal(produto, quantidade));
    }

    private static Double calcularTotal(Produto produto, Integer quantidade) {
        if (produto == null || produto.getPreco() == null || quantidade == null) {
            return 0.0;
        }

        return produto.getPreco() * quantidade;
    }
}
