package com.mapreduce.manager.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mapreduce.manager.dto.ClaimTaskResponse;
import com.mapreduce.manager.dto.HeartbeatRequest;
import com.mapreduce.manager.dto.TaskCallbackPayload;
import com.mapreduce.manager.dto.WorkerRegistrationRequest;
import com.mapreduce.manager.dto.WorkerRegistrationResponse;
import com.mapreduce.manager.entity.Job;
import com.mapreduce.manager.entity.JobStatus;
import com.mapreduce.manager.entity.Task;
import com.mapreduce.manager.entity.TaskStatus;
import com.mapreduce.manager.entity.Worker;
import com.mapreduce.manager.entity.WorkerStatus;
import com.mapreduce.manager.repository.JobRepository;
import com.mapreduce.manager.repository.TaskRepository;
import com.mapreduce.manager.repository.TaskType;
import com.mapreduce.manager.repository.WorkerRepository;

@Service
public class WorkerService {
    private static final Logger log = LoggerFactory.getLogger(WorkerService.class);

    private final WorkerRepository workerRepository;
    private final TaskRepository taskRepository;
    private final JobRepository jobRepository;
    private final ShuffleService shuffleService;

    @Value("${manager.callback.url:http://manager-service.mapreduce-system.svc.cluster.local:8080/internal/v1/callbacks/task-complete}")
    private String managerCallbackUrl;

    public WorkerService(
            JobRepository jobRepository,
            TaskRepository taskRepository,
            WorkerRepository workerRepository,
            ShuffleService shuffleService
    ) {
        this.jobRepository = jobRepository;
        this.taskRepository = taskRepository;
        this.workerRepository = workerRepository;
        this.shuffleService = shuffleService;
    }

    @Transactional
    public WorkerRegistrationResponse registerWorker(WorkerRegistrationRequest request) {
        log.info("Registering worker: {}", request.getWorkerId());

        Optional<Worker> existingWorker = workerRepository.findById(request.getWorkerId());

        Worker worker;
        if (existingWorker.isPresent()) {
            worker = existingWorker.get();
            worker.setStatus(WorkerStatus.ACTIVE);
            worker.setLastHeartbeat(LocalDateTime.now());
            worker.setAddress(request.getAddress());
            log.info("Re-registered existing worker: {}", request.getWorkerId());
        } else {
            worker = new Worker();
            worker.setId(request.getWorkerId());
            worker.setAddress(request.getAddress());
            worker.setStatus(WorkerStatus.ACTIVE);
            worker = workerRepository.save(worker);
            log.info("New worker registered: {}", request.getWorkerId());
        }

        return new WorkerRegistrationResponse(
                worker.getId(),
                worker.getRegisteredAt(),
                worker.getStatus(),
                "Worker registered successfully"
        );
    }

    @Transactional
    public void updateHeartbeat(HeartbeatRequest request) {
        log.debug("Heartbeat from worker: {}", request.getWorkerId());

        Optional<Worker> workerOpt = workerRepository.findById(request.getWorkerId());
        if (workerOpt.isEmpty()) {
            log.warn("Heartbeat from unknown worker: {}", request.getWorkerId());
            return;
        }

        workerRepository.updateHeartbeat(request.getWorkerId(), request.getStatus());
    }

    @Transactional
    public ClaimTaskResponse claimTask(String workerId) {
        log.debug("Worker {} requesting task", workerId);

        Optional<Worker> workerOpt = workerRepository.findById(workerId);
        if (workerOpt.isEmpty()) {
            log.warn("Unknown worker trying to claim task: {}", workerId);
            return new ClaimTaskResponse(false);
        }

        Worker worker = workerOpt.get();

        Optional<Task> taskOpt = claimFirstAvailableTask(TaskType.MAP);
        if (taskOpt.isEmpty()) {
            taskOpt = claimFirstAvailableTask(TaskType.REDUCE);
            if (taskOpt.isPresent()) {
                log.info("No MAP tasks. Assigning REDUCE task to worker {}", workerId);
            }
        }

        if (taskOpt.isEmpty()) {
            log.debug("No pending tasks available for worker: {}", workerId);
            return new ClaimTaskResponse(false);
        }

        Task task = taskOpt.get();
        Job job = jobRepository.findById(task.getJobId()).orElse(null);

        if (job == null) {
            log.warn("Job not found for task: {}", task.getId());
            task.setStatus(TaskStatus.PENDING);
            task.setWorkerId(null);
            task.setAssignedAt(null);
            taskRepository.save(task);
            return new ClaimTaskResponse(false);
        }

        task.setStatus(TaskStatus.ASSIGNED);
        task.setWorkerId(workerId);
        task.setAssignedAt(LocalDateTime.now());
        taskRepository.save(task);

        worker.setStatus(WorkerStatus.BUSY);
        worker.setCurrentTaskId(task.getId());
        worker.setLastHeartbeat(LocalDateTime.now());
        workerRepository.save(worker);

        log.info("Assigned {} task {} to worker {}", task.getType(), task.getId(), workerId);

        if (task.getType() == TaskType.MAP) {
            return buildMapTaskResponse(task, job);
        }

        return buildReduceTaskResponse(task, job);
    }

    private Optional<Task> claimFirstAvailableTask(TaskType taskType) {
        List<Task> pendingTasks = taskRepository.findPendingTasksByTypeForUpdate(
                TaskStatus.PENDING,
                taskType
        );

        if (pendingTasks.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(pendingTasks.get(0));
    }

    private ClaimTaskResponse buildMapTaskResponse(Task task, Job job) {
        return new ClaimTaskResponse(
                task.getId(),
                task.getJobId(),
                TaskType.MAP,
                "mapreduce-input",
                List.of(task.getInputPath()),
                "mapreduce-intermediate",
                "intermediate/" + task.getJobId(),
                job.getMapperCodePath() != null ? job.getMapperCodePath() : "WordCountMapper",
                "WordCountReducer",
                job.getNumReducers(),
                managerCallbackUrl
        );
    }

    private ClaimTaskResponse buildReduceTaskResponse(Task task, Job job) {
        List<String> inputKeys = List.of(task.getInputPath().split(","));

        return new ClaimTaskResponse(
                task.getId(),
                task.getJobId(),
                TaskType.REDUCE,
                "mapreduce-intermediate",
                inputKeys,
                "mapreduce-output",
                "output/" + task.getJobId() + "/part-" + task.getPartitionIndex(),
                "WordCountReducer",
                task.getPartitionIndex(),
                managerCallbackUrl
        );
    }

    @Transactional
    public void onTaskCompleted(TaskCallbackPayload payload) {
        log.info("Task completed callback: taskId={}, status={}", payload.getTaskId(), payload.getStatus());

        Optional<Task> taskOpt = taskRepository.findById(payload.getTaskId());
        if (taskOpt.isEmpty()) {
            log.warn("Task not found: {}", payload.getTaskId());
            return;
        }

        Task task = taskOpt.get();
        String assignedWorkerId = task.getWorkerId();

        task.setStatus(TaskStatus.COMPLETED);
        task.setCompletedAt(LocalDateTime.now());

        if (payload.getOutputObjectKeys() != null && !payload.getOutputObjectKeys().isEmpty()) {
            task.setOutputLocation(String.join(",", payload.getOutputObjectKeys()));
        }

        taskRepository.save(task);
        freeWorker(assignedWorkerId);

        if (task.getType() == TaskType.MAP) {
            handleMapTaskCompletion(task.getJobId());
        } else if (task.getType() == TaskType.REDUCE) {
            handleReduceTaskCompletion(task.getJobId());
        }
    }

    @Transactional
    public void onTaskFailed(TaskCallbackPayload payload) {
        log.error("Task failed callback: taskId={}, error={}", payload.getTaskId(), payload.getDetails());

        Optional<Task> taskOpt = taskRepository.findById(payload.getTaskId());
        if (taskOpt.isEmpty()) {
            log.warn("Task not found: {}", payload.getTaskId());
            return;
        }

        handleTaskFailure(taskOpt.get(), payload.getDetails());
    }

    private void handleMapTaskCompletion(String jobId) {
        Job job = jobRepository.findById(jobId).orElse(null);
        if (job == null) {
            return;
        }

        long completedMappers = taskRepository.countByJobIdAndTypeAndStatus(
                jobId,
                TaskType.MAP,
                TaskStatus.COMPLETED
        );

        job.setCompletedMappers((int) completedMappers);
        jobRepository.save(job);

        log.info("Job {} map progress: {}/{}", jobId, completedMappers, job.getNumMappers());

        if (completedMappers >= job.getNumMappers() && job.getStatus() == JobStatus.MAP_PHASE) {
            log.info("All map tasks completed for job {}, starting shuffle and reduce", jobId);
            job.setStatus(JobStatus.SHUFFLE_PHASE);
            jobRepository.save(job);
            shuffleService.initShuffleAndReduce(jobId);
        }
    }

    private void handleReduceTaskCompletion(String jobId) {
        Job job = jobRepository.findById(jobId).orElse(null);
        if (job == null) {
            return;
        }

        long completedReducers = taskRepository.countByJobIdAndTypeAndStatus(
                jobId,
                TaskType.REDUCE,
                TaskStatus.COMPLETED
        );

        job.setCompletedReducers((int) completedReducers);
        jobRepository.save(job);

        log.info("Job {} reduce progress: {}/{}", jobId, completedReducers, job.getNumReducers());

        if (completedReducers >= job.getNumReducers()) {
            job.setStatus(JobStatus.COMPLETED);
            job.setCompletedAt(LocalDateTime.now());
            jobRepository.save(job);
            log.info("Job {} completed successfully", jobId);
        }
    }

    private void handleTaskFailure(Task task, String errorMessage) {
        int maxRetries = 3;
        String assignedWorkerId = task.getWorkerId();

        if (task.getRetryCount() >= maxRetries) {
            log.error("Task {} failed after {} retries", task.getId(), maxRetries);

            Job job = jobRepository.findById(task.getJobId()).orElse(null);
            if (job != null) {
                job.setStatus(JobStatus.FAILED);
                job.setErrorMessage("Task failed: " + errorMessage);
                jobRepository.save(job);
            }

            task.setStatus(TaskStatus.FAILED);
            task.setErrorMessage(errorMessage);
            taskRepository.save(task);
        } else {
            task.setStatus(TaskStatus.PENDING);
            task.setWorkerId(null);
            task.setAssignedAt(null);
            task.setStartedAt(null);
            task.setErrorMessage(errorMessage);
            task.setRetryCount(task.getRetryCount() + 1);
            taskRepository.save(task);

            log.info("Task {} queued for retry (attempt {})", task.getId(), task.getRetryCount());
        }

        freeWorker(assignedWorkerId);
    }

    private void freeWorker(String workerId) {
        if (workerId == null) {
            return;
        }

        Optional<Worker> workerOpt = workerRepository.findById(workerId);
        if (workerOpt.isPresent()) {
            Worker worker = workerOpt.get();
            worker.setStatus(WorkerStatus.ACTIVE);
            worker.setCurrentTaskId(null);
            worker.setTasksCompleted(worker.getTasksCompleted() + 1);
            workerRepository.save(worker);
            log.debug("Worker {} is freed", workerId);
        }
    }

    @Transactional
    public void recoverStaleWorkers() {
        LocalDateTime timeout = LocalDateTime.now().minusSeconds(60);

        List<Worker> staleWorkers = workerRepository.findStaleWorkers(
                List.of(WorkerStatus.ACTIVE, WorkerStatus.BUSY),
                timeout
        );

        for (Worker worker : staleWorkers) {
            log.warn("Worker {} is stale. Last heartbeat was: {}", worker.getId(), worker.getLastHeartbeat());

            worker.setStatus(WorkerStatus.DEAD);
            workerRepository.save(worker);

            if (worker.getCurrentTaskId() != null) {
                reassignTask(worker.getCurrentTaskId());
            }
        }
    }

    @Transactional
    public void reassignTask(String taskId) {
        Optional<Task> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isEmpty()) {
            return;
        }

        Task task = taskOpt.get();

        if (task.getStatus() == TaskStatus.ASSIGNED || task.getStatus() == TaskStatus.RUNNING) {
            log.info("Reassigning task {}", taskId);

            task.setStatus(TaskStatus.PENDING);
            task.setWorkerId(null);
            task.setAssignedAt(null);
            task.setErrorMessage("Reassigned due to worker failure");
            taskRepository.save(task);
        }
    }

    @Scheduled(fixedDelay = 30000)
    public void scheduledStaleWorkerRecovery() {
        recoverStaleWorkers();
    }

    public List<Worker> getActivWorkers() {
        return workerRepository.findByStatus(WorkerStatus.ACTIVE);
    }
}