package authentication_service.service;

import authentication_service.dto.login.LoginResponseDto;
import authentication_service.dto.login.RefreshedAccessTokenDto;
import authentication_service.dto.user.UserRequestDto;
import authentication_service.dto.user.UserResponseDto;
import authentication_service.entity.RefreshToken;
import authentication_service.entity.User;
import authentication_service.exception.JwtTokenInvalidException;
import authentication_service.exception.LoginDuplicateException;
import authentication_service.exception.RefreshTokenExpiredException;
import authentication_service.exception.RefreshTokenNotFoundException;
import authentication_service.exception.UserNotFoundException;
import authentication_service.exception.WrongPasswordException;
import authentication_service.mapper.UserMapper;
import authentication_service.repository.RefreshTokenRepository;
import authentication_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@EnableScheduling
public class AuthService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${token.expiration.access-minutes}")
    private int jwtExpirationMinutes;
    @Value("${token.expiration.refresh-days}")
    private int refreshTokenExpirationDays;

    @Scheduled(fixedRate = 1000 * 60 * 10)
    @Transactional
    public void deleteExpiredTokensOnceInTenMinutes() {
        refreshTokenRepository.deleteByExpiresAtLessThan(LocalDateTime.now());
    }

    public UserResponseDto signUp(UserRequestDto userRequestDto) {
        if (userRepository.existsByLogin(userRequestDto.getLogin())) {
            throw new LoginDuplicateException("Login " + userRequestDto.getLogin() + " already exists");
        }

        User newUser = userMapper.toUser(userRequestDto);
        newUser.setPassword(passwordEncoder.encode(userRequestDto.getPassword()));

        User savedUser = userRepository.save(newUser);
        return userMapper.toUserResponseDto(savedUser);
    }

    @Transactional
    public LoginResponseDto login(UserRequestDto userRequestDto) {
        User storedUser = userRepository.findByLogin(userRequestDto.getLogin())
                .orElseThrow(() -> new UserNotFoundException
                        ("User with login " + userRequestDto.getLogin() + " not found"));

        if (!passwordEncoder.matches(userRequestDto.getPassword(), storedUser.getPassword())) {
            throw new WrongPasswordException("Wrong password, try again");
        }

        String accessToken = jwtUtil.generateAccessToken(storedUser.getId());

        RefreshToken newRefreshToken = RefreshToken.builder()
                .user(storedUser)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(refreshTokenExpirationDays))
                .build();

        RefreshToken savedRefreshToken = refreshTokenRepository.save(newRefreshToken);

        return LoginResponseDto
                .builder()
                .accessToken(accessToken)
                .refreshToken(savedRefreshToken.getToken().toString())
                .accessTokenExpiresInMinutes(jwtExpirationMinutes)
                .refreshTokenExpiresInDays(refreshTokenExpirationDays)
                .build();
    }

    public void verify(String accessToken) throws JwtTokenInvalidException {
        jwtUtil.validateAccessToken(accessToken);
    }

    public RefreshedAccessTokenDto refreshAccessToken(UUID refreshToken) {
        RefreshToken refreshTokenEntity = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new RefreshTokenNotFoundException(
                        HttpStatus.FORBIDDEN,
                        "Refresh token is invalid"));

        if (refreshTokenEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RefreshTokenExpiredException("Refresh token expired");
        }

        String newAccessToken = jwtUtil
                .generateAccessToken(refreshTokenEntity.getUser().getId());

        return RefreshedAccessTokenDto.builder()
                .accessToken(newAccessToken)
                .expiresInMinutes(jwtExpirationMinutes)
                .build();
    }

    @Transactional
    public void logout(UUID refreshToken) {
        refreshTokenRepository.findByToken(refreshToken).ifPresentOrElse(
                refreshTokenRepository::delete,
                () -> {
                    throw new RefreshTokenNotFoundException(
                            HttpStatus.BAD_REQUEST,
                            "Invalid refresh token or user already logged out");
                });
    }
}
