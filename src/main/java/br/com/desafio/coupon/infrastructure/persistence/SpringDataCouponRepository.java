package br.com.desafio.coupon.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataCouponRepository extends JpaRepository<CouponEntity, UUID> {
}
