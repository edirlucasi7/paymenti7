package tech.paymenti7.resilience;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;

class AcquirerCircuitBreakerTest {

	@Test
	void opensOnlyAfterTheMinimumSampleReachesFiftyPercentFailures() {
		var breaker = new AcquirerCircuitBreaker("acquirer", AcquirerCircuitBreaker.Configuration.defaults());

		for (int index = 0; index < 99; index++) {
			var permit = breaker.tryAcquire(UUID.randomUUID());
			breaker.record(permit, index >= 50);
		}
		assertEquals(CircuitBreaker.State.CLOSED, breaker.state());

		var last = breaker.tryAcquire(UUID.randomUUID());
		breaker.record(last, true);
		assertEquals(CircuitBreaker.State.OPEN, breaker.state());
	}

	@Test
	void businessDeclinesCanBeRecordedAsTechnicalSuccess() {
		var breaker = new AcquirerCircuitBreaker("declines", AcquirerCircuitBreaker.Configuration.defaults());

		for (int index = 0; index < 100; index++) {
			var permit = breaker.tryAcquire(UUID.randomUUID());
			breaker.record(permit, true);
		}

		assertEquals(CircuitBreaker.State.CLOSED, breaker.state());
	}

	@Test
	void halfOpenDeterministicallySelectsTenPercentOfPayments() {
		var breaker = new AcquirerCircuitBreaker("sampling", AcquirerCircuitBreaker.Configuration.defaults());
		breaker.delegate().transitionToOpenState();
		breaker.delegate().transitionToHalfOpenState();
		int selected = 0;

		for (long value = 0; value < 100; value++) {
			var permit = breaker.tryAcquire(new UUID(0, value));
			if (permit.allowed()) {
				selected++;
				breaker.record(permit, true);
			}
		}

		assertEquals(10, selected);
		assertEquals(60_000L, breaker.delegate().getCircuitBreakerConfig()
				.getWaitIntervalFunctionInOpenState().apply(1));
	}

	@Test
	void halfOpenLimitsConcurrencyAndClosesOnlyWithNinetyFiveSuccessfulProbes() {
		var clock = new MutableClock();
		var breaker = new AcquirerCircuitBreaker("recovery", AcquirerCircuitBreaker.Configuration.defaults(), clock);
		breaker.delegate().transitionToOpenState();
		breaker.delegate().transitionToHalfOpenState();

		var inFlight = new ArrayList<AcquirerCircuitBreaker.Permit>();
		for (long value = 0; inFlight.size() < 20; value++) {
			var permit = breaker.tryAcquire(new UUID(0, value));
			if (permit.allowed()) {
				inFlight.add(permit);
			}
		}
		assertEquals(20, breaker.inFlightProbes());
		assertFalse(breaker.tryAcquire(selectedPaymentId(10_000)).allowed());
		inFlight.forEach(permit -> breaker.record(permit, true));

		int completed = 20;
		long candidate = 20_000;
		while (completed < 100) {
			var permit = breaker.tryAcquire(new UUID(0, candidate++));
			if (permit.allowed()) {
				breaker.record(permit, completed < 95);
				completed++;
			}
		}
		assertEquals(CircuitBreaker.State.HALF_OPEN, breaker.state());
		clock.advance(Duration.ofSeconds(10));
		breaker.evaluateRecoveryWindow();
		assertEquals(CircuitBreaker.State.CLOSED, breaker.state());
	}

	@Test
	void incompleteHalfOpenWindowReopensAfterTwoMinutes() {
		var clock = new MutableClock();
		var breaker = new AcquirerCircuitBreaker("incomplete", AcquirerCircuitBreaker.Configuration.defaults(), clock);
		breaker.delegate().transitionToOpenState();
		breaker.delegate().transitionToHalfOpenState();

		clock.advance(Duration.ofMinutes(2));
		breaker.evaluateRecoveryWindow();

		assertEquals(CircuitBreaker.State.OPEN, breaker.state());
	}

	@Test
	void ninetyFourPercentRecoveryReopensTheCircuit() {
		var clock = new MutableClock();
		var breaker = new AcquirerCircuitBreaker("failed-recovery",
				AcquirerCircuitBreaker.Configuration.defaults(), clock);
		breaker.delegate().transitionToOpenState();
		breaker.delegate().transitionToHalfOpenState();

		int completed = 0;
		long candidate = 0;
		while (completed < 100) {
			var permit = breaker.tryAcquire(new UUID(0, candidate++));
			if (permit.allowed()) {
				breaker.record(permit, completed < 94);
				completed++;
			}
		}
		clock.advance(Duration.ofSeconds(10));
		breaker.evaluateRecoveryWindow();

		assertEquals(CircuitBreaker.State.OPEN, breaker.state());
	}

	private UUID selectedPaymentId(long start) {
		for (long value = start; ; value++) {
			UUID paymentId = new UUID(0, value);
			if (Math.floorMod(paymentId.hashCode(), 100) < 10) {
				return paymentId;
			}
		}
	}

	private static final class MutableClock extends Clock {
		private Instant instant = Instant.parse("2026-09-19T00:00:00Z");

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return instant;
		}

		void advance(Duration duration) {
			instant = instant.plus(duration);
		}
	}
}
