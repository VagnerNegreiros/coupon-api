package br.com.desafio.coupon.application.dto;

import br.com.desafio.coupon.domain.coupon.Coupon;
import br.com.desafio.coupon.domain.coupon.CouponStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Visão somente-leitura de um cupom devolvida pelos use cases. Evita que os adapters recebam a
 * entidade de domínio e consigam disparar comportamentos (ex.: {@code delete}) fora de um use case.
 */
public record CouponOutput(
        UUID id,
        String code,
        String description,
        BigDecimal discountValue,
        Instant expirationDate,
        CouponStatus status,
        boolean published,
        boolean redeemed) {

    public static CouponOutput from(Coupon coupon) {
        return new CouponOutput(
                coupon.getId(),
                coupon.getCode(),
                coupon.getDescription(),
                coupon.getDiscountValue(),
                coupon.getExpirationDate(),
                coupon.getStatus(),
                coupon.isPublished(),
                coupon.isRedeemed());
    }
}
