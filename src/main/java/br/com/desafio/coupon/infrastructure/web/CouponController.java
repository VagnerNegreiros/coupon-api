package br.com.desafio.coupon.infrastructure.web;

import br.com.desafio.coupon.application.dto.CouponOutput;
import br.com.desafio.coupon.application.usecase.CreateCouponUseCase;
import br.com.desafio.coupon.application.usecase.DeleteCouponUseCase;
import br.com.desafio.coupon.application.usecase.GetCouponUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/** Adapter de entrada HTTP: traduz requisições em chamadas aos use cases. Sem regra de negócio. */
@RestController
@RequestMapping("/coupon")
public class CouponController {

    private final CreateCouponUseCase createCoupon;
    private final GetCouponUseCase getCoupon;
    private final DeleteCouponUseCase deleteCoupon;

    public CouponController(CreateCouponUseCase createCoupon, GetCouponUseCase getCoupon,
                            DeleteCouponUseCase deleteCoupon) {
        this.createCoupon = createCoupon;
        this.getCoupon = getCoupon;
        this.deleteCoupon = deleteCoupon;
    }

    @PostMapping
    public ResponseEntity<CouponResponse> create(@RequestBody CreateCouponRequest request) {
        CouponOutput created = createCoupon.execute(request.toCommand());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(CouponResponse.from(created));
    }

    @GetMapping("/{id}")
    public CouponResponse getById(@PathVariable UUID id) {
        return CouponResponse.from(getCoupon.execute(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        deleteCoupon.execute(id);
        return ResponseEntity.noContent().build();
    }
}
