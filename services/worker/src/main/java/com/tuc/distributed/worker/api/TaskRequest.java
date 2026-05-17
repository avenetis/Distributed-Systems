package com.tuc.distributed.worker.api;

import com.tuc.distributed.worker.domain.TaskType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

//Είναι ένα μηνυμα που στέλνει ο manager προς τον worker για να του πει τι task να τρέξει
public record TaskRequest(
        @NotBlank String taskId,//ποιο task είναι
        @NotBlank String jobId,//σε ποιο job ανήκει | Αυτό είναι το ID του συνολικού MapReduce job.
        @NotNull TaskType type,//είναι MAP ή REDUCE;
        @NotBlank String inputBucket,//Το inputBucket είναι απλά το όνομα της αποθήκης (του κουβά) στο MinIO.Το MinIO λειτουργεί σαν object storage inputBucket = mapreduce-inputs
        @NotEmpty List<String> inputObjectKeys,//Αν έχουμε 3 Workers (A, B, C) που κάνουν Map, ο Manager θα τους στείλει το ίδιο inputBucket, αλλά διαφορετικό inputObjectKeys στον καθένα για να δουλέψουν παράλληλα χωρίς να μπερδεύονται
        @NotBlank String outputBucket,//πού να γράψει output;
        @NotBlank String outputPrefix,//πού να γράψει output;
        @NotBlank String mapperClass,//ποιον mapper να χρησιμοποιήσει
        @NotBlank String reducerClass,//ποιον reducer να χρησιμοποιήσει
        Integer reducersCount,//πόσοι reducers υπάρχουν συνολικά
        Integer reducePartition,//ποιο reduce partition αφορά;
        String managerCallbackUrl//πού να ενημερώσει τον Manager όταν τελειώσει
) {
}
/* Παράδειγμα
job-1
 ├── map-task-1
 ├── map-task-2
 ├── map-task-3
 ├── reduce-task-1
 └── reduce-task-2
 ένα MapReduce job μπορεί να σπάσει σε πολλά tasks
 Άρα το taskId λέει στον worker: Αυτό που θα εκτελέσεις τώρα είναι το map-task-2.
 */



//ο Worker παίρνει όλες τις πληροφορίες που χρειάζεται για να είναι stateless