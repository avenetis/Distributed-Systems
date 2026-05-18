package com.mapreduce.manager.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.mapreduce.manager.entity.Job;
import com.mapreduce.manager.entity.JobStatus;
import com.mapreduce.manager.entity.Task;
import com.mapreduce.manager.entity.TaskStatus;
import com.mapreduce.manager.repository.JobRepository;
import com.mapreduce.manager.repository.TaskRepository;

@Component
public class ManagerStartupRecovery {

    private static final Logger log = LoggerFactory.getLogger(ManagerStartupRecovery.class);

    private final JobRepository jobRepository;
    private final TaskRepository taskRepository;

    public ManagerStartupRecovery(JobRepository jobRepository,
                                   TaskRepository taskRepository) {
        this.jobRepository = jobRepository;
        this.taskRepository = taskRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void recoverInProgressJobs() {
        log.info("=== Manager startup recovery starting ===");

        List<Job> activeJobs = jobRepository.findByStatusIn(List.of(
            JobStatus.PENDING,
            JobStatus.MAP_PHASE,
            JobStatus.SHUFFLE_PHASE,
            JobStatus.REDUCE_PHASE
        ));

        if (activeJobs.isEmpty()) {
            log.info("No jobs to recover — clean startup");
            return;
        }

        log.info("Found {} jobs to recover after restart", activeJobs.size());

        for (Job job : activeJobs) {
            log.info("Recovering job {} (status: {})", job.getId(), job.getStatus());

            // Reset ASSIGNED tasks back to PENDING
            // so workers can pick them up again
            List<Task> assignedTasks = taskRepository
                .findByJobIdAndStatus(job.getId(), TaskStatus.ASSIGNED);

            for (Task task : assignedTasks) {
                task.setStatus(TaskStatus.PENDING);
                task.setWorkerId(null);
                task.setAssignedAt(null);
                taskRepository.save(task);
                log.info("  Reset task {} ({}) back to PENDING",
                    task.getId(), task.getType());
            }

            // Also reset RUNNING tasks
            List<Task> runningTasks = taskRepository
                .findByJobIdAndStatus(job.getId(), TaskStatus.RUNNING);

            for (Task task : runningTasks) {
                task.setStatus(TaskStatus.PENDING);
                task.setWorkerId(null);
                task.setAssignedAt(null);
                task.setStartedAt(null);
                taskRepository.save(task);
                log.info("  Reset running task {} ({}) back to PENDING",
                    task.getId(), task.getType());
            }

            int totalReset = assignedTasks.size() + runningTasks.size();
            log.info("Job {} recovered — {} tasks reset to PENDING",
                job.getId(), totalReset);
        }

        log.info("=== Manager startup recovery complete ===");
    }
}