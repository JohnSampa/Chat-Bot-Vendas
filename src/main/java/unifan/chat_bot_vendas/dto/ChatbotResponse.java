package unifan.chat_bot_vendas.dto;

import unifan.chat_bot_vendas.dto.enums.TipoResposta;

import java.util.List;

public record ChatbotResponse(
        TipoResposta tipo,
        String mensagem,
        List<?> lista,
        Object dados
) {

    public static ChatbotResponse mensagem(String texto) {
        return new ChatbotResponse(TipoResposta.MENSAGEM, texto, null, null);
    }

    public static ChatbotResponse lista(TipoResposta tipo, List<?> itens) {
        return new ChatbotResponse(tipo, null, itens, null);
    }

    public static ChatbotResponse listaMensagem(TipoResposta tipo, List<?> itens,String mensagem) {
        return new ChatbotResponse(tipo, mensagem, itens, null);
    }

    public static ChatbotResponse dados(TipoResposta tipo, Object objeto) {
        return new ChatbotResponse(tipo, null, null, objeto);
    }
}
