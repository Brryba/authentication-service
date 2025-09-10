package authentication_service.exception;

import org.springframework.http.HttpStatus;

public class JwtTokenInvalidException extends StatusCodeException {
    public JwtTokenInvalidException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
