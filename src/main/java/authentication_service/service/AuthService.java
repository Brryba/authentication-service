package authentication_service.service;

import authentication_service.dto.dto.UserRequestDto;
import authentication_service.dto.dto.UserResponseDto;
import authentication_service.entity.User;
import authentication_service.exception.LoginDuplicateException;
import authentication_service.mapper.UserMapper;
import authentication_service.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserResponseDto signUp(@Valid @RequestBody UserRequestDto userRequestDto) {
        if (userRepository.existsByLogin(userRequestDto.getLogin())) {
            throw new LoginDuplicateException("Login " + userRequestDto.getLogin() + " already exists");
        }

        User newUser = userMapper.toUser(userRequestDto);
        newUser.setPassword(passwordEncoder.encode(userRequestDto.getPassword()));

        User savedUser = userRepository.save(newUser);
        return userMapper.toUserResponseDto(savedUser);
    }
}
