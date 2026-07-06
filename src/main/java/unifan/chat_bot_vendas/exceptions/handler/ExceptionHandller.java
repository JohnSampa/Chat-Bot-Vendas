package unifan.chat_bot_vendas.exceptions.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
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
        return ResponseEntity.ok(new ChatbotResponse(TipoResposta.ERRO, ex.getMessage(), null, null));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ChatbotResponse> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        log.error("Erro de integridade ao processar requisicao", ex);
        return ResponseEntity.ok(new ChatbotResponse(
                TipoResposta.ERRO,
                "Nao consegui salvar os dados porque o banco esta com constraints antigas. Atualize com sql-atualizacao-chatbot-robusto.sql ou rode o SQL de correcao especifico informado no projeto.",
                null,
                null
        ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ChatbotResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        log.error("Payload invalido ao processar requisicao", ex);
        return ResponseEntity.ok(new ChatbotResponse(
                TipoResposta.ERRO,
                "Nao consegui ler os dados enviados. Verifique se o JSON esta compativel com a versao atual do backend e reinicie a aplicacao apos atualizar os enums.",
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
