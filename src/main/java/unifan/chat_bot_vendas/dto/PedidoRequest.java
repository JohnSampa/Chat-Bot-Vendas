package unifan.chat_bot_vendas.dto;

public record PedidoRequest(
        Long produtoId,
        Integer quantidade,
        String tamanho,
        String formaPagamento,
        String dadosPagamento
) {
}
