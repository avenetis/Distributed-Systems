package com.tuc.distributed.worker.config;

import io.minio.MinioClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MinioProperties.class)
public class MinioConfig {
//Η MinioConfig είναι η κλάση που δημιουργεί και ρυθμίζει τον MinioClient του worker
    @Bean
    public MinioClient minioClient(MinioProperties properties) {
        return MinioClient.builder()
                .endpoint(properties.endpoint())//σε ποιο MinIO server θα συνδεθεί
                .credentials(properties.accessKey(), properties.secretKey())//με ποιο access key και με ποιο secret key
                .build();
    }
}
