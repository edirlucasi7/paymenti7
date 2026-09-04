package tech.paymenti7.paymentgatewaycore.application.core.domain;

import java.util.UUID;

public record MerchantDetails(UUID id, MerchantStatus status) {
}
