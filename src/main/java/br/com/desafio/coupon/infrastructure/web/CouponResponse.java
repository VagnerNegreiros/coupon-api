package br.com.desafio.coupon.infrastructure.web;

import br.com.desafio.coupon.application.dto.CouponOutput;
import br.com.desafio.coupon.domain.coupon.CouponStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Contrato HTTP de resposta, conforme a documentação da API. */
public record CouponResponse(
        UUID id,
        String code,
        String description,
        BigDecimal discountValue,
        Instant expirationDate,
        CouponStatus status,
        boolean published,
        boolean redeemed) {

    static CouponResponse from(CouponOutput coupon) {
        return new CouponResponse(
                coupon.id(),
                coupon.code(),
                coupon.description(),
                coupon.discountValue(),
                coupon.expirationDate(),
                coupon.status(),
                coupon.published(),
                coupon.redeemed());
    }
}
