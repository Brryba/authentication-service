package authentication_service.dto.error;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Builder
@Getter
@Setter
public class ValidationErrorDto {
    private long timestamp;
    private int status;
    private String error;
    private String message;
    private List<String> errors;
}
