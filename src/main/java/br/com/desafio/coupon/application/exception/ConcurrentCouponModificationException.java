package br.com.desafio.coupon.application.exception;

import java.util.UUID;

/**
 * O cupom foi alterado por outra operação entre a leitura e a gravação (ex.: dois DELETEs simultâneos).
 * Lançada pelo adapter de persistência, que traduz o erro técnico de lock otimista para este contrato.
 */
public class ConcurrentCouponModificationException extends RuntimeException {

    public ConcurrentCouponModificationException(UUID couponId, Throwable cause) {
        super("O cupom %s foi modificado por outra operação. Consulte o estado atual e tente novamente."
                .formatted(couponId), cause);
    }
}
