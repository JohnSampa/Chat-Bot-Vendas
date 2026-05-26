package unifan.chat_bot_vendas.exceptions.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import unifan.chat_bot_vendas.dto.ChatbotResponse;
import unifan.chat_bot_vendas.dto.enums.TipoResposta;
import unifan.chat_bot_vendas.exceptions.BusinessException;

@RestControllerAdvice
public class ExceptionHandller {

    private static final Logger log = LoggerFactory.getLogger(ExceptionHandller.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ChatbotResponse> handleBusinessException(BusinessException ex) {
        return ResponseEntity.ok(ChatbotResponse.mensagem(ex.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ChatbotResponse> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        log.error("Erro de integridade ao processar requisicao", ex);
        return ResponseEntity.ok(new ChatbotResponse(
                TipoResposta.ERRO,
                "Nao consegui salvar o estado da conversa. Verifique se o banco esta atualizado com as novas colunas e estados da sessao.",
                null,
                null
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ChatbotResponse> handleException(Exception ex) {
        log.error("Erro inesperado ao processar requisicao", ex);
        return ResponseEntity.ok(new ChatbotResponse(
                TipoResposta.ERRO,
                "Ocorreu um erro ao processar sua mensagem. Tente novamente ou reinicie a conversa.",
                null,
                null
        ));
    }
}
