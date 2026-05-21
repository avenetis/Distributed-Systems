package com.tuc.distributed.worker.service;

import com.tuc.distributed.worker.api.TaskCompletionPayload;
import com.tuc.distributed.worker.api.TaskRequest;
import com.tuc.distributed.worker.api.TaskResponse;
import com.tuc.distributed.worker.client.ManagerClient;
import com.tuc.distributed.worker.domain.TaskStatus;
import com.tuc.distributed.worker.domain.TaskType;
import com.tuc.distributed.worker.mapreduce.KeyValue;
import com.tuc.distributed.worker.mapreduce.Mapper;
import com.tuc.distributed.worker.mapreduce.MapperRegistry;
import com.tuc.distributed.worker.mapreduce.Reducer;
import com.tuc.distributed.worker.mapreduce.ReducerRegistry;
import com.tuc.distributed.worker.minio.StorageClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class WorkerExecutionService {

    private static final Logger log = LoggerFactory.getLogger(WorkerExecutionService.class);

    private final StorageClient storageClient;//διαβάζει και να γράφει αρχεία στο storage, δηλαδή στο MinIO
    private final MapperRegistry mapperRegistry;
    private final ReducerRegistry reducerRegistry;
    private final PartitionService partitionService;//χρησιμοποιείται μόνο στο MAP phase
    private final ManagerClient managerClient;
    private final String workerId;

    public WorkerExecutionService(StorageClient storageClient,
                                  MapperRegistry mapperRegistry,
                                  ReducerRegistry reducerRegistry,
                                  PartitionService partitionService,
                                  ManagerClient managerClient,
                                  @Value("${worker.id:${HOSTNAME:worker-local}}") String workerId) {
        this.storageClient = storageClient;
        this.mapperRegistry = mapperRegistry;
        this.reducerRegistry = reducerRegistry;
        this.partitionService = partitionService;
        this.managerClient = managerClient;
        this.workerId = workerId;
    }

    public TaskResponse execute(TaskRequest request) {
        log.info("Worker {} started task {} of type {}", workerId, request.taskId(), request.type());

        try {
            TaskResponse response = switch (request.type()) {
                case MAP -> executeMap(request);
                case REDUCE -> executeReduce(request);
            };

            notifyManager(request, response.status(), response.message(), response.outputObjectKeys());
            return response;
        } catch (Exception e) {
            log.error("Task {} failed", request.taskId(), e);
            notifyManager(request, TaskStatus.FAILED, e.getMessage(), List.of());
            return new TaskResponse(request.taskId(), TaskStatus.FAILED, e.getMessage(), List.of());
        }
    }

    private TaskResponse executeMap(TaskRequest request) {
        Mapper mapper = mapperRegistry.get(request.mapperClass());
        int reducersCount = request.reducersCount() == null ? 1 : request.reducersCount(); //αν true(null) βάλε στο reducerCount =1
        Map<Integer, List<KeyValue>> partitions = new HashMap<>(); //Το Integer (το Key του Map): Είναι ο αριθμός του Reducer (το Partition ID)
        // Το List<KeyValue> (το Value του Map): Είναι μια λίστα που θα περιέχει αντικείμενα τύπου KeyValue (δηλαδή ζευγάρια όπως ("java", "1"))
        for (int i = 0; i < reducersCount; i++) {//partion 0 -> empty list
            partitions.put(i, new ArrayList<>()); //partion 1 -> empty list
        }

        for (String inputKey : request.inputObjectKeys()) {
            String content = storageClient.readText(request.inputBucket(), inputKey);// παίρνει το bucket και το αποθηκεύη στην ram σαν string
            String[] lines = content.split("\\R");// αν είχαμε hello world\n hello me θα γίνει [hello word,hello me]
            for (String line : lines) {
                for (KeyValue pair : mapper.map(line)) {//hello world θα γινει hello:1   word:1
                    int partition = partitionService.partitionOf(pair.key(), reducersCount);//βρίσκω σε ποιο partition (με μέθοδο static hashing) πρέπει να πάει πχ hello -> partion 1
                    //κάτι που θα μας χρησιμέυσει στην επόμενη φάση για το shuffle και reduce
                    partitions.get(partition).add(pair);//Γεμίζουμε τα άδεια partions που δημιουργήσαμε στην γραμμή 78 για την φάση shuffle και reduce
                    // Παίρνει το Key-Value pair που μόλις παρήγαγε ο Mapper και το τοποθετεί μέσα στη συγκεκριμένη δυναμική λίστα (ArrayList) που αντιστοιχεί στο Partition ID που υπολογίσαμε προηγουμένως.
                }//partition 1 -> [hello -> 1]
            }
        }

        List<String> outputKeys = new ArrayList<>();
        for (Map.Entry<Integer, List<KeyValue>> entry : partitions.entrySet()) {
            String objectKey = "%s/%s/part-%d.txt".formatted(request.outputPrefix(), request.taskId(), entry.getKey());//Το objectKey γίνεται: "intermediate/task-1/part-0.txt" για mapper 1 |Το objectKey γίνεται:"intermediate/task-2/part-0.txt"
            String payload = entry.getValue().stream()//απο έτσι [world:1, me:1] το κάνουμε
                    .map(kv -> kv.key() + "\t" + kv.value())//word  1
                    .collect(Collectors.joining("\n"));     // me    1
            storageClient.writeText(request.outputBucket(), objectKey, payload);//τα δεδομένα βγαίνουν από τη RAM και γίνονται αρχείο στο storage
            outputKeys.add(objectKey);//Κρατάμε το όνομα του αρχείου που μόλις ανεβάσαμε στην αρχική μας λίστα
        }

        return new TaskResponse(request.taskId(), TaskStatus.COMPLETED, "Map task completed", outputKeys);
    }

    private TaskResponse executeReduce(TaskRequest request) {
        Reducer reducer = reducerRegistry.get(request.reducerClass());
        Map<String, List<String>> grouped = new HashMap<>();
// Αυτά είναι τα part-X.txt αρχεία από ΟΛΟΥΣ τους Mappers που ανήκουν σε αυτό το partition.
        for (String inputKey : request.inputObjectKeys()) {
            String content = storageClient.readText(request.inputBucket(), inputKey);
            if (content.isBlank()) {//αγνοεί άδεια αρχεία
                continue;
            }
            String[] lines = content.split("\\R");
            for (String line : lines) {
                if (line.isBlank()) {//αγνοεί άδειες γραμμές
                    continue;
                }
                String[] parts = line.split("\\t", 2);
                if (parts.length != 2) {
                    throw new IllegalArgumentException("Invalid intermediate record: " + line);
                }//Κάνω την συγκέντρωση εδώ
                grouped.computeIfAbsent(parts[0], ignored -> new ArrayList<>()).add(parts[1]);
            }
        }

        Map<String, List<String>> sorted = grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        StringBuilder finalOutput = new StringBuilder();//τρέχβω reducer για κάθε key
        for (Map.Entry<String, List<String>> entry : sorted.entrySet()) {
            finalOutput.append(reducer.reduce(entry.getKey(), entry.getValue())).append('\n');
        }

        int partition = request.reducePartition() == null ? 0 : request.reducePartition();
        String objectKey = "%s/reduce-%d.txt".formatted(request.outputPrefix(), partition);
        storageClient.writeText(request.outputBucket(), objectKey, finalOutput.toString());
        return new TaskResponse(request.taskId(), TaskStatus.COMPLETED, "Reduce task completed", List.of(objectKey));
    }

    private void notifyManager(TaskRequest request, TaskStatus status, String details, List<String> outputKeys) {
        managerClient.sendCompletion(
                request.managerCallbackUrl(),
                new TaskCompletionPayload(
                        request.taskId(),
                        request.jobId(),
                        status,
                        workerId,
                        details,
                        outputKeys
                )
        );
    }
}
