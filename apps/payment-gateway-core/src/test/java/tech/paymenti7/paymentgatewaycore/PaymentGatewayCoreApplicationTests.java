package tech.paymenti7.paymentgatewaycore;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import tech.paymenti7.resilience.ResilienceLibrary;

class PaymentGatewayCoreApplicationTests {

	@Test
	void applicationAndResilienceLibraryAreAvailable() {
		assertNotNull(PaymentGatewayCoreApplication.class);
		assertNotNull(ResilienceLibrary.class);
	}
}
