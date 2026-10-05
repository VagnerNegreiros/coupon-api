package br.com.desafio.coupon.infrastructure.web;

import br.com.desafio.coupon.application.dto.CreateCouponCommand;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Corpo do POST /coupon. Só expõe os campos que o cliente pode informar: id, status e redeemed são
 * controlados pela aplicação e, se enviados, são ignorados.
 */
@Schema(name = "CreateCouponRequest")
public record CreateCouponRequest(
        @Schema(description = "Código alfanumérico. Caracteres especiais são removidos; o resultado deve ter 6 caracteres.",
                example = "ABC-123", requiredMode = Schema.RequiredMode.REQUIRED)
        String code,

        @Schema(description = "Descrição do cupom.", example = "Desconto de boas-vindas",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String description,

        @Schema(description = "Valor do desconto. Mínimo 0.5, sem máximo.", example = "0.8",
                requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal discountValue,

        @Schema(description = "Data de expiração (ISO-8601). Não pode estar no passado.",
                example = "2030-12-31T23:59:59Z", requiredMode = Schema.RequiredMode.REQUIRED)
        Instant expirationDate,

        @Schema(description = "Se o cupom já nasce publicado.", example = "false", defaultValue = "false")
        Boolean published) {

    CreateCouponCommand toCommand() {
        return new CreateCouponCommand(code, description, discountValue, expirationDate,
                Boolean.TRUE.equals(published));
    }
}
