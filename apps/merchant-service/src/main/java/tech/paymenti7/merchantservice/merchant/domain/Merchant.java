package tech.paymenti7.merchantservice.merchant.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "merchants")
public class Merchant {

	@Id
	private UUID id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private MerchantStatus status;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Merchant() {
	}

	public boolean hasStatus(MerchantStatus status) {
		return this.status == status;
	}

	public void updateStatus(MerchantStatus status) {
		this.status = status;
		this.updatedAt = Instant.now();
	}
}
