package br.com.desafio.coupon.application.usecase;

import br.com.desafio.coupon.application.dto.CouponOutput;
import br.com.desafio.coupon.application.dto.CreateCouponCommand;
import br.com.desafio.coupon.application.port.out.CouponRepository;
import br.com.desafio.coupon.domain.coupon.Coupon;

import java.time.InstantSource;

/** Cadastra um novo cupom. As regras ficam em {@link Coupon#create}; aqui só orquestramos o fluxo. */
public class CreateCouponUseCase {

    private final CouponRepository couponRepository;
    private final InstantSource clock;

    public CreateCouponUseCase(CouponRepository couponRepository, InstantSource clock) {
        this.couponRepository = couponRepository;
        this.clock = clock;
    }

    public CouponOutput execute(CreateCouponCommand command) {
        Coupon coupon = Coupon.create(
                command.code(),
                command.description(),
                command.discountValue(),
                command.expirationDate(),
                command.published(),
                clock.instant());

        return CouponOutput.from(couponRepository.save(coupon));
    }
}
