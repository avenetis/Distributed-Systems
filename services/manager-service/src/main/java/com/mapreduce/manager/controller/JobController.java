package com.mapreduce.manager.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mapreduce.manager.dto.JobRequest;
import com.mapreduce.manager.dto.JobResponse;
import com.mapreduce.manager.service.JobService;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {
    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    public ResponseEntity<JobResponse> submitJob(@Valid @RequestBody JobRequest request) {
        JobResponse response = jobService.submitJob(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<JobResponse> getJobStatus(@PathVariable String jobId) {
        JobResponse response = jobService.getJobStatus(jobId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{jobId}/result")
    public ResponseEntity<String> getJobResult(@PathVariable String jobId) {
        String result = jobService.getJobResult(jobId);
        return ResponseEntity.ok(result);
    }

    @GetMapping
    public ResponseEntity<List<JobResponse>> listJobs(
        @RequestParam(required = false) String userId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        List<JobResponse> jobs = jobService.listJobs(userId, page, size);
        return ResponseEntity.ok(jobs);
    }
    
    @DeleteMapping("/{jobId}")
    public ResponseEntity<Void> cancelJob(@PathVariable String jobId) {
        jobService.cancelJob(jobId);
        return ResponseEntity.noContent().build();
    }
    

}
