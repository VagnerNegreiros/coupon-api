package br.com.desafio.coupon.domain.coupon;

import br.com.desafio.coupon.domain.exception.CouponAlreadyDeletedException;
import br.com.desafio.coupon.domain.exception.InvalidCouponException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CouponTest {

    private static final Instant NOW = Instant.parse("2026-01-10T12:00:00Z");
    private static final Instant TOMORROW = NOW.plus(Duration.ofDays(1));

    private static Coupon validCoupon() {
        return Coupon.create("ABC-123", "Desconto de boas-vindas", new BigDecimal("0.8"), TOMORROW, false, NOW);
    }

    @Nested
    class Create {

        @Test
        void createsActiveCouponWithSanitizedCode() {
            Coupon coupon = validCoupon();

            assertThat(coupon.getId()).isNotNull();
            assertThat(coupon.getCode()).isEqualTo("ABC123");
            assertThat(coupon.getDescription()).isEqualTo("Desconto de boas-vindas");
            assertThat(coupon.getDiscountValue()).isEqualByComparingTo("0.8");
            assertThat(coupon.getExpirationDate()).isEqualTo(TOMORROW);
            assertThat(coupon.getStatus()).isEqualTo(CouponStatus.ACTIVE);
            assertThat(coupon.isPublished()).isFalse();
            assertThat(coupon.isRedeemed()).isFalse();
            assertThat(coupon.getCreatedAt()).isEqualTo(NOW);
            assertThat(coupon.getDeletedAt()).isNull();
        }

        @Test
        void canBeCreatedAlreadyPublished() {
            Coupon coupon = Coupon.create("ABC123", "desc", new BigDecimal("1"), TOMORROW, true, NOW);

            assertThat(coupon.isPublished()).isTrue();
        }

        @Test
        void eachCouponGetsItsOwnId() {
            assertThat(validCoupon().getId()).isNotEqualTo(validCoupon().getId());
        }

        @Test
        void rejectsExpirationDateInThePast() {
            Instant oneMillisecondAgo = NOW.minusMillis(1);

            assertThatThrownBy(() -> Coupon.create("ABC123", "desc", BigDecimal.ONE, oneMillisecondAgo, false, NOW))
                    .isInstanceOf(InvalidCouponException.class)
                    .hasMessageContaining("passado")
                    .extracting("field").isEqualTo("expirationDate");
        }

        @Test
        void acceptsExpirationDateEqualToNow() {
            Coupon coupon = Coupon.create("ABC123", "desc", BigDecimal.ONE, NOW, false, NOW);

            assertThat(coupon.getExpirationDate()).isEqualTo(NOW);
        }

        @Test
        void rejectsMissingExpirationDate() {
            assertThatThrownBy(() -> Coupon.create("ABC123", "desc", BigDecimal.ONE, null, false, NOW))
                    .isInstanceOf(InvalidCouponException.class)
                    .extracting("field").isEqualTo("expirationDate");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        void rejectsMissingDescription(String description) {
            assertThatThrownBy(() -> Coupon.create("ABC123", description, BigDecimal.ONE, TOMORROW, false, NOW))
                    .isInstanceOf(InvalidCouponException.class)
                    .extracting("field").isEqualTo("description");
        }

        @Test
        void rejectsMissingDiscountValue() {
            assertThatThrownBy(() -> Coupon.create("ABC123", "desc", null, TOMORROW, false, NOW))
                    .isInstanceOf(InvalidCouponException.class)
                    .extracting("field").isEqualTo("discountValue");
        }

        @Test
        void rejectsMissingCode() {
            assertThatThrownBy(() -> Coupon.create(null, "desc", BigDecimal.ONE, TOMORROW, false, NOW))
                    .isInstanceOf(InvalidCouponException.class)
                    .extracting("field").isEqualTo("code");
        }
    }

    @Nested
    class Delete {

        @Test
        void softDeleteMarksAsDeletedAndKeepsAllRegistrationData() {
            Coupon coupon = validCoupon();
            Instant deletionTime = NOW.plusSeconds(60);

            coupon.delete(deletionTime);

            assertThat(coupon.isDeleted()).isTrue();
            assertThat(coupon.getStatus()).isEqualTo(CouponStatus.DELETED);
            assertThat(coupon.getDeletedAt()).isEqualTo(deletionTime);
            assertThat(coupon.getCode()).isEqualTo("ABC123");
            assertThat(coupon.getDescription()).isEqualTo("Desconto de boas-vindas");
            assertThat(coupon.getDiscountValue()).isEqualByComparingTo("0.8");
            assertThat(coupon.getExpirationDate()).isEqualTo(TOMORROW);
        }

        @Test
        void cannotBeDeletedTwice() {
            Coupon coupon = validCoupon();
            coupon.delete(NOW);

            assertThatThrownBy(() -> coupon.delete(NOW.plusSeconds(1)))
                    .isInstanceOf(CouponAlreadyDeletedException.class)
                    .hasMessageContaining(coupon.getId().toString());
            assertThat(coupon.getDeletedAt()).as("data da primeira deleção é preservada").isEqualTo(NOW);
        }

        @Test
        void canBeDeletedEvenIfPublishedOrExpired() {
            Coupon expiredAndPublished = Coupon.restore(java.util.UUID.randomUUID(), "ABC123", "desc",
                    BigDecimal.ONE, NOW.minus(Duration.ofDays(30)), CouponStatus.ACTIVE, true, false,
                    NOW.minus(Duration.ofDays(60)), null, 0L);

            expiredAndPublished.delete(NOW);

            assertThat(expiredAndPublished.isDeleted()).isTrue();
        }

        @Test
        void restoredDeletedCouponCannotBeDeletedAgain() {
            Coupon alreadyDeleted = Coupon.restore(java.util.UUID.randomUUID(), "ABC123", "desc",
                    BigDecimal.ONE, TOMORROW, CouponStatus.DELETED, false, false, NOW, NOW, 1L);

            assertThatThrownBy(() -> alreadyDeleted.delete(NOW))
                    .isInstanceOf(CouponAlreadyDeletedException.class);
        }
    }
}
