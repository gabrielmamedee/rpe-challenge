package com.rpe.orderservice.adapters.inbound.http;

import com.rpe.orderservice.adapters.inbound.http.dto.LoginRequestDto;
import com.rpe.orderservice.config.security.TokenService;
import com.rpe.orderservice.core.domain.User;
import com.rpe.orderservice.core.ports.outbound.UserRepositoryPort;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/login")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final UserRepositoryPort userRepositoryPort;

    @PostMapping
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody LoginRequestDto dto) {

        var usernamePassword = new UsernamePasswordAuthenticationToken(dto.login(), dto.password());
        var auth = this.authenticationManager.authenticate(usernamePassword);
        User user = userRepositoryPort.findByLogin(dto.login()).orElseThrow();
        var token = tokenService.generateToken(user);

        Map<String, String> response = new HashMap<>();
        response.put("token", token);
        response.put("type", "Bearer");

        return ResponseEntity.ok(response);
    }
}