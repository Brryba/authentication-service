package authentication_service.repository;

import authentication_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Integer> {
    User findByLogin(String login);
    boolean existsByLogin(String login);
}