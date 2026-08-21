package tech.paymenti7.resilience;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class ResilienceLibraryTest {

	@Test
	void exposesTheSharedLibraryMarker() {
		assertNotNull(ResilienceLibrary.class);
	}
}
