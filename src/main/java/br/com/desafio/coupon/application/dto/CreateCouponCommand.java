package br.com.desafio.coupon.application.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** Intenção de criar um cupom, independente de como ela chegou (HTTP, fila, CLI...). */
public record CreateCouponCommand(
        String code,
        String description,
        BigDecimal discountValue,
        Instant expirationDate,
        boolean published) {
}
