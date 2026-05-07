package com.tuc.distributed.worker.client;

import com.tuc.distributed.worker.api.TaskCompletionPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpManagerClient implements ManagerClient {

    private static final Logger log = LoggerFactory.getLogger(HttpManagerClient.class);
    private final RestClient restClient = RestClient.builder().build();

    @Override
    public void sendCompletion(String callbackUrl, TaskCompletionPayload payload) {
        if (callbackUrl == null || callbackUrl.isBlank()) {
            log.info("No manager callback URL provided for task {}", payload.taskId());
            return;
        }

        try {
            restClient.post()
                    .uri(callbackUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Reported task {} to manager callback {}", payload.taskId(), callbackUrl);
        } catch (Exception e) {
            log.error("Failed to notify manager for task {}", payload.taskId(), e);
        }
    }
}
