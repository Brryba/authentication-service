package authentication_service.camunda;

import authentication_service.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
@ExternalTaskSubscription("auth_service_rollback")
@RequiredArgsConstructor
public class RollbackUserHandler implements ExternalTaskHandler {
    private final AuthService authService;

    @Override
    public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
        Long userId = externalTask.getVariable("user_id");
        log.info("Received create user external task for {} user", userId);

        try {
            authService.deleteUserById(userId);
            log.info("deleted user with ID: {}", userId);
        } catch (Exception e) {
            externalTaskService.handleFailure(externalTask,"Rollback failed", e.getMessage(),0, 0);
            log.error(e.getMessage());
            return;
        }

        externalTaskService.complete(externalTask);
    }
}
