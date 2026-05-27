package com.rpe.orderservice.adapters.inbound.http;

import com.rpe.orderservice.adapters.inbound.http.dto.OrderRequest;
import com.rpe.orderservice.adapters.inbound.http.dto.OrderResponse;
import com.rpe.orderservice.adapters.inbound.http.dto.OrderSummaryResponse;
import com.rpe.orderservice.adapters.inbound.http.mapper.OrderMapper;
import com.rpe.orderservice.core.domain.Order;
import com.rpe.orderservice.core.domain.PaymentMethod;
import com.rpe.orderservice.core.domain.exceptions.DomainException;
import com.rpe.orderservice.core.ports.inbound.CreateOrderUseCase;
import com.rpe.orderservice.core.ports.inbound.FindOrdersByBuyerCpfUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final CreateOrderUseCase createOrderUseCase;
    private final OrderMapper orderMapper;
    private final StringRedisTemplate redisTemplate;
    private final FindOrdersByBuyerCpfUseCase findOrdersByBuyerCpfUseCase;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestHeader(value = "Idempotency-Key") String idempotencyKey, @Valid @RequestBody OrderRequest dto) {

        String redisKey = "idemp_order:" + idempotencyKey;

        Boolean isNewRequest = redisTemplate.opsForValue().setIfAbsent(redisKey, "PROCESSADO", Duration.ofMinutes(3));

        if (Boolean.FALSE.equals(isNewRequest)) {
            log.warn("Requisição duplicada interceptada! Idempotency-Key: {}", idempotencyKey);

            throw new DomainException("Uma requisição com esta Idempotency-Key já foi processada.");
        }

        try {
            Order order = orderMapper.toDomain(dto);
            Order savedOrder = createOrderUseCase.execute(order);

            return ResponseEntity.status(HttpStatus.CREATED).body(orderMapper.toResponseDto(savedOrder));

        } catch (Exception e) {
            redisTemplate.delete(redisKey);
            throw e;
        }
    }

    @GetMapping
    public ResponseEntity<List<OrderSummaryResponse>> getOrdersByCpf(
            @RequestParam(value = "cpf_comprador") String cpfComprador) {

        List<Order> orders = findOrdersByBuyerCpfUseCase.execute(cpfComprador);
        List<OrderSummaryResponse> summaryList = orderMapper.toSummaryResponseList(orders);

        return ResponseEntity.ok(summaryList);
    }
}