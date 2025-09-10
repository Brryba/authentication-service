package authentication_service.exception;

import org.springframework.http.HttpStatus;

public class RefreshTokenNotFoundException extends StatusCodeException {
    public RefreshTokenNotFoundException(HttpStatus status, String message) {
        super(status, message);
    }
}
