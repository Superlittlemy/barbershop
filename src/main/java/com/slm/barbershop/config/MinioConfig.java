package com.slm.barbershop.config;

import io.minio.MinioClient;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Configuration
public class MinioConfig {

    @Bean
    public MinioClient minioClient(MinioProperties props) {
        return MinioClient.builder()
                .endpoint(props.getEndpoint())
                .credentials(props.getAccessKey(), props.getSecretKey())
                .build();
    }

    @Component
    @ConfigurationProperties(prefix = "minio")
    @Getter
    @Setter
    public static class MinioProperties {

        private String endpoint;

        private String accessKey;

        private String secretKey;

        private String bucket;

        private String publicBaseUrl;

    }

}
