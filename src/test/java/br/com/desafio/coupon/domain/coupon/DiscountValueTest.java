package br.com.desafio.coupon.domain.coupon;

import br.com.desafio.coupon.domain.exception.InvalidCouponException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DiscountValueTest {

    @ParameterizedTest
    @ValueSource(strings = {"0.5", "0.50", "0.51", "1", "100", "999999999999.99"})
    void acceptsValuesFromTheMinimumWithNoUpperLimit(String value) {
        assertThat(DiscountValue.of(new BigDecimal(value)).value()).isEqualByComparingTo(value);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.49", "0.4999999", "0", "-1", "-0.5"})
    void rejectsValuesBelowTheMinimum(String value) {
        assertThatThrownBy(() -> DiscountValue.of(new BigDecimal(value)))
                .isInstanceOf(InvalidCouponException.class)
                .hasMessageContaining("mínimo")
                .extracting("field").isEqualTo("discountValue");
    }

    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> DiscountValue.of(null)).isInstanceOf(InvalidCouponException.class);
    }

    @Test
    void normalizesRepresentation() {
        assertThat(DiscountValue.of(new BigDecimal("0.8000")).value()).isEqualTo(new BigDecimal("0.8"));
        assertThat(DiscountValue.of(new BigDecimal("10.00")).value()).isEqualTo(new BigDecimal("10"));
        assertThat(DiscountValue.of(new BigDecimal("0.80"))).isEqualTo(DiscountValue.of(new BigDecimal("0.8")));
    }
}
