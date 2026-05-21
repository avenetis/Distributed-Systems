package com.tuc.distributed.worker.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.tuc.distributed.worker.api.TaskRequest;
import com.tuc.distributed.worker.domain.TaskType;

@Service
public class WorkerPollingService {
    private static Logger logger = LoggerFactory.getLogger(WorkerPollingService.class);

    private final WorkerExecutionService workerExecutionService;
    private final RestClient restClient = RestClient.builder().build();

    private final AtomicBoolean busy = new AtomicBoolean(false);

    @Value("${worker.id:${HOSTNAME:worker-local}}")
    private String workerId;

    @Value("${manager.url:http://manager-service.mapreduce-system.svc.cluster.local:8080}")
    private String managerUrl;

    @Value("${worker.address:http://localhost:8083}")
    private String workerAddress;

    @Value("${worker.auth-token:}")
    private String workerAuthToken;

    public WorkerPollingService(WorkerExecutionService workerExecutionService) {
        this.workerExecutionService = workerExecutionService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void registerWithManager() {
        logger.info("Registering worker {} with manager at {}", workerId, managerUrl);
        try {
            restClient.post()
                    .uri(managerUrl + "/internal/v1/workers/register")
                    .header("X-Worker-Token", workerAuthToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("workerId", workerId, "address", workerAddress))
                    .retrieve()
                    .toBodilessEntity();

            logger.info("Worker {} registered successfully", workerId);
        } catch (Exception e) {
            logger.error("Failed to register worker {} with manager: {}", workerId, e.getMessage());
        }
    }

    @Scheduled(fixedDelay = 5000)
    public void pollForTasks() {
        if (busy.get()) {
            logger.debug("Worker {} is busy, skipping poll", workerId);
            return;
        }

        try {
            var resp = restClient.get()
                    .uri(managerUrl + "/internal/v1/workers/{workerId}/claim-task", workerId)
                    .header("X-Worker-Token", workerAuthToken)
                    .retrieve()
                    .toEntity(ClaimTaskResponse.class);

            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                ClaimTaskResponse claimedTask = resp.getBody();
                if (claimedTask.isHasWork() && busy.compareAndSet(false, true)) {
                    logger.info("Worker {} claimed task {}", workerId, claimedTask.getTaskId());
                    executeTask(claimedTask);
                }
            }
        } catch (Exception e) {
            if (e.getMessage() == null || !e.getMessage().contains("204")) {
                logger.debug("Poll cycle, no tasks or error: {}", e.getMessage());
            }
        }
    }

    @Scheduled(fixedDelay = 10000)
    public void sendHeartbeat() {
        try {
            restClient.post()
                    .uri(managerUrl + "/internal/v1/workers/heartbeat")
                    .header("X-Worker-Token", workerAuthToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("workerId", workerId, "status", "ACTIVE"))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            logger.warn("Heartbeat failed for worker {}: {}", workerId, e.getMessage());
        }
    }

    private void executeTask(ClaimTaskResponse claimedTask) {
        TaskRequest request = new TaskRequest(
                claimedTask.getTaskId(),
                claimedTask.getJobId(),
                claimedTask.getTaskType().name().equals("MAP")
                        ? com.tuc.distributed.worker.domain.TaskType.MAP
                        : com.tuc.distributed.worker.domain.TaskType.REDUCE,
                claimedTask.getInputBucket(),
                claimedTask.getInputObjectKeys(),
                claimedTask.getOutputBucket(),
                claimedTask.getOutputPrefix(),
                claimedTask.getMapperClass() != null ? claimedTask.getMapperClass() : "WordCountMapper",
                claimedTask.getReducerClass() != null ? claimedTask.getReducerClass() : "WordCountReducer",
                claimedTask.getReducersCount(),
                claimedTask.getReducePartition(),
                claimedTask.getManagerCallbackUrl()
        );

        new Thread(() -> {
            try {
                workerExecutionService.execute(request);
            } finally {
                busy.set(false);
            }
        }).start();
    }

    public static class ClaimTaskResponse {
        private boolean hasWork;
        private String taskId;
        private String jobId;
        private TaskType taskType;
        private String inputBucket;
        private List<String> inputObjectKeys;
        private String outputBucket;
        private String outputPrefix;
        private String mapperClass;
        private String reducerClass;
        private Integer reducersCount;
        private Integer reducePartition;
        private String managerCallbackUrl;

        public boolean isHasWork() {
            return hasWork;
        }

        public void setHasWork(boolean hasWork) {
            this.hasWork = hasWork;
        }

        public String getTaskId() {
            return taskId;
        }

        public void setTaskId(String taskId) {
            this.taskId = taskId;
        }

        public String getJobId() {
            return jobId;
        }

        public void setJobId(String jobId) {
            this.jobId = jobId;
        }

        public TaskType getTaskType() {
            return taskType;
        }

        public void setTaskType(TaskType taskType) {
            this.taskType = taskType;
        }

        public String getInputBucket() {
            return inputBucket;
        }

        public void setInputBucket(String inputBucket) {
            this.inputBucket = inputBucket;
        }

        public List<String> getInputObjectKeys() {
            return inputObjectKeys;
        }

        public void setInputObjectKeys(List<String> inputObjectKeys) {
            this.inputObjectKeys = inputObjectKeys;
        }

        public String getOutputBucket() {
            return outputBucket;
        }

        public void setOutputBucket(String outputBucket) {
            this.outputBucket = outputBucket;
        }

        public String getOutputPrefix() {
            return outputPrefix;
        }

        public void setOutputPrefix(String outputPrefix) {
            this.outputPrefix = outputPrefix;
        }

        public String getMapperClass() {
            return mapperClass;
        }

        public void setMapperClass(String mapperClass) {
            this.mapperClass = mapperClass;
        }

        public String getReducerClass() {
            return reducerClass;
        }

        public void setReducerClass(String reducerClass) {
            this.reducerClass = reducerClass;
        }

        public Integer getReducersCount() {
            return reducersCount;
        }

        public void setReducersCount(Integer reducersCount) {
            this.reducersCount = reducersCount;
        }

        public Integer getReducePartition() {
            return reducePartition;
        }

        public void setReducePartition(Integer reducePartition) {
            this.reducePartition = reducePartition;
        }

        public String getManagerCallbackUrl() {
            return managerCallbackUrl;
        }

        public void setManagerCallbackUrl(String managerCallbackUrl) {
            this.managerCallbackUrl = managerCallbackUrl;
        }
    }
}