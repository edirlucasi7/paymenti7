package tech.paymenti7.paymentgatewaycore.infrastructure.adapter.in.web;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

import tech.paymenti7.paymentgatewaycore.application.core.domain.PaymentStatus;

@Schema(description = "Estado da intenção de pagamento")
public record PaymentResponse(
		@Schema(description = "Identificador permanente do pagamento") UUID paymentId,
		@Schema(description = "Estado atual do processamento") PaymentStatus status) {
}
