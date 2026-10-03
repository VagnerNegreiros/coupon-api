package br.com.desafio.coupon.application.usecase;

import br.com.desafio.coupon.application.InMemoryCouponRepository;
import br.com.desafio.coupon.application.dto.CouponOutput;
import br.com.desafio.coupon.application.dto.CreateCouponCommand;
import br.com.desafio.coupon.domain.coupon.CouponStatus;
import br.com.desafio.coupon.domain.exception.InvalidCouponException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.InstantSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreateCouponUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-01-10T12:00:00Z");

    private final InMemoryCouponRepository repository = new InMemoryCouponRepository();
    private final CreateCouponUseCase createCoupon = new CreateCouponUseCase(repository, InstantSource.fixed(NOW));

    @Test
    void createsAndPersistsCoupon() {
        CouponOutput output = createCoupon.execute(new CreateCouponCommand(
                "ABC-123", "Desconto", new BigDecimal("0.8"), NOW.plusSeconds(3600), true));

        assertThat(output.code()).isEqualTo("ABC123");
        assertThat(output.status()).isEqualTo(CouponStatus.ACTIVE);
        assertThat(output.published()).isTrue();
        assertThat(output.redeemed()).isFalse();
        assertThat(repository.findById(output.id())).isPresent();
    }

    @Test
    void usesTheInjectedClockToValidateExpiration() {
        CreateCouponCommand expiresOneSecondBeforeNow = new CreateCouponCommand(
                "ABC123", "Desconto", BigDecimal.ONE, NOW.minusSeconds(1), false);

        assertThatThrownBy(() -> createCoupon.execute(expiresOneSecondBeforeNow))
                .isInstanceOf(InvalidCouponException.class);
    }

    @Test
    void doesNotPersistInvalidCoupon() {
        CreateCouponCommand invalid = new CreateCouponCommand(
                "ABC123", "Desconto", new BigDecimal("0.1"), NOW.plusSeconds(60), false);

        assertThatThrownBy(() -> createCoupon.execute(invalid)).isInstanceOf(InvalidCouponException.class);
        assertThat(repository.count()).isZero();
    }
}
