package br.com.desafio.coupon.infrastructure.web;

import br.com.desafio.coupon.application.dto.CouponOutput;
import br.com.desafio.coupon.domain.coupon.CouponStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Contrato HTTP de resposta, conforme a documentação da API. */
@Schema(name = "CouponResponse")
public record CouponResponse(
        @Schema(example = "cef9d1e3-aae5-4ab6-a297-358c6032b1e7") UUID id,
        @Schema(example = "ABC123") String code,
        @Schema(example = "Desconto de boas-vindas") String description,
        @Schema(example = "0.8") BigDecimal discountValue,
        @Schema(example = "2030-12-31T23:59:59Z") Instant expirationDate,
        @Schema(example = "ACTIVE") CouponStatus status,
        @Schema(example = "false") boolean published,
        @Schema(example = "false") boolean redeemed) {

    static CouponResponse from(CouponOutput coupon) {
        return new CouponResponse(
                coupon.id(),
                coupon.code(),
                coupon.description(),
                coupon.discountValue(),
                coupon.expirationDate(),
                coupon.status(),
                coupon.published(),
                coupon.redeemed());
    }
}
