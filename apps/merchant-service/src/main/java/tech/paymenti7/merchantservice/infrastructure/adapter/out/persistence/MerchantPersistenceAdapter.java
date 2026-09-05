package tech.paymenti7.merchantservice.infrastructure.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import tech.paymenti7.merchantservice.application.core.domain.Merchant;
import tech.paymenti7.merchantservice.application.port.out.MerchantPersistencePort;
import tech.paymenti7.merchantservice.infrastructure.adapter.out.persistence.entity.MerchantEntity;
import tech.paymenti7.merchantservice.infrastructure.adapter.out.persistence.repository.MerchantJpaRepository;

@Component
public class MerchantPersistenceAdapter implements MerchantPersistencePort {

	private final MerchantJpaRepository merchantJpaRepository;

	public MerchantPersistenceAdapter(MerchantJpaRepository merchantJpaRepository) {
		this.merchantJpaRepository = merchantJpaRepository;
	}

	@Override
	public Optional<Merchant> findById(UUID merchantId) {
		return merchantJpaRepository.findById(merchantId).map(MerchantEntity::toDomain);
	}

	@Override
	public Merchant save(Merchant merchant) {
		return merchantJpaRepository.saveAndFlush(MerchantEntity.fromDomain(merchant)).toDomain();
	}
}
