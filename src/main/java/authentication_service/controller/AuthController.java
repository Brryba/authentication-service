package authentication_service.controller;

import authentication_service.dto.dto.UserRequestDto;
import authentication_service.dto.dto.UserResponseDto;
import authentication_service.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth/")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/signup")
    public UserResponseDto signup(@Valid @RequestBody UserRequestDto userRequestDto) {
        return authService.signUp(userRequestDto);
    }
}
