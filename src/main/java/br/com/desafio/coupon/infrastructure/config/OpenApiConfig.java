package br.com.desafio.coupon.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI couponOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Coupon API")
                .version("1.0")
                .description("API de cupons de desconto: cadastro, consulta e soft delete."));
    }
}
