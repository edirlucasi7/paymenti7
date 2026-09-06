package tech.paymenti7.paymentgatewaycore.infrastructure.adapter.out.persistence.entity;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import tech.paymenti7.paymentgatewaycore.application.core.domain.PaymentStatus;

@Entity
@Table(name = "idempotency_requests")
public class IdempotencyRequestEntity {

	@Id
	private UUID id;

	@Column(name = "merchant_id", nullable = false)
	private UUID merchantId;

	@Column(nullable = false, length = 50)
	private String operation;

	@Column(name = "idempotency_key", nullable = false)
	private UUID idempotencyKey;

	@Column(name = "request_hash", nullable = false, length = 64)
	private String requestHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 16)
	private PaymentStatus status;

	@Column(name = "payment_id", nullable = false)
	private UUID paymentId;

	@Column(name = "response_http_status")
	private Integer responseHttpStatus;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "response_body", columnDefinition = "jsonb")
	private Map<String, Object> responseBody;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "response_headers", columnDefinition = "jsonb")
	private Map<String, String> responseHeaders;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "completed_at")
	private Instant completedAt;

	@Column(name = "expires_at")
	private Instant expiresAt;

	protected IdempotencyRequestEntity() {
	}

	public String getRequestHash() {
		return requestHash;
	}

	public PaymentStatus getStatus() {
		return status;
	}

	public UUID getPaymentId() {
		return paymentId;
	}

	public Integer getResponseHttpStatus() {
		return responseHttpStatus;
	}

	public Map<String, Object> getResponseBody() {
		return responseBody;
	}

	public Map<String, String> getResponseHeaders() {
		return responseHeaders;
	}

	public void complete(PaymentStatus terminalStatus, int httpStatus, Map<String, Object> body,
			Map<String, String> headers, Instant completedAt, Instant expiresAt) {
		if (terminalStatus == PaymentStatus.PROCESSING) {
			throw new IllegalArgumentException("A terminal payment status is required");
		}
		status = terminalStatus;
		responseHttpStatus = httpStatus;
		responseBody = Map.copyOf(body);
		responseHeaders = Map.copyOf(headers);
		this.completedAt = completedAt;
		this.expiresAt = expiresAt;
	}
}
