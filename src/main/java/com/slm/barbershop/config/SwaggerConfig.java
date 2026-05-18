package com.slm.barbershop.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI barbershopOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("理发店接口文档")
                        .description("理发店管理系统 OpenAPI 3.0 接口文档")
                        .version("v1.0")
                        .contact(new Contact().name("Superlittlemy"))
                        .license(new License().name("Apache License 2.0").url("https://www.apache.org/licenses/LICENSE-2.0")));
    }

}