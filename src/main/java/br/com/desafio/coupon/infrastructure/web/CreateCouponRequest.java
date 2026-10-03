package br.com.desafio.coupon.infrastructure.web;

import br.com.desafio.coupon.application.dto.CreateCouponCommand;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Corpo do POST /coupon. Só expõe os campos que o cliente pode informar: id, status e redeemed são
 * controlados pela aplicação e, se enviados, são ignorados.
 */
public record CreateCouponRequest(
        String code,
        String description,
        BigDecimal discountValue,
        Instant expirationDate,
        Boolean published) {

    CreateCouponCommand toCommand() {
        return new CreateCouponCommand(code, description, discountValue, expirationDate,
                Boolean.TRUE.equals(published));
    }
}
