# Worker Service

Έτοιμο starter implementation για το κομμάτι των Workers του MapReduce project.

## Τι κάνει

Ο worker:
- δέχεται ένα task με `POST /task`
- αν το task είναι `MAP`, διαβάζει input objects από MinIO, τρέχει mapper και γράφει intermediate partitions στο MinIO
- αν το task είναι `REDUCE`, διαβάζει intermediate objects από MinIO, κάνει group/sort/reduce και γράφει final output στο MinIO
- προαιρετικά ενημερώνει τον Manager μέσω callback URL

Το implementation ακολουθεί το design document σας:
- οι workers είναι εφήμερα compute units
- τρέχουν ως Kubernetes Jobs
- διαβάζουν/γράφουν intermediate και final data στο MinIO
- ενημερώνουν τον Manager για completion/failure fileciteturn4file0turn4file1

## Stack

- Java 17
- Spring Boot 3
- MinIO Java SDK
- Docker
- Kubernetes Job manifest

## API

### Health

`GET /health`

### Execute task

`POST /task`

Παράδειγμα request για MAP:

```json
{
  "taskId": "map-1",
  "jobId": "job-123",
  "type": "MAP",
  "inputBucket": "datasets",
  "inputObjectKeys": ["input/chunk-1.txt"],
  "outputBucket": "datasets",
  "outputPrefix": "jobs/job-123/intermediate",
  "mapperClass": "WordCountMapper",
  "reducerClass": "WordCountReducer",
  "reducersCount": 2,
  "managerCallbackUrl": "http://manager-service:8082/internal/tasks/complete"
}
```

Παράδειγμα request για REDUCE:

```json
{
  "taskId": "reduce-0",
  "jobId": "job-123",
  "type": "REDUCE",
  "inputBucket": "datasets",
  "inputObjectKeys": [
    "jobs/job-123/intermediate/map-1/part-0.txt",
    "jobs/job-123/intermediate/map-2/part-0.txt"
  ],
  "outputBucket": "datasets",
  "outputPrefix": "jobs/job-123/output",
  "mapperClass": "WordCountMapper",
  "reducerClass": "WordCountReducer",
  "reducePartition": 0,
  "managerCallbackUrl": "http://manager-service:8082/internal/tasks/complete"
}
```

## Τι υποστηρίζει ήδη

- `WordCountMapper`
- `WordCountReducer`

Αν θέλετε κι άλλα algorithms, προσθέτετε νέο `Mapper` ή `Reducer` και το δηλώνετε στο registry.

## Build

```bash
mvn clean package
```

## Run locally

```bash
export MINIO_ENDPOINT=http://localhost:9000
export MINIO_ACCESS_KEY=minioadmin
export MINIO_SECRET_KEY=minioadmin
mvn spring-boot:run
```

## Docker build

```bash
docker build -t worker-service:latest .
```

## Σημειώσεις

Αυτό είναι έτοιμο **worker-side implementation** και όχι όλο το σύστημα. Για να δουλέψει end-to-end θέλει:
- Manager να δημιουργεί Jobs/pods
- σωστό MinIO bucket setup
- callback endpoint στον Manager
- κοινό contract για task metadata

Με άλλα λόγια: το δικό σου κομμάτι είναι πλέον έτοιμο ως βάση, αλλά η πλήρης ολοκλήρωση εξαρτάται και από το Manager/service integration του υπόλοιπου project. 













Worker Service – Current Progress
Overview
Το worker-service αποτελεί το εκτελεστικό κομμάτι του συστήματος MapReduce. Ο ρόλος του είναι να δέχεται tasks από το υπόλοιπο σύστημα και να εκτελεί την αντίστοιχη εργασία MAP ή REDUCE.
Σε αυτό το στάδιο έγινε η αρχική τοπική επαλήθευση του service, δηλαδή επιβεβαιώθηκε ότι:
το Spring Boot application ξεκινά σωστά,
το HTTP API του worker λειτουργεί,
η σύνδεση με MinIO είναι σωστή,
ο worker μπορεί να διαβάσει input object από MinIO,
να εκτελέσει ένα MAP task,
και να γράψει το παραγόμενο output ξανά στο MinIO.
Project Structure
Το worker-service περιέχει τα βασικά πακέτα:
api
Περιλαμβάνει τα REST endpoints και τα request/response models (WorkerController, TaskRequest, TaskResponse, HealthResponse).
config
Περιλαμβάνει το configuration για MinIO (MinioConfig, MinioProperties).
domain
Περιλαμβάνει enums και βασικούς τύπους όπως TaskType και TaskStatus.
mapreduce
Περιλαμβάνει τη λογική map/reduce, όπως WordCountMapper και WordCountReducer.
minio
Περιλαμβάνει τον client αποθήκευσης για επικοινωνία με MinIO.
service
Περιλαμβάνει την κύρια επιχειρησιακή λογική εκτέλεσης του worker.
Local Configuration
Το service ρυθμίστηκε ώστε να τρέχει τοπικά στο port 8083.
Το αρχείο application.yml που χρησιμοποιήθηκε είναι της μορφής:
server:
port: 8083

spring:
application:
name: worker-service

worker:
id: ${WORKER_ID:${HOSTNAME:worker-local}}

minio:
endpoint: ${MINIO_ENDPOINT:http://localhost:9000}
accessKey: ${MINIO_ACCESS_KEY:minioadmin}
secretKey: ${MINIO_SECRET_KEY:minioadmin}

logging:
level:
root: INFO
com.tuc.distributed.worker: INFO
Με αυτή τη ρύθμιση, αν δεν οριστούν environment variables, ο worker χρησιμοποιεί τοπικό MinIO στο http://localhost:9000 με credentials:
access key: minioadmin
secret key: minioadmin
Endpoints Verified
1. Health endpoint
   Ο worker εκθέτει endpoint:
   GET /health
   Το endpoint αυτό χρησιμοποιήθηκε για να επιβεβαιωθεί ότι το service είναι ενεργό και απαντά κανονικά.
   Παράδειγμα URL:
   http://localhost:8083/health
   Η σωστή απάντηση είναι JSON της μορφής:
   {
   "status": "UP",
   "service": "worker-service"
   }
2. Task execution endpoint
   Ο worker εκθέτει endpoint:
   POST /task
   Το endpoint αυτό δέχεται ένα TaskRequest και εκτελεί το αντίστοιχο task.
   Η δομή του request είναι:
   {
   "taskId": "task-1",
   "jobId": "job-1",
   "type": "MAP",
   "inputBucket": "input-data",
   "inputObjectKeys": ["file1.txt"],
   "outputBucket": "intermediate-data",
   "outputPrefix": "job-1/map/",
   "mapperClass": "WordCountMapper",
   "reducerClass": "WordCountReducer",
   "reducersCount": 1,
   "reducePartition": 0,
   "managerCallbackUrl": "http://localhost:8081/task/complete"
   }
   Steps Followed During Local Testing
   Step 1 – Run the Spring Boot application
   Το service εκτελέστηκε από το IntelliJ μέσω της main class:
   WorkerServiceApplication
   Κατά την πρώτη εκκίνηση εμφανίστηκε σφάλμα σχετικό με MinIO configuration:
   endpoint must not be null
   Αυτό έδειξε ότι αρχικά δεν είχε φορτωθεί σωστά το configuration ή δεν είχε γίνει σωστό rebuild.
   Μετά από rebuild του project, το application εκκινήθηκε σωστά.
   Τελικό επιτυχές startup log:
   Tomcat started on port 8083 (http) with context path '/'
   Started WorkerServiceApplication
   Αυτό επιβεβαίωσε ότι ο worker-service τρέχει τοπικά.
   Step 2 – Verify health endpoint
   Έγινε δοκιμή του endpoint:
   http://localhost:8083/health
   Η επιτυχής απάντηση επιβεβαίωσε ότι:
   το web layer λειτουργεί,
   ο controller έχει γίνει σωστά register,
   και το service είναι διαθέσιμο.
   Step 3 – First task execution attempt
   Αρχικά δοκιμάστηκε αποστολή POST /task με το παρακάτω command:
   curl -X POST http://localhost:8083/task \
   -H "Content-Type: application/json" \
   -d '{
   "taskId": "task-1",
   "jobId": "job-1",
   "type": "MAP",
   "inputBucket": "input-data",
   "inputObjectKeys": ["file1.txt"],
   "outputBucket": "intermediate-data",
   "outputPrefix": "job-1/map/",
   "mapperClass": "WordCountMapper",
   "reducerClass": "WordCountReducer",
   "reducersCount": 1,
   "reducePartition": 0,
   "managerCallbackUrl": "http://localhost:8081/task/complete"
   }'
   Η πρώτη απάντηση ήταν:
   {
   "taskId":"task-1",
   "status":"FAILED",
   "message":"Failed to read object input-data/file1.txt",
   "outputObjectKeys":[]
   }
   Αυτό έδειξε ότι:
   το endpoint /task λειτουργεί σωστά,
   ο worker μπαίνει πράγματι στη λογική εκτέλεσης,
   αλλά δεν μπορούσε να βρει το αρχείο εισόδου στο MinIO.
   Άρα το API και το execution flow ήταν σωστά, αλλά έλειπαν τα δεδομένα εισόδου.
   MinIO Verification and Setup
   Για να επιβεβαιωθεί ότι το MinIO τρέχει, ανοίχθηκε το web console:
   http://localhost:9001
   Η εμφάνιση της login σελίδας επιβεβαίωσε ότι το MinIO είναι ενεργό.
   Χρησιμοποιήθηκαν τα credentials:
   username: minioadmin
   password: minioadmin
   Στη συνέχεια δημιουργήθηκαν τα buckets:
   input-data
   intermediate-data
   Έπειτα προστέθηκε input object:
   bucket: input-data
   object: file1.txt
   με δοκιμαστικό περιεχόμενο τύπου:
   Hello word
   Check check
   Successful MAP Task Execution
   Αφού δημιουργήθηκε το input object στο MinIO, εκτελέστηκε ξανά το ίδιο curl command:
   curl -X POST http://localhost:8083/task \
   -H "Content-Type: application/json" \
   -d '{
   "taskId": "task-1",
   "jobId": "job-1",
   "type": "MAP",
   "inputBucket": "input-data",
   "inputObjectKeys": ["file1.txt"],
   "outputBucket": "intermediate-data",
   "outputPrefix": "job-1/map/",
   "mapperClass": "WordCountMapper",
   "reducerClass": "WordCountReducer",
   "reducersCount": 1,
   "reducePartition": 0,
   "managerCallbackUrl": "http://localhost:8081/task/complete"
   }'
   Αυτή τη φορά η απάντηση ήταν επιτυχής:
   {
   "taskId":"task-1",
   "status":"DONE",
   "message":"Map task completed",
   "outputObjectKeys":["job-1/map//task-1/part-0.txt"]
   }
   Αυτό επιβεβαίωσε ότι ο worker:
   διάβασε επιτυχώς το input object από το MinIO,
   εκτέλεσε το WordCountMapper,
   δημιούργησε intermediate output,
   και το αποθήκευσε στο bucket intermediate-data.
   Observed MAP Output Behavior
   Κατά τον έλεγχο του output, παρατηρήθηκε ότι οι λέξεις αποθηκεύονται σε lowercase μορφή.
   Παράδειγμα input:
   Hello word
   Check check
   Παράδειγμα map output:
   hello    1
   word     1
   check    1
   check    1
   Αυτό σημαίνει ότι ο mapper κάνει normalization των λέξεων, πιθανότατα με μετατροπή σε lowercase, κάτι που είναι λογικό και επιθυμητό για word count χρήση, ώστε λέξεις όπως Hello, hello, HELLO να θεωρούνται ίδιες.
   Current Status
   Μέχρι αυτό το σημείο έχει επιβεβαιωθεί επιτυχώς ότι το worker-service:
   ξεκινά τοπικά ως Spring Boot εφαρμογή,
   εκθέτει endpoint /health,
   εκθέτει endpoint /task,
   συνδέεται σωστά με MinIO,
   διαβάζει input object από bucket,
   εκτελεί MAP task,
   και γράφει το intermediate αποτέλεσμα πίσω στο MinIO.
   Με άλλα λόγια, ο worker έχει ήδη περάσει από στάδιο απλού skeleton σε λειτουργικό local prototype.
   Known Notes / Small Issues
   Output path formatting
   Το output object key επέστρεψε path της μορφής:
   job-1/map//task-1/part-0.txt
   Παρατηρείται διπλό slash (//). Αυτό δεν εμπόδισε τη λειτουργία, αλλά είναι μικρό formatting issue που αργότερα καλό είναι να διορθωθεί.
   POST endpoint testing
   Το endpoint /task δεν μπορεί να ελεγχθεί απλά από browser, επειδή απαιτεί POST request με JSON body.
   Για τοπική δοκιμή χρησιμοποιήθηκε curl.
   Java warnings
   Κατά την εκκίνηση εμφανίστηκαν warnings σχετικοί με Java 25 και native access του embedded Tomcat. Αυτά δεν εμπόδισαν τη λειτουργία του worker και προς το παρόν θεωρούνται μη κρίσιμα.
   Commands Used During Testing
   Start worker locally
   Η εκκίνηση έγινε από IntelliJ μέσω της main class:
   WorkerServiceApplication
   Test task execution
   curl -X POST http://localhost:8083/task \
   -H "Content-Type: application/json" \
   -d '{
   "taskId": "task-1",
   "jobId": "job-1",
   "type": "MAP",
   "inputBucket": "input-data",
   "inputObjectKeys": ["file1.txt"],
   "outputBucket": "intermediate-data",
   "outputPrefix": "job-1/map/",
   "mapperClass": "WordCountMapper",
   "reducerClass": "WordCountReducer",
   "reducersCount": 1,
   "reducePartition": 0,
   "managerCallbackUrl": "http://localhost:8081/task/complete"
   }'
   MinIO Console
   http://localhost:9001
   Worker health check
   http://localhost:8083/health
   Reached Milestone
   Το development έχει φτάσει μέχρι το σημείο όπου:
   ο worker-service λειτουργεί κανονικά τοπικά,
   έχει γίνει επιτυχής end-to-end δοκιμή ενός MAP task,
   και το intermediate result παράγεται και αποθηκεύεται στο MinIO.
   Next Steps
   Τα επόμενα βήματα είναι:
   έλεγχος και εκτέλεση REDUCE task,
   έλεγχος του παραγόμενου intermediate output format,
   καθαρισμός του output path formatting,
   integration με manager callbacks,
   μεταφορά από local execution σε Docker/Kubernetes execution model.