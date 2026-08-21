package tech.paymenti7.merchantservice;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import tech.paymenti7.resilience.ResilienceLibrary;

class MerchantServiceApplicationTests {

	@Test
	void applicationAndResilienceLibraryAreAvailable() {
		assertNotNull(MerchantServiceApplication.class);
		assertNotNull(ResilienceLibrary.class);
	}
}
