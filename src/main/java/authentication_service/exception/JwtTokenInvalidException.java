package authentication_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class JwtTokenInvalidException extends ResponseStatusException {
    public JwtTokenInvalidException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
