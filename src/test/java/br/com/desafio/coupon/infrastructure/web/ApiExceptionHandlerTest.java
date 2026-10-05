package br.com.desafio.coupon.infrastructure.web;

import br.com.desafio.coupon.application.dto.CreateCouponCommand;
import br.com.desafio.coupon.application.exception.ConcurrentCouponModificationException;
import br.com.desafio.coupon.application.usecase.CreateCouponUseCase;
import br.com.desafio.coupon.application.usecase.DeleteCouponUseCase;
import br.com.desafio.coupon.application.usecase.GetCouponUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testa só o adapter HTTP para falhas difíceis de provocar ponta a ponta (corrida entre requisições e
 * erro inesperado). As regras de negócio continuam cobertas por testes sem mock.
 */
@WebMvcTest(CouponController.class)
class ApiExceptionHandlerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CreateCouponUseCase createCoupon;

    @MockitoBean
    private GetCouponUseCase getCoupon;

    @MockitoBean
    private DeleteCouponUseCase deleteCoupon;

    @Test
    void concurrentModificationReturns409() throws Exception {
        UUID id = UUID.randomUUID();
        willThrow(new ConcurrentCouponModificationException(id, new RuntimeException("lock otimista")))
                .given(deleteCoupon).execute(id);

        mvc.perform(delete("/coupon/{id}", id))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Conflito de concorrência"))
                .andExpect(jsonPath("$.detail").value(containsString(id.toString())));
    }

    @Test
    void unexpectedErrorReturns500WithoutLeakingInternalDetails() throws Exception {
        given(createCoupon.execute(any(CreateCouponCommand.class)))
                .willThrow(new IllegalStateException("senha do banco: segredo"));

        mvc.perform(post("/coupon").contentType(MediaType.APPLICATION_JSON).content("""
                        {"code": "ABC123", "description": "d", "discountValue": 1, "expirationDate": "2099-01-01T00:00:00Z"}
                        """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value("Erro interno"))
                .andExpect(jsonPath("$.detail").value(not(containsString("segredo"))));
    }
}
