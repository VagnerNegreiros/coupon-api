package br.com.desafio.coupon.application.exception;

import java.util.UUID;

public class CouponNotFoundException extends RuntimeException {

    public CouponNotFoundException(UUID couponId) {
        super("Cupom %s não encontrado.".formatted(couponId));
    }
}
