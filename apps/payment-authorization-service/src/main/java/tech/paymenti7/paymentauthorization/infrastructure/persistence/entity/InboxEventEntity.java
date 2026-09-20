package tech.paymenti7.paymentauthorization.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "inbox_events")
public class InboxEventEntity {

	@Id
	@Column(name = "event_id")
	private UUID eventId;

	@Column(name = "event_type", nullable = false, length = 100)
	private String eventType;

	@Column(name = "aggregate_id", nullable = false)
	private UUID aggregateId;

	@Column(name = "received_at", nullable = false)
	private Instant receivedAt;

	protected InboxEventEntity() {
	}
}
