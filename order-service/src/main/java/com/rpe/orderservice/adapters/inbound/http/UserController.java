package com.rpe.orderservice.adapters.inbound.http;

import com.rpe.orderservice.adapters.inbound.http.dto.UserRequest;
import com.rpe.orderservice.adapters.inbound.http.dto.UserResponse;
import com.rpe.orderservice.adapters.inbound.http.mapper.UserMapper;
import com.rpe.orderservice.core.domain.User;
import com.rpe.orderservice.core.ports.inbound.CreateUserUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final CreateUserUseCase createUserUseCase;
    private final UserMapper userMapper;

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest dto) {
        User user = userMapper.toDomain(dto);

        User savedUser = createUserUseCase.execute(user);

        return ResponseEntity.status(HttpStatus.CREATED).body(userMapper.toResponseDto(savedUser));
    }
}
