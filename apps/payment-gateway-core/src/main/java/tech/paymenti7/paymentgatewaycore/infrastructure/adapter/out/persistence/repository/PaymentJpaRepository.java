package tech.paymenti7.paymentgatewaycore.infrastructure.adapter.out.persistence.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import tech.paymenti7.paymentgatewaycore.infrastructure.adapter.out.persistence.entity.PaymentEntity;

public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, UUID> {
}
