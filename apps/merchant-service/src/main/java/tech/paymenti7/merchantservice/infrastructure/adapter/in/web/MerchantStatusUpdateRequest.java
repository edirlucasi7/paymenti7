package tech.paymenti7.merchantservice.infrastructure.adapter.in.web;

import jakarta.validation.constraints.NotNull;
import tech.paymenti7.merchantservice.application.core.domain.MerchantStatus;

public record MerchantStatusUpdateRequest(@NotNull(message = "status is required") MerchantStatus status) {
}
