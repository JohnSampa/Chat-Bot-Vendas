package unifan.chat_bot_vendas.dto;

public record ChatbotRequest(
        String mensagem,
        String cpf,
        String clientSessionId,
        Boolean novaSessao
) {
}
