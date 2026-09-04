package tech.paymenti7.paymentgatewaycore.infrastructure.adapter.in.web;

import java.util.UUID;

import tech.paymenti7.paymentgatewaycore.application.core.domain.MerchantStatus;

public record PaymentValidationResponse(UUID merchantId, MerchantStatus status) {
}
