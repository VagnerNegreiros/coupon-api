package br.com.desafio.coupon.infrastructure.persistence;

import br.com.desafio.coupon.application.dto.CreateCouponCommand;
import br.com.desafio.coupon.application.exception.ConcurrentCouponModificationException;
import br.com.desafio.coupon.application.port.out.CouponRepository;
import br.com.desafio.coupon.application.usecase.CreateCouponUseCase;
import br.com.desafio.coupon.application.usecase.DeleteCouponUseCase;
import br.com.desafio.coupon.domain.coupon.Coupon;
import br.com.desafio.coupon.domain.exception.CouponAlreadyDeletedException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * "Não deve ser possível deletar um cupom já deletado" também precisa valer quando duas requisições
 * chegam ao mesmo tempo. Sem lock otimista, ambas leriam ACTIVE e ambas "deletariam" o cupom.
 */
@SpringBootTest
class CouponConcurrencyIntegrationTest {

    @Autowired
    private CreateCouponUseCase createCoupon;

    @Autowired
    private DeleteCouponUseCase deleteCoupon;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private SpringDataCouponRepository jpaRepository;

    private UUID newCoupon() {
        return createCoupon.execute(new CreateCouponCommand("CON-123", "Concorrência", BigDecimal.ONE,
                Instant.parse("2099-01-01T00:00:00Z"), false)).id();
    }

    @Test
    void staleCopyCannotOverwriteADeletionThatAlreadyHappened() {
        UUID id = newCoupon();
        Coupon firstRequest = couponRepository.findById(id).orElseThrow();
        Coupon secondRequest = couponRepository.findById(id).orElseThrow();

        firstRequest.delete(Instant.now());
        couponRepository.save(firstRequest);

        secondRequest.delete(Instant.now()); // em memória ainda estava ACTIVE
        assertThatThrownBy(() -> couponRepository.save(secondRequest))
                .isInstanceOf(ConcurrentCouponModificationException.class);
    }

    @Test
    void onlyOneOfManySimultaneousDeletesSucceeds() throws Exception {
        UUID id = newCoupon();
        int threads = 10;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        List<Future<?>> results = new ArrayList<>();
        Callable<Void> deleteTask = () -> {
            start.await();
            deleteCoupon.execute(id);
            return null;
        };
        for (int i = 0; i < threads; i++) {
            results.add(executor.submit(deleteTask));
        }
        start.countDown();

        int successes = 0;
        for (Future<?> result : results) {
            try {
                result.get();
                successes++;
            } catch (ExecutionException ex) {
                assertThat(ex.getCause()).isInstanceOfAny(
                        CouponAlreadyDeletedException.class, ConcurrentCouponModificationException.class);
            }
        }
        executor.shutdown();

        assertThat(successes).isEqualTo(1);
        CouponEntity row = jpaRepository.findById(id).orElseThrow();
        assertThat(row.getStatus()).isEqualTo("DELETED");
        assertThat(row.getVersion()).as("apenas uma gravação de deleção").isEqualTo(1L);
    }
}
