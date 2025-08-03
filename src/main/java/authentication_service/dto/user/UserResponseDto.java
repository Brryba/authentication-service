package authentication_service.dto.user;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponseDto {
    private Long id;
    private String login;
}
