package tech.paymenti7.paymentauthorization.application.domain;

public enum AuthorizationStatus {
	PENDING_ROUTING,
	CALL_IN_PROGRESS,
	PENDING_RECONCILIATION,
	COMPLETED
}
