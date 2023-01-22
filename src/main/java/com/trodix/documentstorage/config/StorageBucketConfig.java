package com.trodix.documentstorage.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.minio.MinioClient;

@Configuration
public class StorageBucketConfig {

    @Value("${app.storage.endpoint}")
    private String endpoint;

    @Value("${app.storage.accessKey}")
    private String accessKey;

    @Value("${app.storage.secretKey}")
    private String secretKey;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

}
