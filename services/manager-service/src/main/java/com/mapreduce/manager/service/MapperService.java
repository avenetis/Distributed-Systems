package com.mapreduce.manager.service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mapreduce.manager.entity.Job;
import com.mapreduce.manager.entity.JobStatus;
import com.mapreduce.manager.entity.Task;
import com.mapreduce.manager.entity.TaskStatus;
import com.mapreduce.manager.repository.JobRepository;
import com.mapreduce.manager.repository.TaskRepository;
import com.mapreduce.manager.repository.TaskType;

@Service
public class MapperService {
    // This service would handle mapper related logic, such as preparing mapper tasks,
    // managing mapper code, and tracking mapper execution status.
    private static final Logger log = LoggerFactory.getLogger(MapperService.class);
    private final JobRepository jobRepository;
    private final TaskRepository taskRepository;

    public MapperService(JobRepository jobRepository, TaskRepository taskRepository) {
        this.jobRepository = jobRepository;
        this.taskRepository = taskRepository;
    }

    @Transactional
    public void startMapping(Job job, List<InputtSplitterService.SplitInfo> splits) {
        // for each split, create a mapper task and dave to repo
        log.info("Starting MAP phase for job: {}", job.getId());
        job.setStatus(JobStatus.MAP_PHASE); // update job status to indicate map phase has started
        jobRepository.save(job); // save status update

        log.info("MAP phase started for job: {}. {} map tasks pending", job.getId(), job.getNumMappers()); //
    }
    
    @Transactional
    public void onMapperTaskCompleted(String taskId) {
        // called when a mapper task completes, update task status and check if all mappers are done
        // update task status in repository
        // check if all mapper tasks for the job are completed
        // if all mappers are done, trigger reduce phase
        Task task = taskRepository.findById(taskId).orElseThrow(() -> new RuntimeException("Task not found: " + taskId));

        if(task.getType() != TaskType.MAP) {
            return; // ignore if not a map task
        }

        log.info("Mapper task completed: {}", taskId);
        
        Job job = jobRepository.findById(task.getJobId()).orElse(null);
        if(job == null || job.getStatus() != JobStatus.MAP_PHASE) {
            return; // job not found or not in map phase, ignore
        }

        // increment completed mappers count
        int completedMappers = job.getCompletedMappers() + 1;
        job.setCompletedMappers(completedMappers);
        jobRepository.save(job);
        
        log.info("Job {}: {}/{} mappers completed", job.getId(), completedMappers, job.getNumMappers());

        //check if all mappers are completed
        if(completedMappers >= job.getNumMappers()) {
            completeMapPhase(job); 
        }
    }

    @Transactional
    public void completeMapPhase(Job job) {
        log.info("MAP phase completes for job: {}", job.getId());
        job.setStatus(JobStatus.SHUFFLE_PHASE); // update job status to indicate shuffle phase has started
        jobRepository.save(job); // save status update

        log.info("Job {} transition to SHUFFLE phase", job.getId());
    }

    @Transactional
    public void onMapTaskFailed(String taskId, String errorMessage) {
        // called when a mapper task fails, update task status and mark job as failed
        Task task = taskRepository.findById(taskId).orElseThrow(() -> new RuntimeException("Task not found: " + taskId));

        log.error("Mapper task failed: {}. Error: {}", taskId, errorMessage);
        
        // check retry count
        if(task.getRetryCount() >= 3) {
            // mark task as failed
            Job job = jobRepository.findById(task.getJobId()).orElse(null);
            if(job != null) {
                job.setStatus(JobStatus.FAILED);
                job.setErrorMessage("Map task failed after 3 retries: " + errorMessage);
                jobRepository.save(job);
            }
        } else {
            // increment retry count and reset task status to pending for retry
            task.setStatus(TaskStatus.PENDING);
            task.setWorkerId(null);
            task.setAssignedAt(null);
            task.setStartedAt(null);
            task.setErrorMessage(errorMessage);
            task.setRetryCount(task.getRetryCount() + 1);
            taskRepository.save(task);
            log.info("Map task {} queued for retry (attempt {})", taskId, task.getRetryCount());
        }
        
    }

    //
    @Scheduled(fixedDelay= 30000) // every 30 seconds, check for stuck map tasks and retry
    @Transactional
    public void checkStaleMapTasks() {
        // find map tasks that are in progress for more than 5 minutes and retry them
        List<Task> staleTasks = taskRepository.findStaleAssignedTasks(TaskStatus.ASSIGNED, LocalDateTime.now().minusMinutes(5));

        for(Task task : staleTasks) {
            if(task.getType() == TaskType.MAP) {
                log.warn("Found stale map task: {}. resetting for retry", task.getId());
                task.setStatus(TaskStatus.PENDING);
                task.setWorkerId(null);
                task.setAssignedAt(null);
                taskRepository.save(task); //reset task for retry
            }
        }
    } 
}
