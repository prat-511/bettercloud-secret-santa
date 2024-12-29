package com.bettercloud.santa.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI secretSantaOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Secret Santa API")
                        .description("API for managing Secret Santa assignments")
                        .version("1.0"));
    }
}
