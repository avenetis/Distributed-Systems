package com.tuc.distributed.worker.minio;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;//String <-> bytes
import java.util.ArrayList;
import java.util.List;

@Component
public class MinioStorageClient implements StorageClient {

    private final MinioClient minioClient;//εργαλείο επικοινωνίας με τον MinIO server

    public MinioStorageClient(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    @Override
    public String readText(String bucket, String objectKey) {
        try (InputStream inputStream = readStream(bucket, objectKey)) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read object %s/%s".formatted(bucket, objectKey), e);
        }//Η μέθοδος διαβάζει ένα object από το MinIO και το επιστρέφει ως String.
    }//Παράδειγμα εισόδου:readText("mapreduce-input", "splits/job-1/split_0.txt");

    @Override
    public void writeText(String bucket, String objectKey, String content) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes)) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)//σε ποιο bucket θα αποθηκευτεί
                            .object(objectKey)//με ποιο όνομα path
                            .stream(inputStream, bytes.length, -1) //ποια δεδομένα θα γραφτούν και τι μέγεθος έχουν
                            .contentType("text/plain")//ότι το object είναι text αρχείο
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
            );//MinIO object -> bytes που διαβάζονται μέσω stream
        } catch (Exception e) {
            throw new IllegalStateException("Failed to open object %s/%s".formatted(bucket, objectKey), e);
        }
    }
}
