package tech.paymenti7.merchantservice.infrastructure.adapter.in.scheduler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import tech.paymenti7.merchantservice.application.port.in.ProcessOutboxEventsUseCase;

@Component
public class OutboxEventScheduler {

	private final ProcessOutboxEventsUseCase processOutboxEventsUseCase;
	private final int batchSize;

	public OutboxEventScheduler(ProcessOutboxEventsUseCase processOutboxEventsUseCase,
			@Value("${merchant.outbox.batch-size:20}") int batchSize) {
		this.processOutboxEventsUseCase = processOutboxEventsUseCase;
		this.batchSize = batchSize;
	}

	@Scheduled(fixedDelayString = "${merchant.outbox.polling-delay:5s}")
	public void poll() {
		processOutboxEventsUseCase.processPendingEvents(batchSize);
	}
}
