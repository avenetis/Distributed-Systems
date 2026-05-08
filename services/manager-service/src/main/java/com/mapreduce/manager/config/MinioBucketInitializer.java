package com.mapreduce.manager.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import jakarta.annotation.PostConstruct;

@Component
public class MinioBucketInitializer {
    // This component can be used to initialize required buckets in Minio on application startup.
    // For example, we can create buckets for storing mapper outputs, reducer outputs, and job artifacts.
    private static final Logger log = LoggerFactory.getLogger(MinioBucketInitializer.class);
    private final MinioClient minioClient;

    private static final List<String> REQUIRED_BUCKETS = List.of("mapreduce-input", "mapreduce-intermediate", "mapreduce-output", "mapreduce-code");

    public MinioBucketInitializer(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    @PostConstruct
    public void initBuckets() {
        for(String bucket : REQUIRED_BUCKETS) {
            try {
                boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
                if(!exists) {
                    minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                    log.info("Created MinIO bucket: {}", bucket);
                } else {
                    log.info("MinIO bucket exists: {}", bucket);
                }
            } catch (Exception e) {
                log.error("Failed to create bucket {}: {}", bucket, e.getMessage());
            }
        }
    }
}
