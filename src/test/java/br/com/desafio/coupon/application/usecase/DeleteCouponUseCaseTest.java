package br.com.desafio.coupon.application.usecase;

import br.com.desafio.coupon.application.InMemoryCouponRepository;
import br.com.desafio.coupon.application.exception.CouponNotFoundException;
import br.com.desafio.coupon.domain.coupon.Coupon;
import br.com.desafio.coupon.domain.coupon.CouponStatus;
import br.com.desafio.coupon.domain.exception.CouponAlreadyDeletedException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.InstantSource;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeleteCouponUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-01-10T12:00:00Z");

    private final InMemoryCouponRepository repository = new InMemoryCouponRepository();
    private final DeleteCouponUseCase deleteCoupon = new DeleteCouponUseCase(repository, InstantSource.fixed(NOW));

    private UUID existingCouponId() {
        Coupon coupon = Coupon.create("ABC123", "Desconto", BigDecimal.ONE, NOW.plusSeconds(60), false, NOW);
        return repository.save(coupon).getId();
    }

    @Test
    void softDeletesAndPersistsTheChange() {
        UUID id = existingCouponId();

        deleteCoupon.execute(id);

        Coupon stored = repository.findById(id).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(CouponStatus.DELETED);
        assertThat(stored.getDeletedAt()).isEqualTo(NOW);
        assertThat(stored.getCode()).isEqualTo("ABC123");
        assertThat(repository.count()).as("registro não é removido fisicamente").isEqualTo(1);
    }

    @Test
    void cannotDeleteTheSameCouponTwice() {
        UUID id = existingCouponId();
        deleteCoupon.execute(id);

        assertThatThrownBy(() -> deleteCoupon.execute(id)).isInstanceOf(CouponAlreadyDeletedException.class);
    }

    @Test
    void failsWhenCouponDoesNotExist() {
        UUID unknownId = UUID.randomUUID();

        assertThatThrownBy(() -> deleteCoupon.execute(unknownId))
                .isInstanceOf(CouponNotFoundException.class)
                .hasMessageContaining(unknownId.toString());
    }
}
