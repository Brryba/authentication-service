package authentication_service.repository;

import authentication_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findById(Long userId);
    Optional<User> findByLogin(String login);
    boolean existsByLogin(String login);
}