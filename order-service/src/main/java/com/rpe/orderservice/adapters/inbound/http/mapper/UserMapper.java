package com.rpe.orderservice.adapters.inbound.http.mapper;

import com.rpe.orderservice.adapters.inbound.http.dto.UserRequest;
import com.rpe.orderservice.adapters.inbound.http.dto.UserResponse;
import com.rpe.orderservice.core.domain.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "role", ignore = true)
    User toDomain(UserRequest dto);

    UserResponse toResponseDto(User user);
}