package com.mapreduce.manager.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class ShuffleService {
    private static final Logger log = LoggerFactory.getLogger(ShuffleService.class);
    private final TaskRepository taskRepository;
    private final JobRepository jobRepo;


    public ShuffleService(TaskRepository taskRepository, JobRepository jobRepo) {
        this.taskRepository = taskRepository;
        this.jobRepo = jobRepo;
    }

    /**
     * Called when all MAP tasks for a job are complete.
     * Collects all intermediate outputs from map tasks, grops them by partition
     * and creates REDUCE tasks for each partition
     */
    @Transactional
    public void initShuffleAndReduce(String jobId) {
        log.info("Initiating shuffle phase for job: {}", jobId);

        Job job = jobRepo.findById(jobId).orElseThrow(() -> new RuntimeException("Job not foound: " + jobId));

        // get all completed MAP tasks for this job
        List<Task> mapTasks = taskRepository.findByJobIdAndType(jobId, TaskType.MAP).stream()
                                .filter(t -> t.getStatus() == TaskStatus.COMPLETED)
                                .toList();
        
        log.info("Found {} completed map tasks for job {}", mapTasks.size(), jobId);

        // for every reducer partition, collect all intermediate file locations
        Map<Integer, List<String>> partitionToInter = new HashMap<>();

        for(Task mapTask : mapTasks) {
            String mapOutputLocation = mapTask.getOutputLocation();
            // the map task output location contains multiple partition files
            // worker saves them as {outputPrefix}/{taskId}/part-{partition}.txt

            for(int partition = 0; partition < job.getNumReducers(); partition++) {
                String intermediateFile = String.format("%s/part-%d.txt", mapOutputLocation.replace("/" + mapTask.getId(), ""), partition);
                partitionToInter.computeIfAbsent(partition, k -> new ArrayList<>()).add(intermediateFile);
            }
        }

        // create REDUCE tasks for each partition
        for(int partition = 0; partition < job.getNumReducers(); partition++) {
            List<String> intermediateFiles = partitionToInter.get(partition);

            if(intermediateFiles == null || intermediateFiles.isEmpty()) {
                log.warn("No intermediate files found for partition {} of job {}", partition, jobId);
                continue;
            }

            Task reduceTask = new Task();
            reduceTask.setJobId(jobId);
            reduceTask.setType(TaskType.REDUCE);
            reduceTask.setStatus(TaskStatus.PENDING);
            reduceTask.setPartitionIndex(partition);
            reduceTask.setInputPath(String.join(",", intermediateFiles));
            reduceTask.setOutputLocation(String.format("%s/reduce-output/part-%d", job.getOutputPath(), partition));
            reduceTask.setRetryCount(0);

            taskRepository.save(reduceTask);
            log.info("Created REDUCE task {} for partition {}", reduceTask.getId(), partition);
        }

        //update job status to REDUCE_PHASE
        job.setStatus(JobStatus.REDUCE_PHASE);
        jobRepo.save(job);

        log.info("Shuffle phase for job {}. {} REDUCE tasks created", jobId, job.getNumReducers());
    }

}
