package authentication_service.unit;

import authentication_service.dto.login.LoginResponseDto;
import authentication_service.dto.login.RefreshedAccessTokenDto;
import authentication_service.dto.user.UserRequestDto;
import authentication_service.dto.user.UserResponseDto;
import authentication_service.entity.RefreshToken;
import authentication_service.entity.User;
import authentication_service.exception.LoginDuplicateException;
import authentication_service.exception.RefreshTokenExpiredException;
import authentication_service.exception.UserNotFoundException;
import authentication_service.exception.WrongPasswordException;
import authentication_service.mapper.UserMapperImpl;
import authentication_service.repository.RefreshTokenRepository;
import authentication_service.repository.UserRepository;
import authentication_service.service.AuthService;
import authentication_service.service.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {
        AuthService.class,
        UserMapperImpl.class,
        PasswordEncoder.class,
        BCryptPasswordEncoder.class
})
@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private RefreshTokenRepository refreshTokenRepository;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Autowired
    private AuthService authService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${token.expiration.access-minutes}")
    private int jwtExpirationMinutes;
    @Value("${token.expiration.refresh-days}")
    private int refreshTokenExpirationDays;

    private User user;
    private UserRequestDto userRequestDto;
    private RefreshToken refreshToken;

    @BeforeEach
    void setUp() {
        this.user = User.builder().
                login("login").
                password(passwordEncoder.encode("password")).
                build();

        this.userRequestDto = UserRequestDto.builder()
                .login("login")
                .password("password")
                .build();

        this.refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID())
                .user(user)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(jwtExpirationMinutes))
                .build();


        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userRepository.findByLogin("login")).thenReturn(Optional.of(user));
    }

    @Test
    void test_signUpWithNewUser() {
        UserResponseDto responseDto = authService.signUp(userRequestDto);

        assertNotNull(responseDto);
        assertEquals(userRequestDto.getLogin(), responseDto.getLogin());
    }

    @Test
    void test_signUp_whenLoginAlreadyExists() {
        when(userRepository.existsByLogin(userRequestDto.getLogin())).thenReturn
                (true);

        assertThrows(LoginDuplicateException.class,
                () -> authService.signUp(userRequestDto));
    }

    @Test
    void test_login_success() {
        when(jwtUtil.generateAccessToken(any())).thenReturn("access_token");
        when(refreshTokenRepository.save(any())).thenReturn(refreshToken);

        LoginResponseDto loginResponseDto = authService.login(userRequestDto);

        assertNotNull(loginResponseDto);
        assertEquals("access_token", loginResponseDto.getAccessToken());
        assertNotNull(loginResponseDto.getRefreshToken());
        assertEquals(refreshTokenExpirationDays, loginResponseDto.getRefreshTokenExpiresInDays());
        assertEquals(jwtExpirationMinutes, loginResponseDto.getAccessTokenExpiresInMinutes());
    }

    @Test
    void test_loginFails_whenUserDoesNotExist() {
        when(userRepository.findByLogin(userRequestDto.getLogin())).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> authService.login(userRequestDto));
    }

    @Test
    void test_loginFails_whenPasswordDoesNotMatch() {
        userRequestDto.setPassword("wrong_password");

        assertThrows(WrongPasswordException.class, () -> authService.login(userRequestDto));
    }

    @Test
    void refreshToken_success() {
        when(refreshTokenRepository.findByToken(any())).thenReturn(Optional.of(refreshToken));
        when(jwtUtil.generateAccessToken(any())).thenReturn("access_token");

        RefreshedAccessTokenDto refreshedAccessTokenDto = authService.refreshAccessToken(UUID.randomUUID());
        assertNotNull(refreshedAccessTokenDto);
        assertEquals("access_token", refreshedAccessTokenDto.getAccessToken());
        assertEquals(jwtExpirationMinutes, refreshedAccessTokenDto.getExpiresInMinutes());
    }

    @Test
    void refreshToken_whenRefreshTokenExpired_throwsException() {
        refreshToken.setExpiresAt(LocalDateTime.now().minusHours(100L));
        when(refreshTokenRepository.findByToken(any())).thenReturn(Optional.of(refreshToken));

        UUID uuid = UUID.randomUUID();
        assertThrows(RefreshTokenExpiredException.class, () -> authService.refreshAccessToken(uuid));
    }

    @Test
    void logout_success() {
        when(refreshTokenRepository.findByToken(any())).thenReturn(Optional.of(refreshToken));

        authService.logout(UUID.randomUUID());

        verify(refreshTokenRepository, times(1)).delete(any());
    }
}
