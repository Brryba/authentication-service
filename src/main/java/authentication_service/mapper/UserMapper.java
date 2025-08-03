package authentication_service.mapper;

import authentication_service.dto.user.UserRequestDto;
import authentication_service.dto.user.UserResponseDto;
import authentication_service.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UserMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    User toUser(UserRequestDto requestDto);

    UserResponseDto toUserResponseDto(User user);
}
