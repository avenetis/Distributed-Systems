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

    @Value("${manager.callback.url:http://localhost:8081/internal/v1/callbacks/task-completion}")
    private String managerCallbackUrl;

    public WorkerService(JobRepository jobRepository, TaskRepository taskRepository, WorkerRepository workerRepository, ShuffleService shuffleService) {
        this.jobRepository = jobRepository;
        this.taskRepository = taskRepository;
        this.workerRepository = workerRepository;
        this.shuffleService = shuffleService;
    }

    //worker registration and heartbeat
    @Transactional
    public WorkerRegistrationResponse registerWorker(WorkerRegistrationRequest request) {
        log.info("Registering worker: {}", request.getWorkerId());
        Optional<Worker> existingWorker = workerRepository.findById(request.getWorkerId());
        
        Worker worker;
        if(existingWorker.isPresent()) {
            worker = existingWorker.get();
            worker.setStatus(WorkerStatus.ACTIVE);
            worker.setLastHeartbeat(LocalDateTime.now());
            worker.setAddress(request.getAddress());
            log.info("Re-registered exinsting worker: {}", request.getWorkerId());
        } else {
            worker = new Worker();
            worker.setId(request.getWorkerId());
            worker.setAddress(request.getAddress());
            worker.setStatus(WorkerStatus.ACTIVE);
            worker = workerRepository.save(worker);
            log.info("New worker registered: {}", request.getWorkerId());
        }

        return new WorkerRegistrationResponse(worker.getId(), worker.getRegisteredAt(), worker.getStatus(), "Worker registered successfully");
    }

    @Transactional
    public void updateHeartbeat(HeartbeatRequest request) {
        log.debug("Heartbeat from worker: {}", request.getWorkerId());
        Optional<Worker> workOpt = workerRepository.findById(request.getWorkerId());
        if(workOpt.isEmpty()) {
            log.warn("Heartbeat from unknown worker: {}", request.getWorkerId());
            return;
        }
        workerRepository.updateHeartbeat(request.getWorkerId(), request.getStatus());
    }

    //task assign
    @Transactional
    public ClaimTaskResponse claimTask(String workerId) {
        log.debug("Worker {} requesting task", workerId);

        Optional<Worker> workOpt = workerRepository.findById(workerId);
        if(workOpt.isEmpty()) {
            log.warn("Unknown worker trying to claim task: {}", workerId);
            return new ClaimTaskResponse(false);
        }

        Worker worker = workOpt.get();
        //map tasks
        List<Task> pendingTasks = taskRepository.findPendindTasksByType(TaskStatus.PENDING, TaskType.MAP);
        //if we dint have map, try reduce
        if(pendingTasks.isEmpty()) {
            pendingTasks = taskRepository.findPendindTasksByType(TaskStatus.PENDING, TaskType.REDUCE);
            if(!pendingTasks.isEmpty()) {
                log.info("No MAP tasks. Assigne REDUCE tasks to worker {}", workerId);
            }
        }

        if(pendingTasks.isEmpty()) {
            log.debug("No pending tasks available for worker: {}", workerId);
            return new ClaimTaskResponse(false);
        }
        Task task = pendingTasks.get(0);
        Job job = jobRepository.findById(task.getJobId()).orElse(null);

        if(job == null) {
            log.warn("Job not found for task: {}", task.getId());
            return new ClaimTaskResponse(false);
        }

        //assign task to the worker
        task.setStatus(TaskStatus.ASSIGNED);
        task.setWorkerId(workerId);
        task.setAssignedAt(LocalDateTime.now());
        taskRepository.save(task);

        //update worker status
        worker.setStatus(WorkerStatus.BUSY);
        worker.setCurrentTaskId(task.getId());
        worker.setLastHeartbeat(LocalDateTime.now());
        workerRepository.save(worker);
        
        log.info("Assigned {} task {} to worker {}", task.getType(), task.getId(), workerId);

        //build response based on task type
        if(task.getType() == TaskType.MAP) {
            return buildMapTaskResponse(task, job);
        } else {
            return buildReduceTaskResponse(task, job);
        }
    }

    private ClaimTaskResponse buildMapTaskResponse(Task task, Job job) {
        //parse inout
        String inputBucket = extractBucket(task.getInputPath());
        String inputKeys = extractKey(task.getInputPath());

        return new ClaimTaskResponse(task.getId(), task.getJobId(), TaskType.MAP, inputBucket, inputKeys, "mapreduce-output", "intermediate/" + task.getJobId(), job.getMapperCodePath() != null ? job.getMapperCodePath() : "WordCountMapper", "WordCountReducer", job.getNumReducers(), managerCallbackUrl);
    }

    private ClaimTaskResponse buildReduceTaskResponse(Task task, Job job) {
        //parse inout
        String inputBucket = "mapreduce-intermediate";
        String inputKeys = task.getInputPath();

        return new ClaimTaskResponse(task.getId(), task.getJobId(), TaskType.REDUCE, inputBucket, inputKeys, job.getOutputPath(), "final/" + task.getJobId(), "WordCountReducer", task.getPartitionIndex(), managerCallbackUrl);
    }

    private String extractBucket(String path) {
        //extraction
        if (path == null) return "default";
        if (path.startsWith("s3://")) {
            String noProtocol = path.substring(5);
            int slashIdx = noProtocol.indexOf('/');
            return slashIdx > 0 ? noProtocol.substring(0, slashIdx) : noProtocol;
        }
        return "default";
    }

    private String extractKey(String path) {
        //extraction
        if (path == null) return "";
        if (path.startsWith("s3://")) {
            String noProtocol = path.substring(5);
            int slashIdx = noProtocol.indexOf('/');
            return slashIdx > 0 ? noProtocol.substring(slashIdx + 1) : "";
        }
        return path;
    }

    //handle tasks that completed
    @Transactional
    public void onTaskCompleted(TaskCallbackPayload payload) {
        log.info("Task completed callback: taskId={}, status={}", payload.getTaskId(), payload.getStatus());

        Optional<Task> taskOpt = taskRepository.findById(payload.getTaskId());
        if(taskOpt.isEmpty()) {
            log.warn("Task not found: {}", payload.getTaskId());
            return;
        }

        Task task = taskOpt.get();
        task.setStatus(TaskStatus.COMPLETED);
        task.setCompletedAt(LocalDateTime.now());
        if(payload.getOutputObjectKeys() != null && !payload.getOutputObjectKeys().isEmpty()) {
            task.setOutputLocation(String.join(",", payload.getOutputObjectKeys()));
        }
        taskRepository.save(task);

        //free worker
        freeWorker(task.getWorkerId());

        //updaate job progress basef on task type
        if(task.getType() == TaskType.MAP) {
            handleMapTaskCompletion(task.getJobId());
        } else if (task.getType() == TaskType.REDUCE) {
            handleReduceTaskCompletion(task.getJobId());
        } 
    }

    @Transactional
    public void onTaskFailed(TaskCallbackPayload payload) {
        log.error("Task failed callback: taskId={}, error={}", payload.getTaskId(), payload.getDetails());

        Optional<Task> taskOptional = taskRepository.findById(payload.getTaskId());
        if(taskOptional.isEmpty()) {
            log.warn("Task not found: {}", payload.getTaskId());
            return;
        }

        Task task = taskOptional.get();
        handleTaskFailure(task, payload.getDetails());
    }

    private void handleMapTaskCompletion(String jobId) {
        Job job = jobRepository.findById(jobId).orElse(null);
        if(job == null) return;

        long completedMappers = taskRepository.countByJobIdAndTypeAndStatus(jobId, TaskType.MAP, TaskStatus.COMPLETED);
        job.setCompletedMappers((int) completedMappers);
        jobRepository.save(job);

        log.info("Job {} map progress: {}/{}", jobId, completedMappers, job.getNumMappers());

        //Check if all mappers done and go to shuffle and create reduce tasks
        if(completedMappers >= job.getNumMappers() && job.getStatus() == JobStatus.MAP_PHASE) {
            log.info("All map tasks completed for job {}, starting shuffle and reduce", jobId);
            job.setStatus(JobStatus.SHUFFLE_PHASE);
            jobRepository.save(job);
            shuffleService.initShuffleAndReduce(jobId);
        }
    }

    private void handleReduceTaskCompletion(String jobId) {
        Job job = jobRepository.findById(jobId).orElse(null);
        if(job == null) return;

        long completedReducers = taskRepository.countByJobIdAndTypeAndStatus(jobId, TaskType.REDUCE, TaskStatus.COMPLETED);
        job.setCompletedMappers((int) completedReducers);
        jobRepository.save(job);

        log.info("Job {} reduce progress: {}/{}", jobId, completedReducers, job.getNumReducers());

        //Check if all mappers done and go to shuffle and create reduce tasks
        if(completedReducers >= job.getNumReducers()) {
            job.setStatus(JobStatus.COMPLETED);
            job.setCompletedAt(LocalDateTime.now());
            jobRepository.save(job);
            log.info("Job {} completed successfully", jobId);
        }
    }

    private void handleTaskFailure(Task task, String errorMessage) {
        int maxRetries = 3;

        if(task.getRetryCount() >= maxRetries) {
            log.error("Task {} failed after {} retries", task.getId(), maxRetries);

            Job job = jobRepository.findById(task.getJobId()).orElse(null);
            if(job != null) {
                job.setStatus(JobStatus.FAILED);
                job.setErrorMessage("Task failed:" + errorMessage);

                jobRepository.save(job);
            }

            task.setStatus(TaskStatus.FAILED);
            task.setErrorMessage(errorMessage);
            taskRepository.save(task);
        } else {
            // reset tasl
            task.setStatus(TaskStatus.PENDING);
            task.setWorkerId(null);
            task.setAssignedAt(null);
            task.setStartedAt(null);
            task.setErrorMessage(errorMessage);
            task.setRetryCount(task.getRetryCount() + 1);
            taskRepository.save(task);
            log.info("Task {} queued for retry (attempt {})", task.getId(), task.getRetryCount());
        }

        //free worker if its assigned
        if(task.getWorkerId() != null) {
            freeWorker(task.getWorkerId());
        }
    }

    private void freeWorker(String workerId) {
        Optional<Worker> workerOpt = workerRepository.findById(workerId);
        if(workerOpt.isPresent()) {
            Worker worker = workerOpt.get();
            worker.setStatus(WorkerStatus.ACTIVE);
            worker.setCurrentTaskId(null);
            worker.setTasksCompleted(worker.getTasksCompleted() + 1);
            workerRepository.save(worker);
            log.debug("Worker {} is freed", workerId);
        }
    }

    //Recover stale workers
    @Transactional
    public void recoverStaleWorkers() {
        LocalDateTime timeout = LocalDateTime.now().minusSeconds(60);
        List<Worker> staleWorkers = workerRepository.findStaleWorkers(List.of(WorkerStatus.ACTIVE, WorkerStatus.BUSY), timeout);

        for(Worker w : staleWorkers) {
            log.warn("Worker {} is stale. Last heartbeat was: {}", w.getId(), w.getLastHeartbeat());

            w.setStatus(WorkerStatus.DEAD);
            workerRepository.save(w);

            //ckeck if the worker has some unfinished tasks and reassing them
            if (w.getCurrentTaskId() != null) {
                reassignTask(w.getCurrentTaskId());
            }
        }
    }

    @Transactional
    public void reassignTask(String taskId) {
        Optional<Task> taskOptional = taskRepository.findById(taskId);
        if(taskOptional.isEmpty()) return;

        Task task = taskOptional.get();
        if(task.getStatus() == TaskStatus.ASSIGNED || task.getStatus() == TaskStatus.RUNNING) {
            log.info("Reassigning task {}", taskId);
            task.setStatus(TaskStatus.PENDING);
            task.setWorkerId(null);
            task.setAssignedAt(null);
            task.setErrorMessage("Reassigned due to woeker failure");
            taskRepository.save(task);
        }
    }

    //add scheduled recovery every 30s
    @Scheduled(fixedDelay=30000)
    public void scheduledStaleWorkerRecovery() {
        recoverStaleWorkers();
    }

    public List<Worker> getActivWorkers() {
        return workerRepository.findByStatus(WorkerStatus.ACTIVE);
    }


}
