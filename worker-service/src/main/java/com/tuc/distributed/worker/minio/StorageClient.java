package com.tuc.distributed.worker.minio;

import java.io.InputStream;
import java.util.List;

public interface StorageClient {
    String readText(String bucket, String objectKey);
    void writeText(String bucket, String objectKey, String content);
    List<String> readMany(String bucket, List<String> objectKeys);
    InputStream readStream(String bucket, String objectKey);
}
