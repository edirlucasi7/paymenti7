package tech.paymenti7.paymentgatewaycore.infrastructure.adapter.in.web;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

import tech.paymenti7.paymentgatewaycore.application.core.domain.MerchantStatus;

@Schema(description = "Resultado da validação do merchant")
public record PaymentValidationResponse(
		@Schema(description = "Identificador do merchant validado", example = "11111111-1111-1111-1111-111111111111") UUID merchantId,
		@Schema(description = "Status atual do merchant", example = "ACTIVE", allowableValues = { "ACTIVE", "INACTIVE" }) MerchantStatus status) {
}
