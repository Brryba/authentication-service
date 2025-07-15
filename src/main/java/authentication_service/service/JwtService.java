package authentication_service.service;

import authentication_service.dto.login.LoginResponseDto;
import authentication_service.exception.JwtTokenInvalidException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {
    @Value("${JWT_KEY}")
    private String jwtKey;
    @Value("${token.expiration.access-minutes}")
    private int jwtExpirationInMinutes;
    private SecretKey key;

    @PostConstruct
    public void setJwtKey() {
        this.key = Keys.hmacShaKeyFor(jwtKey.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(Long id, String login) {
        return Jwts.builder()
                .subject(login)
                .subject(id.toString())
                .issuedAt(new Date())
                .expiration(new Date((new Date()).getTime()
                        + (long) jwtExpirationInMinutes * 60 * 1000))
                .signWith(key)
                .compact();
    }

    public void validateAccessToken(String accessToken) {
        JwtParser jwtParser = Jwts.parser().
                verifyWith(key)
                .build();
        try {
            jwtParser.parse(accessToken);
        } catch (Exception e) {
            throw new JwtTokenInvalidException(e.getMessage());
        }
    }
}
