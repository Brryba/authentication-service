package authentication_service.dto.user;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class UserRequestDto {
    @Size(min = 2, message = "Login must not be empty")
    @Size(max = 100, message = "Login must not be longer than 100 symbols")
    private String login;
    @NotEmpty(message = "Password is required")
    @Size(min = 8, message = "Password not secure. Must contain at least 8 symbols")
    private String password;
}
