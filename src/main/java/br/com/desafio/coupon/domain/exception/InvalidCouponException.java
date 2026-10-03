package br.com.desafio.coupon.domain.exception;

/** Dados de cupom que violam alguma regra de cadastro. Carrega o campo responsável pelo erro. */
public class InvalidCouponException extends DomainException {

    private final String field;

    public InvalidCouponException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
