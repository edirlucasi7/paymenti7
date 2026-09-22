package tech.paymenti7.paymentauthorization.application.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class AuthorizationOutcomeTest {

	@ParameterizedTest
	@MethodSource("decisions")
	void decidesTheNextAuthorizationTransition(AuthorizationOutcome outcome, boolean hasNextRoute,
			AuthorizationDecision expectedDecision) {
		assertThat(outcome.decide(hasNextRoute)).isEqualTo(expectedDecision);
	}

	private static Stream<Arguments> decisions() {
		return Stream.of(
				Arguments.of(AuthorizationOutcome.APPROVED, false, AuthorizationDecision.COMPLETE_APPROVED),
				Arguments.of(AuthorizationOutcome.APPROVED, true, AuthorizationDecision.COMPLETE_APPROVED),
				Arguments.of(AuthorizationOutcome.DECLINED, false, AuthorizationDecision.COMPLETE_DECLINED),
				Arguments.of(AuthorizationOutcome.DECLINED, true, AuthorizationDecision.COMPLETE_DECLINED),
				Arguments.of(AuthorizationOutcome.UNKNOWN, false, AuthorizationDecision.AWAIT_RECONCILIATION),
				Arguments.of(AuthorizationOutcome.UNKNOWN, true, AuthorizationDecision.AWAIT_RECONCILIATION),
				Arguments.of(AuthorizationOutcome.SAFE_TO_FALLBACK, false, AuthorizationDecision.COMPLETE_FAILED),
				Arguments.of(AuthorizationOutcome.SAFE_TO_FALLBACK, true, AuthorizationDecision.CONTINUE_ROUTING));
	}
}
