package com.mapreduce.manager.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mapreduce.manager.dto.JobRequest;
import com.mapreduce.manager.dto.JobResponse;
import com.mapreduce.manager.entity.Job;
import com.mapreduce.manager.entity.JobStatus;
import com.mapreduce.manager.entity.TaskStatus;
import com.mapreduce.manager.repository.JobRepository;
import com.mapreduce.manager.repository.TaskRepository;
import com.mapreduce.manager.repository.TaskType;

@Service
public class JobService {
    private final ShuffleService shuffleService;
    private static final Logger log = LoggerFactory.getLogger(JobService.class);
    private final JobRepository jobRepository;
    private final TaskRepository taskRepository;
    private final InputtSplitterService splitter;
    private final MapperService mapperService;

    public JobService(JobRepository jobRepository, TaskRepository taskRepository, InputtSplitterService splitter, MapperService mapperService, ShuffleService shuffleService) {
        this.jobRepository = jobRepository;
        this.taskRepository = taskRepository;
        this.splitter = splitter;
        this.mapperService = mapperService;
        this.shuffleService = shuffleService;
    }


    @Transactional
    public JobResponse submitJob(JobRequest request) {
        log.info("Submitting new job: {}", request.getName());
        // create job entity
        Job job = new Job();
        job.setName(request.getName());
        job.setUserId(request.getUserId() != null ? request.getUserId() : "anonymous"); // default to anonymous if not provided
        job.setStatus(JobStatus.PENDING); // initial status
        job.setInputPath(request.getInputPath()); // store input path for later retrieval
        job.setOutputPath(request.getOutputPath()); //store output path
        job.setMapperCodePath(request.getMapperCodePath()); // store code paths for later retrieval
        job.setReducerCodePath(request.getReducerCodePath());
        job.setNumMappers(request.getNumMappers()); // store mapper/reduces counts
        job.setNumReducers(request.getNumReducers());

        // save job
        Job savedJob = jobRepository.save(job);
        log.info("Job created with ID: {}", savedJob.getId());

        // start job execution asynchronously
        startJobProcessing(savedJob.getId());
        return convertToResponse(savedJob);
    }

    @Transactional
    public void startJobProcessing(String jobId) {
        // can be called asynchronously after job creation
        log.info("Starting job processing for job: {}", jobId);

        // find job
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new RuntimeException("Job not found: " + jobId));

        // split input data
        try {
            job.setStatus(JobStatus.SPLITTING);
            job.setStartedAt(LocalDateTime.now());
            jobRepository.save(job);

            List<InputtSplitterService.SplitInfo> splits = splitter.splitInput(job);
            log.info("Input split into {} partitions", splits.size());

            // stsrt map phase
            mapperService.startMapping(job, splits);
        } catch (Exception e) {
            log.error("Job processing failed for job {} : {}", jobId, e.getMessage());
            job.setStatus(JobStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            jobRepository.save(job);
        }
    }

    public JobResponse getJobStatus(String jobId) {
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new RuntimeException("Job not found:" + jobId));
        return convertToResponse(job);
    }

    public List<JobResponse> listJobs(String userId, int page, int size) {
        List<Job> jobs;
        if(userId != null && !userId.isEmpty()) {
            jobs = jobRepository.findByUserId(userId);
        } else {
            jobs = jobRepository.findAll();
        }

        // simple pagination
        return jobs.stream()
            .skip((long) page * size)
            .limit(size)
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }
    
    @Transactional
    public void cancelJob(String jobId) {
        log.info("Cancelling job : {}", jobId);
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new RuntimeException("Job not found: " + jobId));
        // only allow cancellation if job is still running
        if(job.getStatus() == JobStatus.COMPLETED || job.getStatus() == JobStatus.FAILED) {
            throw new IllegalStateException("Cannot cancel a job that is already completed or failed");
        }

        // update status to cancelled
        job.setStatus(JobStatus.CANCELLED);
        job.setCompletedAt(LocalDateTime.now());
        jobRepository.save(job);
    }

    // helper method to convert entity to response dto
    private JobResponse convertToResponse(Job job) {
        JobResponse res = new JobResponse();
        res.setId(job.getId());
        res.setName(job.getName());
        res.setUserId(job.getUserId());
        res.setStatus(job.getStatus());
        res.setInputPath(job.getInputPath());
        res.setOutputPath(job.getOutputPath());
        res.setNumMappers(job.getNumMappers());
        res.setNumReducers(job.getNumReducers());
        res.setCompletedMappers(job.getCompletedMappers());
        res.setCompletedReducers(job.getCompletedReducers());
        res.setErrorMessage(job.getErrorMessage());
        res.setCreatedAt(job.getCreatedAt());
        res.setStartedAt(job.getStartedAt());
        res.setCompletedAt(job.getCompletedAt());

        // calculate progress
        double progress = 0.0;
        if(job.getStatus() == JobStatus.MAP_PHASE && job.getNumMappers() > 0) {
            progress = (double) job.getCompletedMappers() / job.getNumMappers() * 50.0; // map phase is 50% of total progress    
        } else if(job.getStatus() == JobStatus.REDUCE_PHASE && job.getNumReducers() > 0) {
            progress = 50.0 + (double) job.getCompletedReducers() / job.getNumReducers() * 50.0; // redice phase is the remaining 50%
        } else if(job.getStatus() == JobStatus.COMPLETED) {
            progress = 100.0;
        }
        res.setProgress(progress);
        return res;
    }

    // when a map task is completed this method is called
    @Transactional
    public void onMapTaskCompleted(String jobId) {
        Job job = jobRepository.findById(jobId).orElse(null);
        if(job == null || job.getStatus() != JobStatus.MAP_PHASE) return;

        long completedMappers = taskRepository.countByJobIdAndTypeAndStatus(jobId, TaskType.MAP, TaskStatus.COMPLETED);

        if(completedMappers >= job.getNumMappers()) {
            log.info("All map tasks completed for job {}, starting shuffle", jobId);
            shuffleService.initShuffleAndReduce(jobId);
        }
    }

}
