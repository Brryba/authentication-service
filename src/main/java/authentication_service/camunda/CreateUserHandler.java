package authentication_service.camunda;

import authentication_service.dto.user.UserRequestDto;
import authentication_service.dto.user.UserResponseDto;
import authentication_service.exception.BpmnException;
import authentication_service.exception.ParsingException;
import authentication_service.service.AuthService;
import com.fasterxml.jackson.core.JsonProcessingException;
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

import java.util.Map;

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
        try {
            UserRequestDto authDto = readAuthRequest(externalTask);
            UserResponseDto userResponseDto = callAuthService(authDto);
            completeTask(externalTask, externalTaskService, userResponseDto);

        } catch (BpmnException e) {
            log.error("BPMN error: {}", e.getMessage());
            externalTaskService.setVariables(externalTask, Map.of("error", e.getMessage()));
            externalTaskService.handleBpmnError(externalTask, SERVICE_ERROR, e.getMessage());

        } catch (ParsingException e) {
            log.error("Parsing parameters failure: {}", e.getMessage(), e);
            externalTaskService.handleFailure(externalTask, "Error parsing JSON",
                    e.getErrorMessage(), 0, 0);

        }
    }

    private UserRequestDto readAuthRequest(ExternalTask externalTask) {
        JsonValue jsonValue = externalTask.getVariableTyped("authRequest");
        if (jsonValue == null) {
            throw new ParsingException("authRequest variable is missing");
        }
        try {
            UserRequestDto userRequest = objectMapper.readValue(jsonValue.getValue(), UserRequestDto.class);
            log.info("Received authRequest: {}", userRequest.getLogin());
            return userRequest;
        } catch (JsonProcessingException e) {
            throw new ParsingException("Failed to parse authRequest");
        }
    }

    private UserResponseDto callAuthService(UserRequestDto authDto) {
        try {
            UserResponseDto userResponse = authService.signUp(authDto);
            log.info("Created user with id {}", userResponse.getId());
            return userResponse;
        } catch (Exception e) {
            throw new BpmnException(e.getMessage());
        }
    }

    private void completeTask(ExternalTask externalTask, ExternalTaskService externalTaskService,
                              UserResponseDto userResponseDto) {
        VariableMap variables = Variables.createVariables();
        variables.put("user_id", userResponseDto.getId());
        try {
            variables.put("authResponse", objectMapper.writeValueAsString(userResponseDto));
        } catch (JsonProcessingException e) {
            throw new ParsingException("Failed to serialize authResponse");
        }
        externalTaskService.complete(externalTask, variables);
        log.info("Completed task by creating user {}", userResponseDto.getLogin());
    }
}