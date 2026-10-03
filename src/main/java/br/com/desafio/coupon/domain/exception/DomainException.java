package br.com.desafio.coupon.domain.exception;

/** Base para violações de regras de negócio. Não conhece HTTP: o adapter web decide o status. */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}
