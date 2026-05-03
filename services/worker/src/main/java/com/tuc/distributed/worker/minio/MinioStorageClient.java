package com.tuc.distributed.worker.minio;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class MinioStorageClient implements StorageClient {

    private final MinioClient minioClient;

    public MinioStorageClient(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    @Override
    public String readText(String bucket, String objectKey) {
        try (InputStream inputStream = readStream(bucket, objectKey)) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read object %s/%s".formatted(bucket, objectKey), e);
        }
    }

    @Override
    public void writeText(String bucket, String objectKey, String content) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes)) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .stream(inputStream, bytes.length, -1)
                            .contentType("text/plain")
                            .build()
            );
        } catch (Exception e) {
            throw new IllegalStateException("Failed to write object %s/%s".formatted(bucket, objectKey), e);
        }
    }

    @Override
    public List<String> readMany(String bucket, List<String> objectKeys) {
        List<String> contents = new ArrayList<>();
        for (String objectKey : objectKeys) {
            contents.add(readText(bucket, objectKey));
        }
        return contents;
    }

    @Override
    public InputStream readStream(String bucket, String objectKey) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .build()
            );
        } catch (Exception e) {
            throw new IllegalStateException("Failed to open object %s/%s".formatted(bucket, objectKey), e);
        }
    }
}
