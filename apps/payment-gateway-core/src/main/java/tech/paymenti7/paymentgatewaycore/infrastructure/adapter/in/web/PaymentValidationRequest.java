package tech.paymenti7.paymentgatewaycore.infrastructure.adapter.in.web;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Solicitação de validação do merchant antes do fluxo de pagamento")
public record PaymentValidationRequest(
		@Schema(description = "Identificador do merchant", example = "11111111-1111-1111-1111-111111111111")
		@NotNull(message = "merchantId is required") UUID merchantId) {
}
