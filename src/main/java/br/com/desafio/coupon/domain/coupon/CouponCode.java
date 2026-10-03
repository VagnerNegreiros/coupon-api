package br.com.desafio.coupon.domain.coupon;

import br.com.desafio.coupon.domain.exception.InvalidCouponException;

import java.util.regex.Pattern;

/**
 * Value object do código do cupom: sempre alfanumérico e com exatamente {@value #LENGTH} caracteres.
 *
 * <p>A sanitização (remoção de caracteres especiais) fica encapsulada aqui: quem cria o cupom não
 * precisa saber como o código é tratado, e é impossível existir um {@code CouponCode} inválido.
 */
public record CouponCode(String value) {

    public static final int LENGTH = 6;

    private static final Pattern VALID = Pattern.compile("[A-Za-z0-9]{" + LENGTH + "}");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^A-Za-z0-9]");

    public CouponCode {
        if (value == null || !VALID.matcher(value).matches()) {
            throw invalid(value);
        }
    }

    /** Recebe o código como digitado, remove caracteres especiais e valida o tamanho final. */
    public static CouponCode of(String rawCode) {
        if (rawCode == null || rawCode.isBlank()) {
            throw new InvalidCouponException("code", "O campo 'code' é obrigatório.");
        }
        String sanitized = NON_ALPHANUMERIC.matcher(rawCode).replaceAll("");
        if (sanitized.length() != LENGTH) {
            throw invalid(rawCode);
        }
        return new CouponCode(sanitized);
    }

    private static InvalidCouponException invalid(String received) {
        return new InvalidCouponException("code",
                "O código deve ter exatamente %d caracteres alfanuméricos após remover caracteres especiais. Recebido: '%s'."
                        .formatted(LENGTH, received));
    }
}
