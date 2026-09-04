package tech.paymenti7.merchantservice.application.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tech.paymenti7.merchantservice.application.core.domain.Merchant;
import tech.paymenti7.merchantservice.application.core.domain.MerchantStatus;
import tech.paymenti7.merchantservice.application.port.out.MerchantPersistencePort;
import tech.paymenti7.merchantservice.application.shared.exception.MerchantNotFoundException;

@ExtendWith(MockitoExtension.class)
class MerchantQueryServiceTest {

	@Mock
	private MerchantPersistencePort merchantPersistencePort;

	@Test
	void returnsMerchantFromItsOwnPersistencePort() {
		UUID merchantId = UUID.randomUUID();
		Merchant merchant = new Merchant(merchantId, MerchantStatus.ACTIVE, Instant.now(), Instant.now());
		given(merchantPersistencePort.findById(merchantId)).willReturn(Optional.of(merchant));

		Merchant result = new MerchantQueryService(merchantPersistencePort).getById(merchantId);

		assertThat(result).isSameAs(merchant);
	}

	@Test
	void throwsWhenMerchantDoesNotExist() {
		UUID merchantId = UUID.randomUUID();
		given(merchantPersistencePort.findById(merchantId)).willReturn(Optional.empty());

		assertThatThrownBy(() -> new MerchantQueryService(merchantPersistencePort).getById(merchantId))
				.isInstanceOf(MerchantNotFoundException.class);
	}
}
