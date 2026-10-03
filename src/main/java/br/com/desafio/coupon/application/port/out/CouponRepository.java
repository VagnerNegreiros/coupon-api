package br.com.desafio.coupon.application.port.out;

import br.com.desafio.coupon.domain.coupon.Coupon;

import java.util.Optional;
import java.util.UUID;

/**
 * Porta de saída para persistência de cupons. A camada application conhece apenas este contrato;
 * a implementação (JPA, memória, etc.) fica na infraestrutura.
 */
public interface CouponRepository {

    /**
     * Persiste o cupom e devolve o estado salvo.
     *
     * @throws br.com.desafio.coupon.application.exception.ConcurrentCouponModificationException
     *         se o cupom foi alterado por outra operação desde que foi lido
     */
    Coupon save(Coupon coupon);

    Optional<Coupon> findById(UUID id);
}
