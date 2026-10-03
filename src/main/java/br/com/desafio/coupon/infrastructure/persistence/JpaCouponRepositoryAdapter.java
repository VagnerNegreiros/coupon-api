package br.com.desafio.coupon.infrastructure.persistence;

import br.com.desafio.coupon.application.exception.ConcurrentCouponModificationException;
import br.com.desafio.coupon.application.port.out.CouponRepository;
import br.com.desafio.coupon.domain.coupon.Coupon;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** Adapter que implementa a porta {@link CouponRepository} usando Spring Data JPA. */
@Component
public class JpaCouponRepositoryAdapter implements CouponRepository {

    private final SpringDataCouponRepository jpaRepository;

    public JpaCouponRepositoryAdapter(SpringDataCouponRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Coupon save(Coupon coupon) {
        try {
            return jpaRepository.saveAndFlush(CouponEntity.fromDomain(coupon)).toDomain();
        } catch (OptimisticLockingFailureException ex) {
            throw new ConcurrentCouponModificationException(coupon.getId(), ex);
        }
    }

    @Override
    public Optional<Coupon> findById(UUID id) {
        return jpaRepository.findById(id).map(CouponEntity::toDomain);
    }
}
