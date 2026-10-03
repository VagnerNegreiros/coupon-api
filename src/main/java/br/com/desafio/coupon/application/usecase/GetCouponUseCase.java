package br.com.desafio.coupon.application.usecase;

import br.com.desafio.coupon.application.dto.CouponOutput;
import br.com.desafio.coupon.application.exception.CouponNotFoundException;
import br.com.desafio.coupon.application.port.out.CouponRepository;

import java.util.UUID;

/** Consulta um cupom pelo id, inclusive os deletados (o soft delete preserva o histórico). */
public class GetCouponUseCase {

    private final CouponRepository couponRepository;

    public GetCouponUseCase(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    public CouponOutput execute(UUID couponId) {
        return couponRepository.findById(couponId)
                .map(CouponOutput::from)
                .orElseThrow(() -> new CouponNotFoundException(couponId));
    }
}
