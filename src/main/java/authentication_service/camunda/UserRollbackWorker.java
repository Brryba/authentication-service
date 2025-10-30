package authentication_service.camunda;

import authentication_service.service.AuthService;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.annotation.Variable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserRollbackWorker {
    private final AuthService authService;

    @JobWorker(type = "auth_service_rollback")
    public Map<String, Object> handleUserRollback(@Variable("user_id") Long userId) {
        log.info("Received user {} rollback request", userId);

        try {
            authService.deleteUserById(userId);
            log.info("User {} was rolled back", userId);
        } catch (Exception e) {
            log.error("User {} rolled back failed! Check Camunda process incident", userId, e);
            throw e;
        }
        return Map.of();
    }
}
