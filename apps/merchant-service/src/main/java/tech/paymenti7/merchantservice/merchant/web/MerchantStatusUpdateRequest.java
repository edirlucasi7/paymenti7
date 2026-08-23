package tech.paymenti7.merchantservice.merchant.web;

import tech.paymenti7.merchantservice.merchant.domain.MerchantStatus;

public record MerchantStatusUpdateRequest(MerchantStatus status) {
}
