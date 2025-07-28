package authentication_service.unit_test;

import authentication_service.service.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = JwtUtil.class)
@ActiveProfiles("test")
public class JwtUnitTest {
    @Autowired
    private JwtUtil jwtUtil;
    @Value("${JWT_KEY}")
    private String key;

    @Test
    public void generateAccessToken_test() {
        String token = jwtUtil.generateAccessToken(1L);
        assertNotNull(token);
    }

    @Test
    public void validatesItsOwnAccessToken() {
        String token = jwtUtil.generateAccessToken(1L);
        assertNotNull(token);

        assertDoesNotThrow(() -> jwtUtil.validateAccessToken(token));
    }

    @Test
    public void checkJwtTokenReturnsCorrectId_andSetsCorrectExpirationDate() {
        String token = jwtUtil.generateAccessToken(1L);

        Jws<Claims> claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(key.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token);

        Date expiration = claims.getPayload().getExpiration();

        Long id = Long.parseLong(
                claims.getPayload()
                        .getSubject());

        assertNotNull(id);
        assertNotNull(expiration);

        assertEquals(1, id);
        assertDoesNotThrow(() -> jwtUtil.validateAccessToken(token));
        assertTrue(expiration.after(new Date()));
    }
}
