package br.com.desafio.coupon.infrastructure.web;

import br.com.desafio.coupon.application.dto.CouponOutput;
import br.com.desafio.coupon.application.usecase.CreateCouponUseCase;
import br.com.desafio.coupon.application.usecase.DeleteCouponUseCase;
import br.com.desafio.coupon.application.usecase.GetCouponUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ProblemDetail;
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
@Tag(name = "Coupon", description = "Cadastro, consulta e exclusão (soft delete) de cupons")
@RestController
@RequestMapping("/coupon")
public class CouponController {

    private static final String PROBLEM_JSON = "application/problem+json";

    private final CreateCouponUseCase createCoupon;
    private final GetCouponUseCase getCoupon;
    private final DeleteCouponUseCase deleteCoupon;

    public CouponController(CreateCouponUseCase createCoupon, GetCouponUseCase getCoupon,
                            DeleteCouponUseCase deleteCoupon) {
        this.createCoupon = createCoupon;
        this.getCoupon = getCoupon;
        this.deleteCoupon = deleteCoupon;
    }

    @Operation(summary = "Cadastra um cupom",
            description = "O código tem caracteres especiais removidos e precisa resultar em 6 caracteres alfanuméricos.")
    @ApiResponse(responseCode = "201", description = "Cupom criado")
    @ApiResponse(responseCode = "400", description = "Dados inválidos ou JSON malformado",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping
    public ResponseEntity<CouponResponse> create(@RequestBody CreateCouponRequest request) {
        CouponOutput created = createCoupon.execute(request.toCommand());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(CouponResponse.from(created));
    }

    @Operation(summary = "Consulta um cupom pelo id",
            description = "Cupons deletados também são retornados, com status DELETED.")
    @ApiResponse(responseCode = "200", description = "Cupom encontrado")
    @ApiResponse(responseCode = "400", description = "Id não é um UUID válido",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Cupom não encontrado",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{id}")
    public CouponResponse getById(@Parameter(description = "Id (UUID) retornado no cadastro") @PathVariable UUID id) {
        return CouponResponse.from(getCoupon.execute(id));
    }

    @Operation(summary = "Deleta um cupom (soft delete)",
            description = "Os dados são preservados e o status passa a DELETED. Não é possível deletar duas vezes.")
    @ApiResponse(responseCode = "204", description = "Cupom deletado")
    @ApiResponse(responseCode = "400", description = "Id não é um UUID válido",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Cupom não encontrado",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Cupom já deletado",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@Parameter(description = "Id (UUID) retornado no cadastro") @PathVariable UUID id) {
        deleteCoupon.execute(id);
        return ResponseEntity.noContent().build();
    }
}
