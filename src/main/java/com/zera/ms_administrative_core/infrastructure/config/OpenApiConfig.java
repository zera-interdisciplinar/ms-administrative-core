package com.zera.ms_administrative_core.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("ms-administrative-core")
                .description("Nucleo administrativo: usuarios, organizacoes, unidades, telefones, "
                        + "recicladoras, convites e alertas. Autenticacao via JWT Bearer emitido "
                        + "por POST /api/v1/auth/login.")
                .version("1.0.0")
                .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")));
    }
}
