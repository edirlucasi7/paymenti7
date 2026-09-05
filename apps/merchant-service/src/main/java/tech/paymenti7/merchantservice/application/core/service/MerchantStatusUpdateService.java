package tech.paymenti7.merchantservice.application.core.service;

import tech.paymenti7.merchantservice.application.core.domain.OutboxEvent;
import tech.paymenti7.merchantservice.application.port.in.UpdateMerchantStatusCommand;
import tech.paymenti7.merchantservice.application.port.out.MerchantPersistencePort;
import tech.paymenti7.merchantservice.application.port.out.OutboxEventPersistencePort;
import tech.paymenti7.merchantservice.application.shared.exception.InvalidMerchantStatusException;
import tech.paymenti7.merchantservice.application.shared.exception.MerchantNotFoundException;

public class MerchantStatusUpdateService {

	private final MerchantPersistencePort merchantPersistencePort;
	private final OutboxEventPersistencePort outboxEventPersistencePort;

	public MerchantStatusUpdateService(MerchantPersistencePort merchantPersistencePort,
			OutboxEventPersistencePort outboxEventPersistencePort) {
		this.merchantPersistencePort = merchantPersistencePort;
		this.outboxEventPersistencePort = outboxEventPersistencePort;
	}

	public void updateStatus(UpdateMerchantStatusCommand command) {
		if (command.status() == null) {
			throw new InvalidMerchantStatusException();
		}

		var merchant = merchantPersistencePort.findById(command.merchantId())
				.orElseThrow(() -> new MerchantNotFoundException(command.merchantId()));

		if (merchant.hasStatus(command.status())) {
			return;
		}

		merchant.updateStatus(command.status());
		var updatedMerchant = merchantPersistencePort.save(merchant);
		outboxEventPersistencePort.save(
				OutboxEvent.merchantUpdated(updatedMerchant.id(), updatedMerchant.status(), updatedMerchant.revision()));
	}
}
