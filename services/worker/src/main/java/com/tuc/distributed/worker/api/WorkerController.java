package com.tuc.distributed.worker.api;

import com.tuc.distributed.worker.service.WorkerExecutionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class WorkerController {

    private final WorkerExecutionService workerExecutionService;

    public WorkerController(WorkerExecutionService workerExecutionService) {
        this.workerExecutionService = workerExecutionService;
    }

    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse("UP", "worker-service");
    }

    @PostMapping("/task")
    public ResponseEntity<TaskResponse> executeTask(@Valid @RequestBody TaskRequest request) {
        return ResponseEntity.ok(workerExecutionService.execute(request));
    }

    @PostMapping("/task/complete")
    public ResponseEntity<TaskResponse> complete(@RequestBody TaskResponse response) {
        return ResponseEntity.ok(response);
    }
}
