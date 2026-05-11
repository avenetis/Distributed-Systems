package com.distributed.ui.service;

import com.distributed.ui.dto.JobRequest;
import com.distributed.ui.dto.JobResponse;
import com.distributed.ui.dto.JobStatusResponse;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class JobService {

    public JobResponse submitJob(JobRequest request) {
        if (request.getInputPath() == null || request.getInputPath().isBlank()) {
            return new JobResponse(null, "FAILED", "Input path is required");
        }

        if (request.getOutputPath() == null || request.getOutputPath().isBlank()) {
            return new JobResponse(null, "FAILED", "Output path is required");
        }

        String jobId = UUID.randomUUID().toString();

        return new JobResponse(
                jobId,
                "SUBMITTED",
                "Job submitted successfully"
        );
    }

    public JobStatusResponse getJobStatus(String jobId) {
        return new JobStatusResponse(
                jobId,
                "RUNNING",
                50,
                "Job is currently running"
        );
    }

    public String getJobResult(String jobId) {
        return "Result for job " + jobId;
    }
}