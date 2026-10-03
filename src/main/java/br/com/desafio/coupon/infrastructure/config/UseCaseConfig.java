package br.com.desafio.coupon.infrastructure.config;

import br.com.desafio.coupon.application.port.out.CouponRepository;
import br.com.desafio.coupon.application.usecase.CreateCouponUseCase;
import br.com.desafio.coupon.application.usecase.DeleteCouponUseCase;
import br.com.desafio.coupon.application.usecase.GetCouponUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.InstantSource;

/**
 * Composition root: é aqui (na infraestrutura) que o Spring conhece os use cases. Assim as classes da
 * camada application continuam sem nenhuma anotação ou import de framework.
 */
@Configuration
public class UseCaseConfig {

    @Bean
    InstantSource clock() {
        return Clock.systemUTC();
    }

    @Bean
    CreateCouponUseCase createCouponUseCase(CouponRepository couponRepository, InstantSource clock) {
        return new CreateCouponUseCase(couponRepository, clock);
    }

    @Bean
    GetCouponUseCase getCouponUseCase(CouponRepository couponRepository) {
        return new GetCouponUseCase(couponRepository);
    }

    @Bean
    DeleteCouponUseCase deleteCouponUseCase(CouponRepository couponRepository, InstantSource clock) {
        return new DeleteCouponUseCase(couponRepository, clock);
    }
}
