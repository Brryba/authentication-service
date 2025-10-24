package authentication_service.camunda;

import authentication_service.dto.user.UserRequestDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.camunda.bpm.client.variable.value.JsonValue;
import org.camunda.bpm.engine.variable.VariableMap;
import org.camunda.bpm.engine.variable.Variables;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@ExternalTaskSubscription("auth_service_create")
@RequiredArgsConstructor
public class CreateUserHandler implements ExternalTaskHandler {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
        log.info("Received create user external task");

        try {
            JsonValue jsonValue = externalTask.getVariableTyped("authRequest");
            if (jsonValue == null) {
                log.warn("authRequest variable is null!");
                externalTaskService.handleFailure(externalTask, "Missing authRequest", "", 0, 0);
                return;
            }

            String jsonString = jsonValue.getValue();
            UserRequestDto authDto = objectMapper.readValue(jsonString, UserRequestDto.class);
            System.out.println(authDto);

            VariableMap variables = Variables.createVariables();
            variables.put("user_id", 1);
            externalTaskService.complete(externalTask, variables);
        } catch (Exception e) {
            log.error("Error processing authRequest", e);
            externalTaskService.handleFailure(externalTask, e.getMessage(), e.getMessage(), 0, 0);
        }
    }
}