package tech.paymenti7.merchantservice.infrastructure.adapter.out.persistence.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import tech.paymenti7.merchantservice.application.core.domain.Merchant;
import tech.paymenti7.merchantservice.application.core.domain.MerchantStatus;

@Entity
@Table(name = "merchants")
public class MerchantEntity {

	@Id
	private UUID id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private MerchantStatus status;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Column(nullable = false)
	private Long revision;

	protected MerchantEntity() {
	}

	public static MerchantEntity fromDomain(Merchant merchant) {
		MerchantEntity entity = new MerchantEntity();
		entity.id = merchant.id();
		entity.status = merchant.status();
		entity.createdAt = merchant.createdAt();
		entity.updatedAt = merchant.updatedAt();
		entity.revision = merchant.revision();
		return entity;
	}

	public Merchant toDomain() {
		return new Merchant(id, status, createdAt, updatedAt, revision);
	}
}
