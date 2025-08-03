package authentication_service.dto.user;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserRequestDto {
    @Size(min = 2, message = "Login must not be empty")
    @Size(max = 100, message = "Login must not be longer than 100 symbols")
    private String login;
    @NotEmpty(message = "Password is required")
    @Size(min = 8, message = "Password not secure. Must contain at least 8 symbols")
    private String password;
}
