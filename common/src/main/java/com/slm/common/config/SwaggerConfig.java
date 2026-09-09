package com.slm.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "springdoc-info")
public class SwaggerConfig {

    private String title;
    private String description;
    private String version;
    private String contactName;

    @Bean
    public OpenAPI barbershopOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title(title)
                        .description(description)
                        .version(version)
                        .contact(new Contact().name(contactName))
                        .license(new License().name("Apache License 2.0").url("https://www.apache.org/licenses/LICENSE-2.0")));
    }

}