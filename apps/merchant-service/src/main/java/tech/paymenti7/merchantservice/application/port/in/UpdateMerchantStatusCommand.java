package tech.paymenti7.merchantservice.application.port.in;

import java.util.UUID;

import tech.paymenti7.merchantservice.application.core.domain.MerchantStatus;

public record UpdateMerchantStatusCommand(UUID merchantId, MerchantStatus status) {
}
