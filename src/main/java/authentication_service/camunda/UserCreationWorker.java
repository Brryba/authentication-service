package authentication_service.camunda;

import authentication_service.dto.user.UserRequestDto;
import authentication_service.dto.user.UserResponseDto;
import authentication_service.exception.LoginDuplicateException;
import authentication_service.exception.ParsingException;
import authentication_service.service.AuthService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.annotation.Variable;
import io.camunda.client.exception.BpmnError;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserCreationWorker {
    private final ObjectMapper objectMapper;
    private final AuthService authService;

    @JobWorker(type = "auth_service_create")
    public Map<String, Object> handleUserCreation(@Variable String authRequest) {
        UserRequestDto authDto = readAuthRequest(authRequest);
        UserResponseDto userResponseDto = callAuthService(authDto);
        return createResponseMap(userResponseDto);
    }

    private UserRequestDto readAuthRequest(String authRequest) {
        UserRequestDto userRequestDto;
        try {
            userRequestDto = objectMapper.readValue(authRequest, UserRequestDto.class);
            log.info("Received authRequest: login={}", userRequestDto.getLogin());
        } catch (JsonProcessingException e) {
            log.error("Error parsing authRequest:", e);
            throw new ParsingException("Failed to serialize authResponse");
        }
        return userRequestDto;
    }

    private UserResponseDto callAuthService(UserRequestDto authDto) {
        try {
            UserResponseDto userResponse = authService.signUp(authDto);
            log.info("Created user with id {}", userResponse.getId());
            return userResponse;
        } catch (LoginDuplicateException e) {
            log.warn("Login duplicate exception. Login {} is not unique. Bpmn AUTH_SERVICE_ERROR was thrown", authDto.getLogin());
            throw new BpmnError(
                    "AUTH_SERVICE_ERROR",
                    e.getMessage(),
                    Map.of("error", e.getMessage()),
                    e
            );
        }
    }

    private Map<String, Object> createResponseMap(UserResponseDto userResponseDto) {
        String authResponse;
        try {
            authResponse = objectMapper.writeValueAsString(userResponseDto);
        } catch (JsonProcessingException e) {
            log.error("Error parsing authResponse:", e);
            throw new ParsingException("Failed to serialize authResponse");
        }
        log.info("Completed task by creating user {}", userResponseDto.getLogin());
        return Map.of("authResponse", authResponse,
                "user_id", userResponseDto.getId());
    }
}
