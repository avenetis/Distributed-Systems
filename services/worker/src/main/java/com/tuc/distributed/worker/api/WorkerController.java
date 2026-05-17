package com.tuc.distributed.worker.api;

import com.tuc.distributed.worker.service.WorkerExecutionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController/*Αυτή η κλάση είναι REST controller.
Οι μέθοδοι της μπορούν να απαντούν σε HTTP requests.
Τα αντικείμενα που επιστρέφει θα μετατραπούν σε JSON.*/
@RequestMapping
public class WorkerController {//Είναι η απάντηση του worker όταν κάποιος ρωτήσει Είναι η απάντηση του worker όταν κάποιος ρωτήσει:

    private final WorkerExecutionService workerExecutionService;

    public WorkerController(WorkerExecutionService workerExecutionService) {
        this.workerExecutionService = workerExecutionService;
    }

    @GetMapping("/health")//ο Manager να ελέγχει αν ο worker είναι διαθέσιμος το Kubernetes να ελέγχει αν το pod είναι ζωντανό
    //Όταν κάποιος κάνει GET /health,τρέξε τη μέθοδο health().
    public HealthResponse health() {
        return new HealthResponse("UP", "worker-service");
        /*Η Spring το μετατρέπει σε JSON:
{
  "status": "UP",
  "service": "worker-service"
}*/
    }

    @PostMapping("/task")
    /*Όταν κάποιος κάνει POST /task,στείλει ένα TaskRequest σε JSON,τότε ο worker θα εκτελέσει το task.*/
    public ResponseEntity<TaskResponse> executeTask(@Valid @RequestBody TaskRequest request) {
        return ResponseEntity.ok(workerExecutionService.execute(request));
    }

    @PostMapping("/task/complete")
    public ResponseEntity<TaskResponse> complete(@RequestBody TaskResponse response) {//Πάρε το JSON που ήρθε στο HTTP request body και μετέτρεψέ το σε Java αντικείμενο TaskRequest.
        return ResponseEntity.ok(response);
    }
}