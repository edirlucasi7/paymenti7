package tech.paymenti7.paymentgatewaycore.infrastructure.adapter.in.web;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Intenção de pagamento aceita para processamento assíncrono")
public record PaymentRequest(
		@Schema(description = "Identificador do merchant", example = "11111111-1111-1111-1111-111111111111")
		@NotNull(message = "merchantId is required") UUID merchantId,
		@Schema(description = "Valor do pagamento", example = "125.90")
		@NotNull(message = "amount is required")
		@DecimalMin(value = "0.0001", message = "amount must be greater than zero")
		@Digits(integer = 15, fraction = 4, message = "amount must have up to 15 integer and 4 fraction digits") BigDecimal amount,
		@Schema(description = "Moeda ISO 4217 em letras maiúsculas", example = "BRL")
		@NotBlank(message = "currency is required")
		@Pattern(regexp = "[A-Z]{3}", message = "currency must contain three uppercase letters") String currency,
		@Schema(description = "Referência opaca do instrumento previamente tokenizado", example = "pmt_opaque_token")
		@NotBlank(message = "paymentMethodToken is required")
		@Size(max = 512, message = "paymentMethodToken must have at most 512 characters") String paymentMethodToken) {
}
