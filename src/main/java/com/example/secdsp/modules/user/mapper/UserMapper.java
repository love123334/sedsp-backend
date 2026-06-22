package com.example.secdsp.modules.user.mapper;

import com.example.secdsp.modules.user.dto.response.UserProfileResponse;
import com.example.secdsp.modules.user.dto.response.UserSummaryResponse;
import com.example.secdsp.modules.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "role", source = "role.name")
    @Mapping(target = "status", expression = "java(user.getStatus() != null ? user.getStatus().name() : null)")
    UserProfileResponse toProfileResponse(User user);

    @Mapping(target = "role", source = "role.name")
    @Mapping(target = "status", expression = "java(user.getStatus() != null ? user.getStatus().name() : null)")
    UserSummaryResponse toSummaryResponse(User user);
}