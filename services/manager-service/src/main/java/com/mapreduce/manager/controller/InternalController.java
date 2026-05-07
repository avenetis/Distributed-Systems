package com.mapreduce.manager.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mapreduce.manager.service.WorkerService;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.mapreduce.manager.dto.HeartbeatRequest;
import com.mapreduce.manager.dto.TaskCallbackPayload;
import com.mapreduce.manager.dto.WorkerRegistrationRequest;
import com.mapreduce.manager.dto.WorkerRegistrationResponse;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.mapreduce.manager.dto.ClaimTaskResponse;
import com.mapreduce.manager.entity.Worker;



@RestController
@RequestMapping("/internal/v1")
public class InternalController {
    private final WorkerService workerService;

    public InternalController(WorkerService workerService) {
        this.workerService = workerService;
    }

    @PostMapping("/workers/register")
    public ResponseEntity<WorkerRegistrationResponse> registerWorker(@Valid @RequestBody WorkerRegistrationRequest request) {
        WorkerRegistrationResponse response = workerService.registerWorker(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/workers/heartbeat")
    public ResponseEntity<Void> heartbeat(@RequestBody HeartbeatRequest request) {
        workerService.updateHeartbeat(request);
        
        return ResponseEntity.ok().build();
    }

    @GetMapping("/workers/{workerId}/claim-task")
    public ResponseEntity<ClaimTaskResponse> claimTask(@PathVariable String workerId) {
        ClaimTaskResponse response = workerService.claimTask(workerId);
        if(response.isHasWork()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }
    }

    @PostMapping("/callbacks/task-complete")
    public ResponseEntity<Void> onTaskComplete(@RequestBody TaskCallbackPayload payload) {
        if("DONE".equals(payload.getStatus())) {
            workerService.onTaskCompleted(payload);
        } else if ("FAILED".equals(payload.getStatus())) {
            workerService.onTaskFailed(payload);
        }
        
        return ResponseEntity.ok().build();
    }

    @GetMapping("/workers")
    public ResponseEntity<List<Worker>> getWorkers() {
        return ResponseEntity.ok(workerService.getActivWorkers());
    }   

}
