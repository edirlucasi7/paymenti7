package tech.paymenti7.merchantservice.application.core.service;

import java.util.UUID;

import tech.paymenti7.merchantservice.application.core.domain.Merchant;
import tech.paymenti7.merchantservice.application.port.out.MerchantPersistencePort;
import tech.paymenti7.merchantservice.application.shared.exception.MerchantNotFoundException;

public class MerchantQueryService {

	private final MerchantPersistencePort merchantPersistencePort;

	public MerchantQueryService(MerchantPersistencePort merchantPersistencePort) {
		this.merchantPersistencePort = merchantPersistencePort;
	}

	public Merchant getById(UUID merchantId) {
		return merchantPersistencePort.findById(merchantId)
				.orElseThrow(() -> new MerchantNotFoundException(merchantId));
	}
}
