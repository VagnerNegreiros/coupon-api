package br.com.desafio.coupon.domain.exception;

import java.util.UUID;

public class CouponAlreadyDeletedException extends DomainException {

    public CouponAlreadyDeletedException(UUID couponId) {
        super("O cupom %s já foi deletado.".formatted(couponId));
    }
}
