package tech.paymenti7.merchantservice.merchant.domain;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantRepository extends JpaRepository<Merchant, UUID> {
}
