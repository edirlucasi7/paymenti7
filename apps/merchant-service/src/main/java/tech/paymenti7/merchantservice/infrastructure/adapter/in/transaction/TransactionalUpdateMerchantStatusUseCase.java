package tech.paymenti7.merchantservice.infrastructure.adapter.in.transaction;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tech.paymenti7.merchantservice.application.core.service.MerchantStatusUpdateService;
import tech.paymenti7.merchantservice.application.port.in.UpdateMerchantStatusCommand;
import tech.paymenti7.merchantservice.application.port.in.UpdateMerchantStatusUseCase;
import tech.paymenti7.merchantservice.application.port.out.MerchantPersistencePort;
import tech.paymenti7.merchantservice.application.port.out.OutboxEventPersistencePort;

@Service
public class TransactionalUpdateMerchantStatusUseCase implements UpdateMerchantStatusUseCase {

	private final MerchantStatusUpdateService merchantStatusUpdateService;

	public TransactionalUpdateMerchantStatusUseCase(MerchantPersistencePort merchantPersistencePort,
			OutboxEventPersistencePort outboxEventPersistencePort) {
		this.merchantStatusUpdateService = new MerchantStatusUpdateService(merchantPersistencePort, outboxEventPersistencePort);
	}

	@Override
	@Transactional
	public void updateStatus(UpdateMerchantStatusCommand command) {
		merchantStatusUpdateService.updateStatus(command);
	}
}
