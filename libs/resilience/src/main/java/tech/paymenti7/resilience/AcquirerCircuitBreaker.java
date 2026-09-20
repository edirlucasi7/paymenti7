package tech.paymenti7.resilience;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

/**
 * Circuit breaker técnico com uma janela de recuperação mais rigorosa que a
 * janela usada para abrir o circuito.
 */
public final class AcquirerCircuitBreaker {

	public enum PermitType {
		NORMAL,
		PROBE,
		REJECTED
	}

	public record Permit(PermitType type, Instant startedAt) {
		public boolean allowed() {
			return type != PermitType.REJECTED;
		}
	}

	private final CircuitBreaker delegate;
	private final Clock clock;
	private final Semaphore probeConcurrency;
	private final int probeSamplePercent;
	private final int requiredProbes;
	private final double recoverySuccessRate;
	private final Duration minimumRecoveryObservation;
	private final Duration maximumRecoveryObservation;
	private final AtomicInteger reservedProbes = new AtomicInteger();
	private final AtomicInteger completedProbes = new AtomicInteger();
	private final AtomicInteger successfulProbes = new AtomicInteger();
	private volatile Instant halfOpenSince;

	public AcquirerCircuitBreaker(String name, Configuration configuration) {
		this(name, configuration, Clock.systemUTC());
	}

	AcquirerCircuitBreaker(String name, Configuration configuration, Clock clock) {
		configuration.validate();
		this.clock = clock;
		this.probeConcurrency = new Semaphore(configuration.maximumConcurrentProbes());
		this.probeSamplePercent = configuration.probeSamplePercent();
		this.requiredProbes = configuration.requiredCompletedProbes();
		this.recoverySuccessRate = configuration.recoverySuccessRate();
		this.minimumRecoveryObservation = configuration.minimumRecoveryObservation();
		this.maximumRecoveryObservation = configuration.maximumRecoveryObservation();

		var delegateConfig = CircuitBreakerConfig.custom()
				.slidingWindowType(CircuitBreakerConfig.SlidingWindowType.TIME_BASED)
				.slidingWindowSize(Math.toIntExact(configuration.closedWindow().toSeconds()))
				.minimumNumberOfCalls(configuration.minimumCalls())
				.failureRateThreshold((float) configuration.openFailureRate())
				.waitDurationInOpenState(configuration.openDuration())
				.automaticTransitionFromOpenToHalfOpenEnabled(true)
				.permittedNumberOfCallsInHalfOpenState(configuration.requiredCompletedProbes())
				.maxWaitDurationInHalfOpenState(Duration.ZERO)
				.build();
		this.delegate = CircuitBreakerRegistry.of(delegateConfig).circuitBreaker(name);
		this.delegate.getEventPublisher().onStateTransition(event -> {
			if (event.getStateTransition().getToState() == CircuitBreaker.State.HALF_OPEN) {
				resetRecoveryWindow();
			}
		});
	}

	public Permit tryAcquire(UUID paymentId) {
		CircuitBreaker.State state = delegate.getState();
		if (state == CircuitBreaker.State.CLOSED) {
			return new Permit(PermitType.NORMAL, clock.instant());
		}
		if (state != CircuitBreaker.State.HALF_OPEN) {
			return new Permit(PermitType.REJECTED, clock.instant());
		}

		evaluateRecoveryWindow();
		if (delegate.getState() != CircuitBreaker.State.HALF_OPEN || !selectedForProbe(paymentId)) {
			return new Permit(PermitType.REJECTED, clock.instant());
		}
		if (!probeConcurrency.tryAcquire()) {
			return new Permit(PermitType.REJECTED, clock.instant());
		}
		if (!reserveProbe()) {
			probeConcurrency.release();
			return new Permit(PermitType.REJECTED, clock.instant());
		}
		return new Permit(PermitType.PROBE, clock.instant());
	}

	public void record(Permit permit, boolean technicalSuccess) {
		long durationNanos = Math.max(0, Duration.between(permit.startedAt(), clock.instant()).toNanos());
		if (permit.type() == PermitType.NORMAL) {
			if (technicalSuccess) {
				delegate.onSuccess(durationNanos, TimeUnit.NANOSECONDS);
			}
			else {
				delegate.onError(durationNanos, TimeUnit.NANOSECONDS,
						new TechnicalCallFailureException());
			}
			return;
		}
		if (permit.type() == PermitType.PROBE) {
			if (delegate.getState() != CircuitBreaker.State.HALF_OPEN) {
				probeConcurrency.release();
				return;
			}
			if (technicalSuccess) {
				successfulProbes.incrementAndGet();
			}
			completedProbes.incrementAndGet();
			probeConcurrency.release();
			evaluateRecoveryWindow();
		}
	}

	public void cancel(Permit permit) {
		if (permit.type() == PermitType.PROBE) {
			reservedProbes.decrementAndGet();
			probeConcurrency.release();
		}
	}

	public void evaluateRecoveryWindow() {
		if (delegate.getState() != CircuitBreaker.State.HALF_OPEN) {
			return;
		}
		Instant startedAt = halfOpenSince;
		if (startedAt == null) {
			resetRecoveryWindow();
			startedAt = halfOpenSince;
		}
		Duration elapsed = Duration.between(startedAt, clock.instant());
		int completed = completedProbes.get();
		if (completed >= requiredProbes && elapsed.compareTo(minimumRecoveryObservation) >= 0) {
			double successRate = successfulProbes.get() * 100.0 / completed;
			if (successRate >= recoverySuccessRate) {
				delegate.transitionToClosedState();
			}
			else {
				delegate.transitionToOpenState();
			}
		}
		else if (elapsed.compareTo(maximumRecoveryObservation) >= 0) {
			delegate.transitionToOpenState();
		}
	}

	public CircuitBreaker.State state() {
		return delegate.getState();
	}

	public CircuitBreaker delegate() {
		return delegate;
	}

	public int completedProbes() {
		return completedProbes.get();
	}

	public int inFlightProbes() {
		return delegate.getState() == CircuitBreaker.State.HALF_OPEN
				? reservedProbes.get() - completedProbes.get()
				: 0;
	}

	private boolean selectedForProbe(UUID paymentId) {
		return Math.floorMod(paymentId.hashCode(), 100) < probeSamplePercent;
	}

	private boolean reserveProbe() {
		while (true) {
			int current = reservedProbes.get();
			if (current >= requiredProbes) {
				return false;
			}
			if (reservedProbes.compareAndSet(current, current + 1)) {
				return true;
			}
		}
	}

	private void resetRecoveryWindow() {
		reservedProbes.set(0);
		completedProbes.set(0);
		successfulProbes.set(0);
		halfOpenSince = clock.instant();
	}

	public record Configuration(Duration closedWindow, int minimumCalls, double openFailureRate,
			Duration openDuration, int probeSamplePercent, int maximumConcurrentProbes,
			int requiredCompletedProbes, double recoverySuccessRate, Duration minimumRecoveryObservation,
			Duration maximumRecoveryObservation) {

		public static Configuration defaults() {
			return new Configuration(Duration.ofSeconds(30), 100, 50, Duration.ofSeconds(60),
					10, 20, 100, 95, Duration.ofSeconds(10), Duration.ofMinutes(2));
		}

		private void validate() {
			if (closedWindow.isZero() || closedWindow.isNegative() || minimumCalls < 1
					|| openFailureRate <= 0 || openFailureRate > 100 || openDuration.isNegative()
					|| probeSamplePercent < 1 || probeSamplePercent > 100 || maximumConcurrentProbes < 1
					|| requiredCompletedProbes < 1 || recoverySuccessRate <= 0 || recoverySuccessRate > 100
					|| minimumRecoveryObservation.isNegative()
					|| maximumRecoveryObservation.compareTo(minimumRecoveryObservation) < 0) {
				throw new IllegalArgumentException("Invalid circuit breaker configuration");
			}
		}
	}

	private static final class TechnicalCallFailureException extends RuntimeException {
		private TechnicalCallFailureException() {
			super("Technical acquiring call failure", null, false, false);
		}
	}
}
