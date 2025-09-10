package authentication_service.exception;

import org.springframework.http.HttpStatus;

public class LoginDuplicateException extends StatusCodeException {
    public LoginDuplicateException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
