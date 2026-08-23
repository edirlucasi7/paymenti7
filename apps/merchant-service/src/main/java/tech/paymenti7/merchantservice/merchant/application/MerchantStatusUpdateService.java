package tech.paymenti7.merchantservice.merchant.application;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tech.paymenti7.merchantservice.merchant.domain.MerchantRepository;
import tech.paymenti7.merchantservice.merchant.domain.MerchantStatus;
import tech.paymenti7.merchantservice.merchant.domain.OutboxEvent;
import tech.paymenti7.merchantservice.merchant.domain.OutboxEventRepository;

@Service
public class MerchantStatusUpdateService {

	private final MerchantRepository merchantRepository;
	private final OutboxEventRepository outboxEventRepository;

	public MerchantStatusUpdateService(MerchantRepository merchantRepository, OutboxEventRepository outboxEventRepository) {
		this.merchantRepository = merchantRepository;
		this.outboxEventRepository = outboxEventRepository;
	}

	@Transactional
	public void updateStatus(UUID merchantId, MerchantStatus status) {
		if (status == null) {
			throw new InvalidMerchantStatusException();
		}

		var merchant = merchantRepository.findById(merchantId)
				.orElseThrow(() -> new MerchantNotFoundException(merchantId));

		if (merchant.hasStatus(status)) {
			return;
		}

		merchant.updateStatus(status);
		outboxEventRepository.save(OutboxEvent.merchantUpdated(merchantId, status));
	}
}
