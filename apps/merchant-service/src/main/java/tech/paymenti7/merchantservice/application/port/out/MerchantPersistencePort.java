package tech.paymenti7.merchantservice.application.port.out;

import java.util.Optional;
import java.util.UUID;

import tech.paymenti7.merchantservice.application.core.domain.Merchant;

public interface MerchantPersistencePort {

	Optional<Merchant> findById(UUID merchantId);

	Merchant save(Merchant merchant);
}
