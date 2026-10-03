package br.com.desafio.coupon.domain.coupon;

import br.com.desafio.coupon.domain.exception.InvalidCouponException;

import java.math.BigDecimal;

/**
 * Value object do valor de desconto: mínimo de {@code 0.5}, sem máximo predeterminado.
 *
 * <p>Usa {@link BigDecimal} para evitar erros de arredondamento de ponto flutuante. A representação é
 * normalizada (0.80 e 0.8 são o mesmo desconto).
 */
public record DiscountValue(BigDecimal value) {

    public static final BigDecimal MINIMUM = new BigDecimal("0.5");

    public DiscountValue {
        if (value == null) {
            throw new InvalidCouponException("discountValue", "O campo 'discountValue' é obrigatório.");
        }
        if (value.compareTo(MINIMUM) < 0) {
            throw new InvalidCouponException("discountValue",
                    "O valor de desconto deve ser no mínimo %s. Recebido: %s."
                            .formatted(MINIMUM, value.toPlainString()));
        }
        value = normalize(value);
    }

    public static DiscountValue of(BigDecimal value) {
        return new DiscountValue(value);
    }

    private static BigDecimal normalize(BigDecimal value) {
        BigDecimal stripped = value.stripTrailingZeros();
        return stripped.scale() < 0 ? stripped.setScale(0) : stripped;
    }
}
