package authentication_service.exception;

import org.springframework.http.HttpStatus;

public class RefreshTokenExpiredException extends StatusCodeException {
    public RefreshTokenExpiredException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
