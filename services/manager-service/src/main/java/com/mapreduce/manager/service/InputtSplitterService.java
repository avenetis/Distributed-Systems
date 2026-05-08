package com.mapreduce.manager.service;

import io.minio.MinioClient;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.mapreduce.manager.entity.Job;
import com.mapreduce.manager.entity.Task;
import com.mapreduce.manager.entity.TaskStatus;
import com.mapreduce.manager.repository.TaskRepository;
import com.mapreduce.manager.repository.TaskType;

import org.springframework.transaction.annotation.Transactional;

import io.minio.GetObjectArgs;
import io.minio.PutObjectArgs;

@Service
public class InputtSplitterService {
    private final MinioClient minioClient;

    private static final Logger log = LoggerFactory.getLogger(InputtSplitterService.class);

    private static final String INPUT_BUCKET = "mapreduce-input";
    private static final String INTERMEDIATE_BUCKET = "mapreduce-intermediate";
    private final TaskRepository taskRepository;


    public InputtSplitterService(TaskRepository taskRepository, MinioClient minioClient) {
        this.taskRepository = taskRepository;
        this.minioClient = minioClient;
    }

    @Transactional
    public List<SplitInfo> splitInput(Job job) {
        log.info("Splitting input for job: {}, numMappers: {}", job.getId(), job.getNumMappers());

        // read input file from minio
        List<String> lines = readLinesFromMinio(INPUT_BUCKET, job.getInputPath());
        log.info("Read {} lines from input: {}", lines.size(), job.getInputPath());

        int numMappers = job.getNumMappers();
        int totalLines = lines.size();
        // we should have 1 line per split (at least)
        int linesPerSplit = Math.max(1, (int) Math.ceil((double) totalLines / numMappers));

        List<SplitInfo> splits = new ArrayList<>();
        for(int i = 0; i < numMappers; i++) {
            int start = i * linesPerSplit;
            if(start >= totalLines) {
                //no more lines, fewer splits than requested mappers
                log.info("No more lines for mapper {}, stopped at split {}", i, splits.size());
                job.setNumMappers(splits.size()); // update job with actual num of mappers
                break;
            }
            int end = Math.min(start + linesPerSplit, totalLines);
            List<String> chunk = lines.subList(start, end);
            
            // upload split to minio
            String splitKey = "splits/" + job.getId() + "/split_" + i + ".txt";
            uploadSplit(INPUT_BUCKET, splitKey, chunk);
            log.info("Uploaded split {} with {} lines to {}/{}", i, chunk.size(), INPUT_BUCKET, splitKey);

            //build split info
            SplitInfo split = new SplitInfo();
            split.setPartitionIndex(i);
            split.setInputPath(splitKey); //object key
            split.setStartOffset(start);
            split.setEndOffset(end);
            splits.add(split);

            //create map task
            createMapperTask(job, split);
        }

        log.info("Created {} map tasks for job {}", splits.size(), job.getId());
        return splits;
    }

    private List<String> readLinesFromMinio(String bucket, String objectKey) {
        // implement logic to read lines from minio
        try (var stream = minioClient.getObject(GetObjectArgs.builder().bucket(bucket).object(objectKey).build());
             var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                return reader.lines().toList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to read input from minio:" + bucket + "/" + objectKey, e);
        }
    }

    private void uploadSplit(String bucket, String objectKey, List<String> lines) {
        // upload the split as a text file to minio
        String content = String.join("\n", lines);
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        try (var stream = new ByteArrayInputStream(bytes)) {
            minioClient.putObject(
                PutObjectArgs.builder()
                .bucket(bucket)
                .object(objectKey)
                .stream(stream, bytes.length, -1)
                .contentType("text/plain")
                .build()
                );
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload split to Minio: " + bucket + "/" + objectKey, e);
        }
    }

    // helper method to create a mapper task for a given split
    private void createMapperTask(Job job, SplitInfo split) {
        Task task = new Task();
        task.setJobId(job.getId());
        task.setType(TaskType.MAP);
        task.setStatus(TaskStatus.PENDING);
        task.setPartitionIndex(split.getPartitionIndex());
        task.setInputPath(split.getInputPath());
        task.setOutputLocation("intermediate/" + job.getId() + "/map_" + split.getPartitionIndex());
        task.setRetryCount(0);
        taskRepository.save(task);

        log.debug("Created map task {} for partition {}", task.getId(), split.getPartitionIndex());
    }

    // simple dto to represent input split information 
    public static class SplitInfo {
        private int partitionIndex;
        private String inputPath;
        private long startOffset;
        private long endOffset;

        public int getPartitionIndex() {
            return partitionIndex;
        }

        public void setPartitionIndex(int partitionIndex) {
            this.partitionIndex = partitionIndex;
        }

        public String getInputPath() {
            return inputPath;
        }

        public void setInputPath(String inputPath) {
            this.inputPath = inputPath;
        }

        public long getStartOffset() {
            return startOffset;
        }

        public void setStartOffset(long startOffset) {
            this.startOffset = startOffset;
        }

        public long getEndOffset() {
            return endOffset;
        }

        public void setEndOffset(long endOffset) {
            this.endOffset = endOffset;
        }
    } 

}
