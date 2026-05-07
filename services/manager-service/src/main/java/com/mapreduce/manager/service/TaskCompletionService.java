package com.mapreduce.manager.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mapreduce.manager.dto.TaskCompletionPayload;
import com.mapreduce.manager.entity.Task;
import com.mapreduce.manager.repository.JobRepository;
import com.mapreduce.manager.repository.TaskRepository;

@Service
public class TaskCompletionService {
    private static final Logger log = LoggerFactory.getLogger(TaskCompletionService.class);
    private final TaskRepository taskRepository;
    private final JobRepository jobRepository;
    private final ShuffleService shuffleService;
    
    public TaskCompletionService(TaskRepository taskRepository, JobRepository jobRepository,
            ShuffleService shuffleService) {
        this.taskRepository = taskRepository;
        this.jobRepository = jobRepository;
        this.shuffleService = shuffleService;
    }

    @Transactional
    public void processTaskCompletion(TaskCompletionPayload payload) {
        String taskId = payload.taskId();
        String jobId = payload.jobId();

        //find task
        Task task = taskRepository.findById(taskId).orElseThrow(() -> new RuntimeException("Task not found: " + taskId));
        //verify worker matches
        if(task.getWorkerId() != null && !task.getWorkerId().equals(payload.workerId())) {
            log.warn("Task {} was assigned to worker {} but completion came from worker {}", taskId, task.getWorkerId(), payload.workerId());
        }

        //handle based on status

    }
    

}
