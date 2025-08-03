package authentication_service.dto.login;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponseDto {
    private String accessToken;
    @JsonIgnore
    private String refreshToken;
    private int accessTokenExpiresInMinutes;
    private int refreshTokenExpiresInDays;
}
