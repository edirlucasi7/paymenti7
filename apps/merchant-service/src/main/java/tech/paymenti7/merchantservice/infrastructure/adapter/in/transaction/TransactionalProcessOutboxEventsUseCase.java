package tech.paymenti7.merchantservice.infrastructure.adapter.in.transaction;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tech.paymenti7.merchantservice.application.core.service.OutboxEventProcessingService;
import tech.paymenti7.merchantservice.application.port.in.ProcessOutboxEventsUseCase;
import tech.paymenti7.merchantservice.application.port.out.OutboxEventPersistencePort;
import tech.paymenti7.merchantservice.application.port.out.OutboxEventPublisherPort;

@Service
public class TransactionalProcessOutboxEventsUseCase implements ProcessOutboxEventsUseCase {

	private final OutboxEventProcessingService outboxEventProcessingService;

	public TransactionalProcessOutboxEventsUseCase(OutboxEventPersistencePort outboxEventPersistencePort,
			OutboxEventPublisherPort outboxEventPublisherPort) {
		this.outboxEventProcessingService = new OutboxEventProcessingService(outboxEventPersistencePort, outboxEventPublisherPort);
	}

	@Override
	@Transactional
	public void processPendingEvents(int batchSize) {
		outboxEventProcessingService.processPendingEvents(batchSize);
	}
}
