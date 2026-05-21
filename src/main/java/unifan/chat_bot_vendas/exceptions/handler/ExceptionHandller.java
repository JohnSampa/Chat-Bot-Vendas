package unifan.chat_bot_vendas.exceptions.handler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import unifan.chat_bot_vendas.exceptions.BusinessException;

@RestControllerAdvice
public class ExceptionHandller {

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusinessException(BusinessException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problemDetail.setTitle(ex.getMessage());
        return problemDetail;
    }
}
