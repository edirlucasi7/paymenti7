package tech.paymenti7.merchantservice.application.port.in;

public interface ProcessOutboxEventsUseCase {

	void processPendingEvents(int batchSize);
}
