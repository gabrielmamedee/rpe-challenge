package com.rpe.orderservice.core.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaymentStatusTest {

    @Test
    @DisplayName("Deve converter Strings com variações para o Enum correto de forma segura")
    void shouldConvertStringsToEnumSafely() {
        // Testando conversões felizes com sujeiras
        assertEquals(PaymentStatus.PENDENTE_PAGAMENTO, PaymentStatus.fromString("PENDENTE PAGAMENTO"));
        assertEquals(PaymentStatus.PENDENTE_PAGAMENTO, PaymentStatus.fromString("pendente-pagamento"));
        assertEquals(PaymentStatus.PAGO, PaymentStatus.fromString("  PaGo  "));
        assertEquals(PaymentStatus.RECUSADO, PaymentStatus.fromString("recusado"));

        // Testando fallbacks (Cenários de erro onde não queremos que a aplicação exploda)
        assertEquals(PaymentStatus.PENDENTE_PAGAMENTO, PaymentStatus.fromString(null));
        assertEquals(PaymentStatus.PENDENTE_PAGAMENTO, PaymentStatus.fromString(""));
        assertEquals(PaymentStatus.PENDENTE_PAGAMENTO, PaymentStatus.fromString("UM_STATUS_LOUCO_DO_GO"));
    }
}