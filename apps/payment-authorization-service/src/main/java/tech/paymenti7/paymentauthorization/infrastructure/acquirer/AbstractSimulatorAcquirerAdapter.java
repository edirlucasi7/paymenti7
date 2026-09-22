package tech.paymenti7.paymentauthorization.infrastructure.acquirer;

import java.time.Duration;

import tech.paymenti7.paymentauthorization.application.domain.AuthorizationOutcome;
import tech.paymenti7.paymentauthorization.application.port.out.AcquirerAuthorizationPort;

abstract class AbstractSimulatorAcquirerAdapter implements AcquirerAuthorizationPort {

	private final String name;
	private final AuthorizationOutcome configuredOutcome;
	private final Duration delay;

	protected AbstractSimulatorAcquirerAdapter(String name, String configuredOutcome, Duration delay) {
		this.name = name;
		this.configuredOutcome = AuthorizationOutcome.valueOf(configuredOutcome);
		this.delay = delay;
		if (delay.isNegative()) {
			throw new IllegalArgumentException("Simulator delay cannot be negative");
		}
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public AcquirerResult authorize(AcquirerCommand command) {
		if (!delay.isZero()) {
			try {
				Thread.sleep(delay);
			}
			catch (InterruptedException exception) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException("Simulator call interrupted", exception);
			}
		}
		String providerReference = configuredOutcome.isTechnicalSuccess()
				? name.toLowerCase() + "-" + command.idempotencyReference()
				: null;
		return new AcquirerResult(configuredOutcome, providerReference, configuredOutcome.name());
	}
}
