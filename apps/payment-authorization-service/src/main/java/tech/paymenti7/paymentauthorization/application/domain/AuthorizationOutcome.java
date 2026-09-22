package tech.paymenti7.paymentauthorization.application.domain;

public enum AuthorizationOutcome {
	APPROVED,
	DECLINED,
	SAFE_TO_FALLBACK,
	UNKNOWN;

	public AuthorizationDecision decide(boolean hasNextRoute) {
		return switch (this) {
			case APPROVED -> AuthorizationDecision.COMPLETE_APPROVED;
			case DECLINED -> AuthorizationDecision.COMPLETE_DECLINED;
			case SAFE_TO_FALLBACK -> hasNextRoute
					? AuthorizationDecision.CONTINUE_ROUTING
					: AuthorizationDecision.COMPLETE_FAILED;
			case UNKNOWN -> AuthorizationDecision.AWAIT_RECONCILIATION;
		};
	}

	public boolean isTechnicalSuccess() {
		return this == APPROVED || this == DECLINED;
	}
}
