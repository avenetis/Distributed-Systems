package com.tuc.distributed.worker.service;

import org.springframework.stereotype.Service;

@Service
public class PartitionService {

    public int partitionOf(String key, int reducersCount) {
        if (reducersCount <= 0) {
            throw new IllegalArgumentException("reducersCount must be > 0");
        }
        return Math.floorMod(key.hashCode(), reducersCount);//x-([x/y]*y)
    }//key.hashCode() % reducersCount Κάνουμε Static hashing
}//h(key.hashCode()) = key.hashCode() % reducersCount
//και απλά αντι να αποθηκεύσουμε σε κάποιο array σε κάποια κλάση
//τα δεδομένα μας αποθηκεύονται στο miniIO

//το Key ειναι η λέξη το .hashCode() μετατρέπει τη λέξη σε αριθμό (int 32-bit)

/*άρα πρακτικά το floorMod στην ουσία μετατρέπει ένα απλο array με χωριτηκότητα
θέσεων reducersCount το μετατρέπει σε ring buffer ωστε αν βγει αρνητικος αριθμος
να κινηθεί αριστερόστροφα στο buffer
 */