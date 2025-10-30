package authentication_service.exception;

import org.springframework.http.HttpStatus;

public class WrongPasswordException extends StatusCodeException {
    public WrongPasswordException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
