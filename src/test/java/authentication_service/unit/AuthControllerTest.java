package authentication_service.unit;

import authentication_service.controller.AuthController;
import authentication_service.controller.GlobalExceptionHandler;
import authentication_service.dto.login.LoginResponseDto;
import authentication_service.dto.login.RefreshedAccessTokenDto;
import authentication_service.dto.user.UserRequestDto;
import authentication_service.dto.user.UserResponseDto;
import authentication_service.exception.LoginDuplicateException;
import authentication_service.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        AuthController.class,
        GlobalExceptionHandler.class
})
public class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;


    @Value("${token.expiration.access-minutes}")
    private int jwtExpirationMinutes;
    @Value("${token.expiration.refresh-days}")
    private int refreshTokenExpirationDays;

    private UserRequestDto userRequestDto;
    private UserResponseDto userResponseDto;
    private LoginResponseDto loginResponseDto;
    private RefreshedAccessTokenDto refreshedAccessTokenDto;


    @BeforeEach
    void setUp() {
        userRequestDto = UserRequestDto.builder()
                .login("login")
                .password("password")
                .build();

        userResponseDto = UserResponseDto.builder()
                .login("login")
                .id(1L)
                .build();

        loginResponseDto = LoginResponseDto.builder()
                .accessToken("accessToken")
                .refreshToken("refreshToken")
                .refreshTokenExpiresInDays(refreshTokenExpirationDays)
                .accessTokenExpiresInMinutes(jwtExpirationMinutes)
                .build();

        refreshedAccessTokenDto = RefreshedAccessTokenDto.builder()
                .accessToken("newAccessToken")
                .expiresInMinutes(jwtExpirationMinutes)
                .build();
    }

    @Test
    void testSignUp_success_201() throws Exception {
        when(authService.signUp(any())).thenReturn(userResponseDto);

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequestDto))).
                andExpect(status().isCreated())
                .andExpect(jsonPath("$.login").value(userResponseDto.getLogin()));
    }

    @Test
    void testLogin_smallPassword_400() throws Exception {
        userRequestDto.setPassword("pw");
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequestDto)))
                        .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void testSignUp_duplicate_409() throws Exception {
        when(authService.signUp(any())).thenThrow(new LoginDuplicateException("duplicate login"));
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequestDto))).
                andExpect(status().isConflict());
    }

    @Test
    void testLogin_success_200_andSetsRefreshTokenAsCookie() throws Exception {
        UUID uuid = UUID.randomUUID();
        loginResponseDto.setRefreshToken(uuid.toString());
        when(authService.login(any())).thenReturn(loginResponseDto);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequestDto))).
                andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(loginResponseDto.getAccessToken()))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(cookie().value("refreshToken", loginResponseDto.getRefreshToken()))
                .andExpect(cookie().maxAge("refreshToken", refreshTokenExpirationDays * 24 * 60 * 60))
                .andExpect(cookie().httpOnly("refreshToken", true))
                .andExpect(cookie().path("refreshToken", "/api/auth"));
    }

    @Test
    void testTokenVerifier_success_200() throws Exception {
        mockMvc.perform(get("/api/auth/verify?token=accessToken")).
                andExpect(status().isOk())
                .andExpect(content().string("Token verified!"));
    }

    @Test
    void testRefreshAccessToken_success_200() throws Exception {
        UUID uuid = UUID.randomUUID();
        Cookie cookie = new Cookie("refreshToken", uuid.toString());

        when(authService.refreshAccessToken(uuid)).thenReturn(refreshedAccessTokenDto);

        mockMvc.perform(post("/api/auth/refresh")
                .cookie(cookie))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(refreshedAccessTokenDto.getAccessToken()))
                .andExpect(jsonPath("$.refreshToken").doesNotExist());
    }

    @Test
    void testRefreshAccessToken_noRefreshTokenCookie_400() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void testLogout_success_204() throws Exception {
        Cookie cookie = new Cookie("refreshToken", UUID.randomUUID().toString());

        mockMvc.perform(post("/api/auth/logout")
                        .cookie(cookie))
                .andDo(print())
                .andExpect(status().isNoContent());
    }
}
