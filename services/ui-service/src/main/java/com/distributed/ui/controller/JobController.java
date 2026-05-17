package com.distributed.ui.controller;

import com.distributed.ui.dto.JobRequest;
import com.distributed.ui.dto.JobResponse;
import com.distributed.ui.dto.JobStatusResponse;
import com.distributed.ui.service.JobService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    public JobResponse submitJob(@RequestBody JobRequest request) {
        return jobService.submitJob(request);
    }

    @GetMapping("/{jobId}")
    public JobStatusResponse getJobStatus(@PathVariable String jobId) {
        return jobService.getJobStatus(jobId);
    }

    @GetMapping("/{jobId}/result")
    public JobStatusResponse getJobResult(@PathVariable String jobId) {
        return jobService.getJobResult(jobId);
    }
}