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

        int maxAttempts = 3;
        long backoffMillis = 1000;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                restClient.post()
                        .uri(callbackUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(payload)
                        .retrieve()
                        .toBodilessEntity();

                log.info(
                        "Reported task {} to manager callback {} on attempt {}",
                        payload.taskId(),
                        callbackUrl,
                        attempt
                );
                return;
            } catch (Exception e) {
                if (attempt == maxAttempts) {
                    log.error(
                            "Failed to notify manager for task {} after {} attempts",
                            payload.taskId(),
                            maxAttempts,
                            e
                    );
                    return;
                }

                log.warn(
                        "Failed to notify manager for task {} on attempt {}. Retrying in {} ms",
                        payload.taskId(),
                        attempt,
                        backoffMillis
                );

                try {
                    Thread.sleep(backoffMillis);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    log.error(
                            "Interrupted while retrying manager callback for task {}",
                            payload.taskId(),
                            interruptedException
                    );
                    return;
                }

                backoffMillis *= 2;
            }
        }
    }
}
