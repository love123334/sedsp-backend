package com.example.secdsp.modules.user.mapper;

import com.example.secdsp.modules.user.dto.internal.UserInfo;
import com.example.secdsp.modules.user.dto.response.UserProfileResponse;
import com.example.secdsp.modules.user.dto.response.UserSummaryResponse;
import com.example.secdsp.modules.user.entity.Role;
import com.example.secdsp.modules.user.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserProfileResponse toProfileResponse(User user);

    UserSummaryResponse toSummaryResponse(User user);

    UserInfo toUserInfo(User user);

    default String map(Role role) {
        return role != null ? role.getName() : null;
    }
}