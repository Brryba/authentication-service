package authentication_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class RefreshTokenNotFoundException extends ResponseStatusException {
    public RefreshTokenNotFoundException(HttpStatus status, String message) {
        super(status, message);
    }
}
