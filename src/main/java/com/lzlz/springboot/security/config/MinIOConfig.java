package com.lzlz.springboot.security.config;

import io.minio.MinioClient;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Configuration
@Data
public class MinIOConfig {
    @Value("${minio.endpoint}")
    public String endpoint;

    @Value("${minio.endpoint-bat}")
    public String endpointBat;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Bean("publicMinioClient")
    public MinioClient publicMinioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    @Bean("innerMinioClient")
    public MinioClient innerMinioClient() {
        return MinioClient.builder()
                .endpoint(endpointBat)
                .credentials(accessKey, secretKey)
                .build();
    }
}
