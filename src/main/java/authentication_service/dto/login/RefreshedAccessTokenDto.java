package authentication_service.dto.login;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RefreshedAccessTokenDto {
    private String accessToken;
    private int expiresInMinutes;
}
