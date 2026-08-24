package tech.paymenti7.merchantservice.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import tech.paymenti7.merchantservice.application.core.domain.OutboxEvent;

public interface OutboxEventPersistencePort {

	void save(OutboxEvent event);

	List<OutboxEvent> lockLatestPendingEvents(int batchSize);

	void suppressOlderPendingEvents(UUID aggregateId, UUID latestEventId, Instant processedAt);

	void markPublished(UUID eventId, Instant processedAt);
}
