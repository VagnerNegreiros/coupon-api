package br.com.desafio.coupon.application.usecase;

import br.com.desafio.coupon.application.exception.CouponNotFoundException;
import br.com.desafio.coupon.application.port.out.CouponRepository;
import br.com.desafio.coupon.domain.coupon.Coupon;

import java.time.InstantSource;
import java.util.UUID;

/**
 * Deleta (soft delete) um cupom. A regra "não deletar duas vezes" pertence ao domínio
 * ({@link Coupon#delete}); este use case apenas busca, delega e persiste.
 */
public class DeleteCouponUseCase {

    private final CouponRepository couponRepository;
    private final InstantSource clock;

    public DeleteCouponUseCase(CouponRepository couponRepository, InstantSource clock) {
        this.couponRepository = couponRepository;
        this.clock = clock;
    }

    public void execute(UUID couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new CouponNotFoundException(couponId));

        coupon.delete(clock.instant());

        couponRepository.save(coupon);
    }
}
