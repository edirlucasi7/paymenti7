package tech.paymenti7.merchantservice.application.port.out;

import tech.paymenti7.merchantservice.application.core.domain.OutboxEvent;

public interface OutboxEventPersistencePort {

	void save(OutboxEvent event);
}
