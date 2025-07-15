package authentication_service.service;

import authentication_service.dto.login.LoginResponseDto;
import authentication_service.dto.login.RefreshTokenDto;
import authentication_service.dto.user.UserRequestDto;
import authentication_service.dto.user.UserResponseDto;
import authentication_service.entity.RefreshToken;
import authentication_service.entity.User;
import authentication_service.exception.LoginDuplicateException;
import authentication_service.exception.RefreshTokenNotFoundException;
import authentication_service.exception.UserNotFoundException;
import authentication_service.exception.WrongPasswordException;
import authentication_service.mapper.UserMapper;
import authentication_service.repository.RefreshTokenRepository;
import authentication_service.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${token.expiration.access-minutes}")
    private int jwtExpirationMinutes;
    @Value("${token.expiration.refresh-days}")
    private int refreshTokenExpirationDays;

    public UserResponseDto signUp(@Valid @RequestBody UserRequestDto userRequestDto) {
        if (userRepository.existsByLogin(userRequestDto.getLogin())) {
            throw new LoginDuplicateException("Login " + userRequestDto.getLogin() + " already exists");
        }

        User newUser = userMapper.toUser(userRequestDto);
        newUser.setPassword(passwordEncoder.encode(userRequestDto.getPassword()));

        User savedUser = userRepository.save(newUser);
        return userMapper.toUserResponseDto(savedUser);
    }

    @Transactional
    public LoginResponseDto login(@Valid @RequestBody UserRequestDto userRequestDto) {
        User storedUser = userRepository.findByLogin(userRequestDto.getLogin())
                .orElseThrow(() -> new UserNotFoundException
                        ("User with login " + userRequestDto.getLogin() + " not found"));

        if (!passwordEncoder.matches(userRequestDto.getPassword(), storedUser.getPassword())) {
            throw new WrongPasswordException("Wrong password, try again");
        }

        String accessToken = jwtService.generateAccessToken(storedUser.getId(),
                storedUser.getLogin());

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

    public void verify(String accessToken) {
        jwtService.validateAccessToken(accessToken);
    }

    public RefreshTokenDto refreshAccessToken(UUID refreshToken) {
        RefreshToken refreshTokenEntity = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new RefreshTokenNotFoundException("Refresh token is invalid"));

        String newAccessToken = jwtService
                .generateAccessToken(refreshTokenEntity.getUser().getId(),
                        refreshTokenEntity.getUser().getLogin());

        return RefreshTokenDto.builder()
                .refreshToken(newAccessToken)
                .expiresInMinutes(jwtExpirationMinutes)
                .build();
    }
}
