package authentication_service.controller;

import authentication_service.dto.login.LoginResponseDto;
import authentication_service.dto.login.RefreshedAccessTokenDto;
import authentication_service.dto.user.UserRequestDto;
import authentication_service.dto.user.UserResponseDto;
import authentication_service.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    @Value("${token.expiration.refresh-days}")
    private int refreshTokenExpirationDays;

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponseDto signup(@Valid @RequestBody UserRequestDto userRequestDto) {
        return authService.signUp(userRequestDto);
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public LoginResponseDto login(@Valid @RequestBody UserRequestDto userRequestDto,
                                  HttpServletResponse response) {
        LoginResponseDto responseDto = authService.login(userRequestDto);
        setRefreshTokenCookies(response, responseDto.getRefreshToken());
        return responseDto;
    }

    @GetMapping("/verify")
    @ResponseStatus(HttpStatus.OK)
    public String verify(@NotNull @RequestParam(name = "token") String accessToken) {
        authService.verify(accessToken);
        return "Token verified!";
    }

    @PostMapping("/refresh")
    @ResponseStatus(HttpStatus.OK)
    public RefreshedAccessTokenDto refresh(@CookieValue("refreshToken") UUID refreshToken) {
        return authService.refreshAccessToken(refreshToken);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@CookieValue("refreshToken") UUID refreshToken) {
        authService.logout(refreshToken);
    }

    private void setRefreshTokenCookies(HttpServletResponse response, String refreshToken) {
        Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setPath("/api/auth");
        refreshTokenCookie.setMaxAge(refreshTokenExpirationDays * 24 * 60 * 60);
        response.addCookie(refreshTokenCookie);
    }
}
