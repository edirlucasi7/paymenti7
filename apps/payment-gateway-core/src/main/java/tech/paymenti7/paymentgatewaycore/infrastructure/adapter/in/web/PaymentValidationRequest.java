package tech.paymenti7.paymentgatewaycore.infrastructure.adapter.in.web;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record PaymentValidationRequest(@NotNull(message = "merchantId is required") UUID merchantId) {
}
