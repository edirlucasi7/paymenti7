package tech.paymenti7.paymentauthorization.application.domain;

public enum AuthorizationOutcome {
	APPROVED,
	DECLINED,
	SAFE_TO_FALLBACK,
	UNKNOWN;

	public boolean isTechnicalSuccess() {
		return this == APPROVED || this == DECLINED;
	}
}
