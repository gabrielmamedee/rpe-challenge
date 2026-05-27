package com.rpe.orderservice.adapters.inbound.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rpe.orderservice.adapters.inbound.http.dto.OrderRequest;
import com.rpe.orderservice.adapters.inbound.http.dto.OrderResponse;
import com.rpe.orderservice.adapters.inbound.http.mapper.OrderMapper;
import com.rpe.orderservice.config.security.TokenService;
import com.rpe.orderservice.core.domain.Order;
import com.rpe.orderservice.core.domain.PaymentStatus;
import com.rpe.orderservice.core.domain.Role;
import com.rpe.orderservice.core.domain.User;
import com.rpe.orderservice.core.ports.inbound.CreateOrderUseCase;
import com.rpe.orderservice.core.ports.outbound.UserRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import com.rpe.orderservice.config.security.SecurityConfig;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@Import(SecurityConfig.class)
class OrderControllerTest {

    private static final String VALID_TOKEN = "Bearer test-jwt-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CreateOrderUseCase createOrderUseCase;

    @MockBean
    private OrderMapper orderMapper;

    @MockBean
    private StringRedisTemplate redisTemplate;

    @MockBean
    private ValueOperations<String, String> valueOperations;

    @MockBean
    private TokenService tokenService;

    @MockBean
    private UserRepositoryPort userRepositoryPort;

    private OrderRequest validRequestDto;
    private Order validOrder;
    private OrderResponse validResponseDto;

    @BeforeEach
    void setUp() {
        User authenticatedUser = new User("testuser", "password", Role.ROLE_ORDER_CREATOR);
        when(tokenService.validateToken("test-jwt-token")).thenReturn("testuser");
        when(userRepositoryPort.findByLogin("testuser")).thenReturn(Optional.of(authenticatedUser));

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        validRequestDto = new OrderRequest(
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                "PIX",
                "João Silva",
                "12345678901"
        );

        validOrder = new Order();
        validOrder.setId(UUID.randomUUID());

        validResponseDto = new OrderResponse(
                validOrder.getId(),
                PaymentStatus.PENDENTE_PAGAMENTO,
                null,
                null
        );
    }

    @Test
    @DisplayName("Deve retornar 201 Created quando for a primeira requisição (Idempotência Livre)")
    void shouldReturn201WhenRequestIsNew() throws Exception {
        // Arrange
        when(valueOperations.setIfAbsent(anyString(), anyString(), any())).thenReturn(true);
        when(orderMapper.toDomain(any())).thenReturn(validOrder);
        when(createOrderUseCase.execute(any())).thenReturn(validOrder);
        when(orderMapper.toResponseDto(any())).thenReturn(validResponseDto);

        // Act & Assert
        mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", VALID_TOKEN)
                        .header("Idempotency-Key", "chave-nova-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id_ordem").value(validResponseDto.id_ordem().toString()));

        verify(createOrderUseCase, times(1)).execute(any());
    }

    @Test
    @DisplayName("Deve retornar 409 Conflict quando a Idempotency-Key já existir no Redis")
    void shouldReturn409WhenRequestIsDuplicate() throws Exception {
        // Arrange
        when(valueOperations.setIfAbsent(anyString(), anyString(), any())).thenReturn(false);

        // Act & Assert
        mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", VALID_TOKEN)
                        .header("Idempotency-Key", "chave-repetida-456")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequestDto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.statusCode").value(409))
                .andExpect(jsonPath("$.message").value("Uma requisição com esta Idempotency-Key já foi processada."));

        verify(createOrderUseCase, never()).execute(any());
    }
}
