package br.com.desafio.coupon.application;

import br.com.desafio.coupon.application.port.out.CouponRepository;
import br.com.desafio.coupon.domain.coupon.Coupon;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementação real (em memória) da porta, usada no lugar de mocks. Guarda cópias dos cupons, como um
 * banco faria: se um use case alterar o objeto e esquecer de chamar {@code save}, o teste percebe.
 */
public class InMemoryCouponRepository implements CouponRepository {

    private final Map<UUID, Coupon> storage = new HashMap<>();

    @Override
    public Coupon save(Coupon coupon) {
        long nextVersion = coupon.getVersion() == null ? 0 : coupon.getVersion() + 1;
        Coupon stored = copy(coupon, nextVersion);
        storage.put(coupon.getId(), stored);
        return copy(stored, nextVersion);
    }

    @Override
    public Optional<Coupon> findById(UUID id) {
        return Optional.ofNullable(storage.get(id)).map(c -> copy(c, c.getVersion()));
    }

    public int count() {
        return storage.size();
    }

    private static Coupon copy(Coupon c, Long version) {
        return Coupon.restore(c.getId(), c.getCode(), c.getDescription(), c.getDiscountValue(),
                c.getExpirationDate(), c.getStatus(), c.isPublished(), c.isRedeemed(), c.getCreatedAt(),
                c.getDeletedAt(), version);
    }
}
