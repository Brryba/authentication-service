package authentication_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class LoginDuplicateException extends ResponseStatusException {
    public LoginDuplicateException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
