package com.rpe.orderservice.adapters.inbound.scheduler;

import com.rpe.orderservice.core.ports.inbound.ReprocessPendingOrdersUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderReprocessingScheduler {

    private final ReprocessPendingOrdersUseCase useCase;

    @Scheduled(fixedDelay = 60000)
    public void reprocess() {
        log.debug("Disparando rotina automática de reprocessamento de ordens...");
        useCase.execute();
    }
}