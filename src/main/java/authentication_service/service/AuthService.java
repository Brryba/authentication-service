package authentication_service.service;

import authentication_service.dto.login.LoginResponseDto;
import authentication_service.dto.user.UserRequestDto;
import authentication_service.dto.user.UserResponseDto;
import authentication_service.entity.User;
import authentication_service.exception.LoginDuplicateException;
import authentication_service.exception.UserNotFoundException;
import authentication_service.exception.WrongPasswordException;
import authentication_service.mapper.UserMapper;
import authentication_service.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    @Value("${JWT_EXPIRATION_MINUTES}")
    private int jwtExpirationMinutes;

    public UserResponseDto signUp(@Valid @RequestBody UserRequestDto userRequestDto) {
        if (userRepository.existsByLogin(userRequestDto.getLogin())) {
            throw new LoginDuplicateException("Login " + userRequestDto.getLogin() + " already exists");
        }

        User newUser = userMapper.toUser(userRequestDto);
        newUser.setPassword(passwordEncoder.encode(userRequestDto.getPassword()));

        User savedUser = userRepository.save(newUser);
        return userMapper.toUserResponseDto(savedUser);
    }

    public LoginResponseDto login(@Valid @RequestBody UserRequestDto userRequestDto) {
        User storedUser = userRepository.findByLogin(userRequestDto.getLogin())
                .orElseThrow(() -> new UserNotFoundException
                        ("User with login " + userRequestDto.getLogin() + " not found"));

        if (!passwordEncoder.matches(userRequestDto.getPassword(), storedUser.getPassword())) {
            throw new WrongPasswordException("Wrong password, try again");
        }

        String accessToken = jwtService.generateAccessToken(storedUser.getId(),
                storedUser.getLogin());

        return LoginResponseDto
                .builder()
                .accessToken(accessToken)
                //TODO:DIFFERENT TOKENS, CURRENTLY DUPLICATED
                .refreshToken(accessToken)
                .expiresInMinutes(jwtExpirationMinutes)
                .build();
    }
}
