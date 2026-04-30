package com.mapreduce.manager.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mapreduce.manager.dto.TaskCompletionPayload;
import com.mapreduce.manager.service.TaskCompletionService;

import feign.Response;


@RestController
@RequestMapping("/internal/v1/callbacks")
public class CallbackController {
    private static final Logger log = LoggerFactory.getLogger(CallbackController.class);
    private final TaskCompletionService taskCompletionService;
    
    
    public CallbackController(TaskCompletionService taskCompletionService) {
        this.taskCompletionService = taskCompletionService;
    }

    /**
     * endpoint that workers call to report task completion status
     */
    @PostMapping("/task-completion")
    public ResponseEntity<Void> taskCompletion(@RequestBody TaskCompletionPayload payload) {
        log.info("Received task completion callback - taskId: {}, status: {}, workerId: {}", payload.taskId(), payload.taskStatus(), payload.workerId());
        taskCompletionService.processTaskCompletion(payload);
        
        return ResponseEntity.ok().build();
    }
    
    
}
