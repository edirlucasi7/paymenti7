package tech.paymenti7.merchantservice.infrastructure.adapter.out.persistence.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import tech.paymenti7.merchantservice.infrastructure.adapter.out.persistence.entity.MerchantEntity;

public interface MerchantJpaRepository extends JpaRepository<MerchantEntity, UUID> {
}
