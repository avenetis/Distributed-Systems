package com.tuc.distributed.worker.service;

import com.tuc.distributed.worker.api.TaskCompletionPayload;
import com.tuc.distributed.worker.api.TaskRequest;
import com.tuc.distributed.worker.api.TaskResponse;
import com.tuc.distributed.worker.client.ManagerClient;
import com.tuc.distributed.worker.domain.TaskStatus;
import com.tuc.distributed.worker.domain.TaskType;
import com.tuc.distributed.worker.mapreduce.KeyValue;
import com.tuc.distributed.worker.mapreduce.Mapper;
import com.tuc.distributed.worker.mapreduce.MapperRegistry;
import com.tuc.distributed.worker.mapreduce.Reducer;
import com.tuc.distributed.worker.mapreduce.ReducerRegistry;
import com.tuc.distributed.worker.minio.StorageClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class WorkerExecutionService {

    private static final Logger log = LoggerFactory.getLogger(WorkerExecutionService.class);

    private final StorageClient storageClient;
    private final MapperRegistry mapperRegistry;
    private final ReducerRegistry reducerRegistry;
    private final PartitionService partitionService;
    private final ManagerClient managerClient;
    private final String workerId;

    public WorkerExecutionService(StorageClient storageClient,
                                  MapperRegistry mapperRegistry,
                                  ReducerRegistry reducerRegistry,
                                  PartitionService partitionService,
                                  ManagerClient managerClient,
                                  @Value("${worker.id:${HOSTNAME:worker-local}}") String workerId) {
        this.storageClient = storageClient;
        this.mapperRegistry = mapperRegistry;
        this.reducerRegistry = reducerRegistry;
        this.partitionService = partitionService;
        this.managerClient = managerClient;
        this.workerId = workerId;
    }

    public TaskResponse execute(TaskRequest request) {
        log.info("Worker {} started task {} of type {}", workerId, request.taskId(), request.type());

        try {
            TaskResponse response = switch (request.type()) {
                case MAP -> executeMap(request);
                case REDUCE -> executeReduce(request);
            };

            notifyManager(request, response.status(), response.message(), response.outputObjectKeys());
            return response;
        } catch (Exception e) {
            log.error("Task {} failed", request.taskId(), e);
            notifyManager(request, TaskStatus.FAILED, e.getMessage(), List.of());
            return new TaskResponse(request.taskId(), TaskStatus.FAILED, e.getMessage(), List.of());
        }
    }

    private TaskResponse executeMap(TaskRequest request) {
        Mapper mapper = mapperRegistry.get(request.mapperClass());
        int reducersCount = request.reducersCount() == null ? 1 : request.reducersCount();
        Map<Integer, List<KeyValue>> partitions = new HashMap<>();
        for (int i = 0; i < reducersCount; i++) {
            partitions.put(i, new ArrayList<>());
        }

        for (String inputKey : request.inputObjectKeys()) {
            String content = storageClient.readText(request.inputBucket(), inputKey);
            String[] lines = content.split("\\R");
            for (String line : lines) {
                for (KeyValue pair : mapper.map(line)) {
                    int partition = partitionService.partitionOf(pair.key(), reducersCount);
                    partitions.get(partition).add(pair);
                }
            }
        }

        List<String> outputKeys = new ArrayList<>();
        for (Map.Entry<Integer, List<KeyValue>> entry : partitions.entrySet()) {
            String objectKey = "%s/%s/part-%d.txt".formatted(request.outputPrefix(), request.taskId(), entry.getKey());
            String payload = entry.getValue().stream()
                    .map(kv -> kv.key() + "\t" + kv.value())
                    .collect(Collectors.joining("\n"));
            storageClient.writeText(request.outputBucket(), objectKey, payload);
            outputKeys.add(objectKey);
        }

        return new TaskResponse(request.taskId(), TaskStatus.DONE, "Map task completed", outputKeys);
    }

    private TaskResponse executeReduce(TaskRequest request) {
        Reducer reducer = reducerRegistry.get(request.reducerClass());
        Map<String, List<String>> grouped = new HashMap<>();

        for (String inputKey : request.inputObjectKeys()) {
            String content = storageClient.readText(request.inputBucket(), inputKey);
            if (content.isBlank()) {
                continue;
            }
            String[] lines = content.split("\\R");
            for (String line : lines) {
                if (line.isBlank()) {
                    continue;
                }
                String[] parts = line.split("\\t", 2);
                if (parts.length != 2) {
                    throw new IllegalArgumentException("Invalid intermediate record: " + line);
                }
                grouped.computeIfAbsent(parts[0], ignored -> new ArrayList<>()).add(parts[1]);
            }
        }

        Map<String, List<String>> sorted = grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        StringBuilder finalOutput = new StringBuilder();
        for (Map.Entry<String, List<String>> entry : sorted.entrySet()) {
            finalOutput.append(reducer.reduce(entry.getKey(), entry.getValue())).append('\n');
        }

        int partition = request.reducePartition() == null ? 0 : request.reducePartition();
        String objectKey = "%s/reduce-%d.txt".formatted(request.outputPrefix(), partition);
        storageClient.writeText(request.outputBucket(), objectKey, finalOutput.toString());
        return new TaskResponse(request.taskId(), TaskStatus.DONE, "Reduce task completed", List.of(objectKey));
    }

    private void notifyManager(TaskRequest request, TaskStatus status, String details, List<String> outputKeys) {
        managerClient.sendCompletion(
                request.managerCallbackUrl(),
                new TaskCompletionPayload(
                        request.taskId(),
                        request.jobId(),
                        status,
                        workerId,
                        details,
                        outputKeys
                )
        );
    }
}
