package tech.paymenti7.merchantservice.infrastructure.adapter.in.web;

import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;

import tech.paymenti7.merchantservice.application.core.domain.MerchantStatus;

@Schema(description = "Solicitação para alterar o status de um merchant")
public record MerchantStatusUpdateRequest(
		@Schema(description = "Novo status do merchant", example = "ACTIVE", allowableValues = { "ACTIVE", "INACTIVE" })
		@NotNull(message = "status is required") MerchantStatus status) {
}
