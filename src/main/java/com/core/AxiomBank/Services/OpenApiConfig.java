package com.core.AxiomBank.Services;


import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class OpenApiConfig {

    @Primary
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AXIOM CORE | Banking API")
                        .description("Proprietary high-throughput banking engine. " +
                                "Features include: Atomic multi-currency transfers, " +
                                "encrypted client onboarding, and automated account lifecycle management.")
                        .version("1.0.0-PROD"));
    }
}