package tech.paymenti7.paymentgatewaycore.application.port.in;

import java.math.BigDecimal;
import java.util.UUID;

public record SubmitPaymentCommand(UUID merchantId, BigDecimal amount, String currency, UUID idempotencyKey) {
}
