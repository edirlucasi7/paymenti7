package tech.paymenti7.paymentauthorization.application.domain;

public enum AuthorizationDecision {
	COMPLETE_APPROVED,
	COMPLETE_DECLINED,
	CONTINUE_ROUTING,
	AWAIT_RECONCILIATION,
	COMPLETE_FAILED
}
