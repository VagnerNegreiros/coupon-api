package br.com.desafio.coupon.application.usecase;

import br.com.desafio.coupon.application.InMemoryCouponRepository;
import br.com.desafio.coupon.application.exception.CouponNotFoundException;
import br.com.desafio.coupon.domain.coupon.Coupon;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GetCouponUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-01-10T12:00:00Z");

    private final InMemoryCouponRepository repository = new InMemoryCouponRepository();
    private final GetCouponUseCase getCoupon = new GetCouponUseCase(repository);

    @Test
    void returnsExistingCoupon() {
        Coupon saved = repository.save(
                Coupon.create("XYZ-789", "Desconto", BigDecimal.TEN, NOW.plusSeconds(60), false, NOW));

        assertThat(getCoupon.execute(saved.getId()).code()).isEqualTo("XYZ789");
    }

    @Test
    void failsWhenCouponDoesNotExist() {
        assertThatThrownBy(() -> getCoupon.execute(UUID.randomUUID()))
                .isInstanceOf(CouponNotFoundException.class);
    }
}
