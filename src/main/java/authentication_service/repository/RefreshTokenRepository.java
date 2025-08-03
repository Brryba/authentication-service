package authentication_service.repository;

import authentication_service.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Integer> {
    Optional<RefreshToken> findByToken(UUID token);
    void deleteByToken(RefreshToken token);
    void deleteByExpiresAtLessThan(LocalDateTime now);
}
