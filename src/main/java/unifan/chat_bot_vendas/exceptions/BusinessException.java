package unifan.chat_bot_vendas.exceptions;

public class BusinessException extends RuntimeException {
    public BusinessException(String mensagem){
        super(mensagem);
    }
}
