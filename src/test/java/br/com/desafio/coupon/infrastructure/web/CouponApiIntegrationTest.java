package br.com.desafio.coupon.infrastructure.web;

import br.com.desafio.coupon.infrastructure.persistence.CouponEntity;
import br.com.desafio.coupon.infrastructure.persistence.SpringDataCouponRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes ponta a ponta: requisição HTTP real (MockMvc) → use case → JPA → banco H2, sem mocks.
 * Validam o comportamento observável da API, inclusive tentativas de quebrar as regras.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CouponApiIntegrationTest {

    private static final String FUTURE = "2099-12-31T23:59:59Z";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private SpringDataCouponRepository jpaRepository;

    private ResultActions postCoupon(String json) throws Exception {
        return mvc.perform(post("/coupon").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private String createCoupon(String code) throws Exception {
        String response = postCoupon("""
                {"code": "%s", "description": "Cupom de teste", "discountValue": 0.8, "expirationDate": "%s"}
                """.formatted(code, FUTURE))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    @Nested
    class Create {

        @Test
        void createsCouponFollowingTheApiContract() throws Exception {
            postCoupon("""
                    {
                      "code": "ABC-123",
                      "description": "Desconto de boas-vindas",
                      "discountValue": 0.8,
                      "expirationDate": "2099-11-04T17:14:45.180Z",
                      "published": false
                    }
                    """)
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", matchesPattern(".*/coupon/[0-9a-f-]{36}$")))
                    .andExpect(jsonPath("$.id").isString())
                    .andExpect(jsonPath("$.code").value("ABC123"))
                    .andExpect(jsonPath("$.description").value("Desconto de boas-vindas"))
                    .andExpect(jsonPath("$.discountValue").value(0.8))
                    .andExpect(jsonPath("$.expirationDate").value("2099-11-04T17:14:45.180Z"))
                    .andExpect(jsonPath("$.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.published").value(false))
                    .andExpect(jsonPath("$.redeemed").value(false));
        }

        @Test
        void persistsTheSanitizedCode() throws Exception {
            String id = createCoupon("X#Y@Z-9.8!7");

            assertThat(jpaRepository.findById(UUID.fromString(id)))
                    .get().extracting(CouponEntity::getCode).isEqualTo("XYZ987");
        }

        @Test
        void canBeCreatedAlreadyPublished() throws Exception {
            postCoupon("""
                    {"code": "PUB123", "description": "d", "discountValue": 1, "expirationDate": "%s", "published": true}
                    """.formatted(FUTURE))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.published").value(true));
        }

        @Test
        void publishedDefaultsToFalseWhenOmitted() throws Exception {
            postCoupon("""
                    {"code": "DEF123", "description": "d", "discountValue": 1, "expirationDate": "%s"}
                    """.formatted(FUTURE))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.published").value(false));
        }

        @Test
        void acceptsTheMinimumDiscountValue() throws Exception {
            postCoupon("""
                    {"code": "MIN050", "description": "d", "discountValue": 0.5, "expirationDate": "%s"}
                    """.formatted(FUTURE))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.discountValue").value(0.5));
        }

        @Test
        void hasNoUpperLimitForDiscountValue() throws Exception {
            String response = postCoupon("""
                    {"code": "BIG123", "description": "d", "discountValue": 1e30, "expirationDate": "%s"}
                    """.formatted(FUTURE))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();
            String id = JsonPath.read(response, "$.id");

            assertThat(jpaRepository.findById(UUID.fromString(id))).get()
                    .extracting(CouponEntity::getDiscountValue)
                    .satisfies(value -> assertThat(value).isEqualByComparingTo("1000000000000000000000000000000"));
        }

        @Test
        void rejectsDiscountValueWithWrongType() throws Exception {
            postCoupon("""
                    {"code": "ABC123", "description": "d", "discountValue": "abc", "expirationDate": "%s"}
                    """.formatted(FUTURE))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("Valor inválido para o campo 'discountValue'."));
        }

        @Test
        void clientCannotChooseIdStatusOrRedeemed() throws Exception {
            String forcedId = "00000000-0000-0000-0000-000000000001";
            postCoupon("""
                    {"id": "%s", "code": "HCK123", "description": "d", "discountValue": 1,
                     "expirationDate": "%s", "status": "DELETED", "redeemed": true}
                    """.formatted(forcedId, FUTURE))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(not(forcedId)))
                    .andExpect(jsonPath("$.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.redeemed").value(false));
        }

        @ParameterizedTest(name = "{0}")
        @CsvSource(delimiter = '|', value = {
                "code ausente          | {\"description\":\"d\",\"discountValue\":1,\"expirationDate\":\"2099-01-01T00:00:00Z\"}                  | code",
                "code vazio            | {\"code\":\"\",\"description\":\"d\",\"discountValue\":1,\"expirationDate\":\"2099-01-01T00:00:00Z\"}     | code",
                "code curto            | {\"code\":\"AB-12\",\"description\":\"d\",\"discountValue\":1,\"expirationDate\":\"2099-01-01T00:00:00Z\"} | code",
                "code longo            | {\"code\":\"ABC1234\",\"description\":\"d\",\"discountValue\":1,\"expirationDate\":\"2099-01-01T00:00:00Z\"} | code",
                "description ausente   | {\"code\":\"ABC123\",\"discountValue\":1,\"expirationDate\":\"2099-01-01T00:00:00Z\"}                    | description",
                "description em branco | {\"code\":\"ABC123\",\"description\":\"  \",\"discountValue\":1,\"expirationDate\":\"2099-01-01T00:00:00Z\"} | description",
                "discountValue ausente | {\"code\":\"ABC123\",\"description\":\"d\",\"expirationDate\":\"2099-01-01T00:00:00Z\"}                   | discountValue",
                "discountValue < 0.5   | {\"code\":\"ABC123\",\"description\":\"d\",\"discountValue\":0.49,\"expirationDate\":\"2099-01-01T00:00:00Z\"} | discountValue",
                "discountValue zero    | {\"code\":\"ABC123\",\"description\":\"d\",\"discountValue\":0,\"expirationDate\":\"2099-01-01T00:00:00Z\"} | discountValue",
                "discountValue negativo| {\"code\":\"ABC123\",\"description\":\"d\",\"discountValue\":-5,\"expirationDate\":\"2099-01-01T00:00:00Z\"} | discountValue",
                "expiration ausente    | {\"code\":\"ABC123\",\"description\":\"d\",\"discountValue\":1}                                          | expirationDate",
                "expiration no passado | {\"code\":\"ABC123\",\"description\":\"d\",\"discountValue\":1,\"expirationDate\":\"2020-01-01T00:00:00Z\"} | expirationDate",
        })
        void rejectsInvalidCouponWith400(String scenario, String json, String field) throws Exception {
            long before = jpaRepository.count();

            postCoupon(json)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.field").value(field))
                    .andExpect(jsonPath("$.detail").isNotEmpty());

            assertThat(jpaRepository.count()).as("nada é persistido").isEqualTo(before);
        }

        @Test
        void rejectsInvalidDateFormat() throws Exception {
            postCoupon("""
                    {"code": "ABC123", "description": "d", "discountValue": 1, "expirationDate": "31/12/2099"}
                    """)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value(containsString("expirationDate")));
        }

        @Test
        void rejectsMalformedJson() throws Exception {
            postCoupon("{\"code\": \"ABC123\",")
                    .andExpect(status().isBadRequest());
        }

        @Test
        void rejectsEmptyBody() throws Exception {
            postCoupon("")
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class Delete {

        @Test
        void softDeletesTheCoupon() throws Exception {
            String id = createCoupon("DEL-001");
            CouponEntity beforeDelete = jpaRepository.findById(UUID.fromString(id)).orElseThrow();

            mvc.perform(delete("/coupon/{id}", id))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));

            CouponEntity row = jpaRepository.findById(UUID.fromString(id)).orElseThrow();
            assertThat(row.getId()).isEqualTo(UUID.fromString(id));
            assertThat(row.getStatus()).isEqualTo("DELETED");
            assertThat(row.getDeletedAt()).isAfterOrEqualTo(row.getCreatedAt());
            assertThat(row.getCode()).isEqualTo("DEL001");
            assertThat(row.getDescription()).isEqualTo("Cupom de teste");
            assertThat(row.getDiscountValue()).isEqualByComparingTo("0.8");
            assertThat(row.getExpirationDate()).isEqualTo(Instant.parse(FUTURE));
            assertThat(row.isPublished()).isFalse();
            assertThat(row.isRedeemed()).isFalse();
            assertThat(row.getCreatedAt()).as("data de cadastro preservada").isEqualTo(beforeDelete.getCreatedAt());
        }

        @Test
        void deletedCouponIsStillQueryableWithDeletedStatus() throws Exception {
            String id = createCoupon("DEL-002");
            mvc.perform(delete("/coupon/{id}", id)).andExpect(status().isNoContent());

            mvc.perform(get("/coupon/{id}", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("DELETED"))
                    .andExpect(jsonPath("$.code").value("DEL002"))
                    .andExpect(jsonPath("$.description").value("Cupom de teste"));
        }

        @Test
        void cannotDeleteTwice() throws Exception {
            String id = createCoupon("DEL-003");
            mvc.perform(delete("/coupon/{id}", id)).andExpect(status().isNoContent());

            mvc.perform(delete("/coupon/{id}", id))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.title").value("Cupom já deletado"));

            assertThat(jpaRepository.findById(UUID.fromString(id))).get()
                    .extracting(CouponEntity::getVersion).as("segunda tentativa não altera o registro").isEqualTo(1L);
        }

        @Test
        void returns404ForUnknownCoupon() throws Exception {
            mvc.perform(delete("/coupon/{id}", UUID.randomUUID()))
                    .andExpect(status().isNotFound());
        }

        @Test
        void returns400ForInvalidId() throws Exception {
            mvc.perform(delete("/coupon/{id}", "nao-e-uuid"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class GetById {

        @Test
        void returnsTheCoupon() throws Exception {
            String id = createCoupon("GET-001");

            mvc.perform(get("/coupon/{id}", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id))
                    .andExpect(jsonPath("$.code").value("GET001"))
                    .andExpect(jsonPath("$.discountValue").value(0.8))
                    .andExpect(jsonPath("$.expirationDate").value(FUTURE))
                    .andExpect(jsonPath("$.status").value("ACTIVE"));
        }

        @Test
        void returns404ForUnknownCoupon() throws Exception {
            mvc.perform(get("/coupon/{id}", UUID.randomUUID()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Cupom não encontrado"));
        }

        @Test
        void returns400ForInvalidId() throws Exception {
            mvc.perform(get("/coupon/{id}", "123"))
                    .andExpect(status().isBadRequest());
        }
    }
}
