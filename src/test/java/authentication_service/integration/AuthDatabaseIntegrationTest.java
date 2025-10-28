package authentication_service.integration;

import authentication_service.dto.login.LoginResponseDto;
import authentication_service.dto.login.RefreshedAccessTokenDto;
import authentication_service.dto.user.UserRequestDto;
import authentication_service.dto.user.UserResponseDto;
import authentication_service.entity.User;
import authentication_service.exception.LoginDuplicateException;
import authentication_service.exception.RefreshTokenNotFoundException;
import authentication_service.exception.WrongPasswordException;
import authentication_service.repository.RefreshTokenRepository;
import authentication_service.repository.UserRepository;
import authentication_service.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
public class AuthDatabaseIntegrationTest {
    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private UserRequestDto userRequestDto;

    @Container
    public static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            "postgres:15-alpine"
    );

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @BeforeEach
    void beforeEach() {
        this.userRequestDto = UserRequestDto.builder()
                .login("login")
                .password("password")
                .build();
    }

    @Test
    @Transactional
    void signUp_success_savesUser() {
        UserResponseDto userDto = authService.signUp(userRequestDto);

        Optional<User> savedUser = userRepository.findByLogin(userDto.getLogin());
        assertTrue(savedUser.isPresent());
        assertNotEquals(savedUser.get().getPassword(), userRequestDto.getPassword());
        assertTrue(passwordEncoder.matches(userRequestDto.getPassword(), savedUser.get().getPassword()));
    }

    @Test
    @Transactional
    void signUp_fails_whenDuplicateLogin_andStoresOldPassword() {
        UserResponseDto userDto = authService.signUp(userRequestDto);

        userRequestDto.setPassword("another_password");
        assertThrows(LoginDuplicateException.class,
                () -> authService.signUp(userRequestDto));

        Optional<User> savedUser = userRepository.findByLogin(userDto.getLogin());
        assertTrue(savedUser.isPresent());
        assertTrue(passwordEncoder.matches("password", savedUser.get().getPassword()));
        assertFalse(passwordEncoder.matches("another_password", savedUser.get().getPassword()));
    }

    @Test
    @Transactional
    void login_success() {
        authService.signUp(userRequestDto);
        LoginResponseDto loginResponseDto = authService.login(userRequestDto);

        assertNotNull(loginResponseDto);
        assertNotNull(loginResponseDto.getAccessToken());
        assertNotNull(loginResponseDto.getRefreshToken());
        assertNotNull(refreshTokenRepository.findByToken(UUID.fromString(loginResponseDto.getRefreshToken())));

        assertDoesNotThrow(() -> authService.verify(loginResponseDto.getAccessToken()));
    }

    @Test
    @Transactional
    void login_fails_whenInvalidPassword() {
        authService.signUp(userRequestDto);
        userRequestDto.setPassword("invalid_password");

        assertThrows(WrongPasswordException.class, () -> authService.login(userRequestDto));
    }

    @Test
    @Transactional
    void refreshAccessToken_success() {
        authService.signUp(userRequestDto);
        LoginResponseDto loginResponseDto = authService.login(userRequestDto);
        RefreshedAccessTokenDto refreshedAccessTokenDto = authService
                .refreshAccessToken(UUID.fromString(loginResponseDto.getRefreshToken()));

        assertNotNull(refreshedAccessTokenDto);
    }

    @Test
    @Transactional
    void refreshAccessToken_fails_whenInvalidRefreshToken() {
        authService.signUp(userRequestDto);
        UUID uuid = UUID.randomUUID();
        assertThrows(RefreshTokenNotFoundException.class, () ->
                authService.refreshAccessToken(uuid));
    }

    @Test
    @Transactional
    void logout_success() {
        authService.signUp(userRequestDto);
        LoginResponseDto responseDto = authService.login(userRequestDto);
        assertNotNull(responseDto);

        authService.logout(UUID.fromString(responseDto.getRefreshToken()));
        assertFalse(refreshTokenRepository.findByToken(UUID.fromString(responseDto.getRefreshToken())).isPresent());
    }
}
