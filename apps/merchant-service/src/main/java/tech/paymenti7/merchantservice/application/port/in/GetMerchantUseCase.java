package tech.paymenti7.merchantservice.application.port.in;

import java.util.UUID;

import tech.paymenti7.merchantservice.application.core.domain.Merchant;

public interface GetMerchantUseCase {

	Merchant getById(UUID merchantId);
}
