package br.com.desafio.coupon.domain.coupon;

import br.com.desafio.coupon.domain.exception.InvalidCouponException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CouponCodeTest {

    @ParameterizedTest(name = "\"{0}\" -> \"{1}\"")
    @CsvSource(delimiter = '|', value = {
            "ABC123     | ABC123",
            "ABC-123    | ABC123",
            "A.B.C-1/2#3| ABC123",
            "' ab c12 3'| abc123",
            "@@x1y2z3!! | x1y2z3",
            "Çá-ABC123  | ABC123",
    })
    void removesSpecialCharactersKeepingSixAlphanumerics(String raw, String expected) {
        assertThat(CouponCode.of(raw).value()).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ABC12", "AB-12", "ABC1234", "ABCD-1234", "!!!!!!", "------"})
    void rejectsCodesThatDoNotEndUpWithExactlySixCharacters(String raw) {
        assertThatThrownBy(() -> CouponCode.of(raw))
                .isInstanceOf(InvalidCouponException.class)
                .hasMessageContaining("6 caracteres")
                .extracting("field").isEqualTo("code");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void rejectsMissingCode(String raw) {
        assertThatThrownBy(() -> CouponCode.of(raw))
                .isInstanceOf(InvalidCouponException.class)
                .hasMessageContaining("obrigatório");
    }

    @Test
    void cannotBypassValidationThroughConstructor() {
        assertThatThrownBy(() -> new CouponCode("AB-123")).isInstanceOf(InvalidCouponException.class);
        assertThatThrownBy(() -> new CouponCode(null)).isInstanceOf(InvalidCouponException.class);
    }
}
