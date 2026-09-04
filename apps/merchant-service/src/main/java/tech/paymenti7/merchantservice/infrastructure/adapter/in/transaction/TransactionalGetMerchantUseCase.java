package tech.paymenti7.merchantservice.infrastructure.adapter.in.transaction;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tech.paymenti7.merchantservice.application.core.domain.Merchant;
import tech.paymenti7.merchantservice.application.core.service.MerchantQueryService;
import tech.paymenti7.merchantservice.application.port.in.GetMerchantUseCase;
import tech.paymenti7.merchantservice.application.port.out.MerchantPersistencePort;

@Service
public class TransactionalGetMerchantUseCase implements GetMerchantUseCase {

	private final MerchantQueryService merchantQueryService;

	public TransactionalGetMerchantUseCase(MerchantPersistencePort merchantPersistencePort) {
		this.merchantQueryService = new MerchantQueryService(merchantPersistencePort);
	}

	@Override
	@Transactional(readOnly = true)
	public Merchant getById(UUID merchantId) {
		return merchantQueryService.getById(merchantId);
	}
}
