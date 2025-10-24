package authentication_service.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ParsingException extends RuntimeException {
    private String errorMessage;
}
