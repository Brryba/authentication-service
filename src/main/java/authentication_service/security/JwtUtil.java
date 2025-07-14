package authentication_service.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {
    @Value("${JWT_KEY}")
    private String JwtKey;
    @Value("${JWT_EXPIRATION_MINUTES}")
    private int jwtExpirationInMinutes;
    private SecretKey key;

    @PostConstruct
    public void setJwtKey() {
        this.key = Keys.hmacShaKeyFor(JwtKey.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Integer id, String login) {
        return Jwts.builder()
                .subject(login)
                .subject(id.toString())
                .issuedAt(new Date())
                .expiration(new Date((new Date()).getTime()
                        + (long) jwtExpirationInMinutes * 60 * 1000))
                .signWith(key)
                .compact();
    }
}
