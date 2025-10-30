package authentication_service.exception;

import org.springframework.http.HttpStatus;

public class UserNotFoundException extends StatusCodeException {
    public UserNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
