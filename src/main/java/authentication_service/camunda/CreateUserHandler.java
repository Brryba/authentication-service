package authentication_service.camunda;

import authentication_service.dto.user.UserRequestDto;
import authentication_service.dto.user.UserResponseDto;
import authentication_service.entity.User;
import authentication_service.service.AuthService;
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
    private final String SERVICE_ERROR = "AUTH_SERVICE_ERROR";
    private final ObjectMapper objectMapper;
    private final AuthService authService;

    @Override
    public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
        log.info("Received create user external task");

        try {
            JsonValue jsonValue = externalTask.getVariableTyped("authRequest");
            if (jsonValue == null) {
                log.error("authRequest variable is null!");
                externalTaskService.handleBpmnError(externalTask, SERVICE_ERROR, "The authRequest " +
                        "variable was not provided");
                return;
            }

            String jsonString = jsonValue.getValue();
            UserRequestDto authDto = objectMapper.readValue(jsonString, UserRequestDto.class);
            log.info("created authentication dto: {}", authDto);

            UserResponseDto userResponseDto;
            try {
                userResponseDto = authService.signUp(authDto);
                log.info("created user with ID: {}", userResponseDto.getId());
            } catch (Exception e) {
                externalTaskService.handleBpmnError(externalTask, SERVICE_ERROR, e.getMessage());
                log.error(e.getMessage());
                return;
            }

            VariableMap variables = Variables.createVariables();
            variables.put("user_id", userResponseDto.getId());
            externalTaskService.complete(externalTask, variables);
        } catch (Exception e) {
            log.error("Error processing authRequest", e);
            externalTaskService.handleFailure(externalTask, e.getMessage(), e.getMessage(), 0, 0);
        }
    }
}