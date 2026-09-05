package tech.paymenti7.merchantservice.application.core.domain;

import java.time.Instant;
import java.util.UUID;

public class Merchant {

	private final UUID id;
	private MerchantStatus status;
	private final Instant createdAt;
	private Instant updatedAt;
	private final long revision;

	public Merchant(UUID id, MerchantStatus status, Instant createdAt, Instant updatedAt) {
		this(id, status, createdAt, updatedAt, 0);
	}

	public Merchant(UUID id, MerchantStatus status, Instant createdAt, Instant updatedAt, long revision) {
		this.id = id;
		this.status = status;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
		this.revision = revision;
	}

	public UUID id() {
		return id;
	}

	public MerchantStatus status() {
		return status;
	}

	public Instant createdAt() {
		return createdAt;
	}

	public Instant updatedAt() {
		return updatedAt;
	}

	public long revision() {
		return revision;
	}

	public boolean hasStatus(MerchantStatus status) {
		return this.status == status;
	}

	public void updateStatus(MerchantStatus status) {
		this.status = status;
		this.updatedAt = Instant.now();
	}
}
