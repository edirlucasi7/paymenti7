package tech.paymenti7.paymentauthorization.application.domain;

public enum AttemptStatus {
	DISPATCHING,
	APPROVED,
	DECLINED,
	SAFE_TO_FALLBACK,
	UNKNOWN
}
