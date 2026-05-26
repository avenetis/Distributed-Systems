package com.tuc.distributed.worker.client;

import com.tuc.distributed.worker.api.TaskCompletionPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpManagerClient implements ManagerClient {

    private static final Logger log = LoggerFactory.getLogger(HttpManagerClient.class);

    private final RestClient restClient = RestClient.builder().build();

    @Value("${worker.auth-token:}")
    private String workerAuthToken;

    @Override
    public void sendCompletion(String callbackUrl, TaskCompletionPayload payload) {
        if (callbackUrl == null || callbackUrl.isBlank()) {
            log.info("No manager callback URL provided for task {}", payload.taskId());
            return;
        }

        int maxAttempts = 3;//Ο worker θα προσπαθήσει να ενημερώσει τον manager μέχρι 3 φορές.
        long backoffMillis = 1000;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                restClient.post()//http request προς manager
                        .uri(callbackUrl)//ορίζει την διεύθυνση του manager
                        .header("X-Worker-Token", workerAuthToken)//Ο manager θα το ελέγξει πρωτού δεχτεί το callback
                        .contentType(MediaType.APPLICATION_JSON)// τα δεδομένα που γίνοται post ειναι json
                        .body(payload)// TaskCompletionPayload
                        .retrieve()
                        .toBodilessEntity();

                log.info(//Αν το Callback Πετύχει
                        "Reported task {} to manager callback {} on attempt {}",
                        payload.taskId(),
                        callbackUrl,
                        attempt
                );//Στον manager, το callback φτάνει στον controller και μετά το task αλλάζει κατάσταση: ASSIGNED->COMPLETED
                return;
            } catch (Exception e) {//Αν το Callback αποτύχει
                if (attempt == maxAttempts) {//αν ήταν η 3η προσπάθεια
                    log.error(
                            "Failed to notify manager for task {} after {} attempts",
                            payload.taskId(),
                            maxAttempts,
                            e
                    );
                    return;
                }

                log.warn(//αναμονή πριν την επόμενξ προσπάθεια
                        "Failed to notify manager for task {} on attempt {}. Retrying in {} ms",
                        payload.taskId(),
                        attempt,
                        backoffMillis
                );

                try {
                    Thread.sleep(backoffMillis);//1η αποτυχία -> περιμένει 1 δευτερόλεπτο
                    //2η αποτυχία -> περιμένει 2 δευτερόλεπτα 3η αποτυχία -> σταματά
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();//επαναφέρει την ένδειξη ότι το thread διακόπηκε
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