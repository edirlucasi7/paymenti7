package tech.paymenti7.paymentgatewaycore.application.port.in;

import java.util.UUID;

import tech.paymenti7.paymentgatewaycore.application.core.domain.PaymentStatus;

public record GetPaymentResult(UUID paymentId, PaymentStatus status) {
}
