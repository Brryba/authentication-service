package authentication_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class RefreshTokenExpiredException extends ResponseStatusException {
    public RefreshTokenExpiredException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
